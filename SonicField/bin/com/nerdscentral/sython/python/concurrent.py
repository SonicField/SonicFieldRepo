import threading
import time
from java.util.concurrent import Callable, Future, ConcurrentLinkedQueue, \
                                 ConcurrentHashMap, Executors, TimeUnit
from java.util.concurrent.locks import ReentrantLock

from java.lang import System
from java.lang import ThreadLocal
from java.lang import Thread
from java.util import Collections
from java.util.concurrent import TimeUnit
from java.util.concurrent.atomic import AtomicLong, AtomicBoolean

SF_MAX_CONCURRENT = int(System.getProperty("synthon.threads"))
SF_MAX_CONQUEUE   = SF_MAX_CONCURRENT
SF_TASK_ID        = AtomicLong()
SF_QUEUED         = AtomicLong()
SF_ASLEEP         = AtomicLong()
SF_STARTED        = System.currentTimeMillis()
TRACE=True

SF_LOG_LOCK=ReentrantLock()
print "Thread\tQueue\tAsleep\tTime\tMessage..."
def cLog(*args):
    SF_LOG_LOCK.lock()
    print "\t".join(str(x) for x in [Thread.currentThread().getId(),SF_QUEUED.get(),SF_ASLEEP.get(),(System.currentTimeMillis()-SF_STARTED)] + list(args))
    SF_LOG_LOCK.unlock()

cLog( "Concurrent Threads: " + SF_MAX_CONCURRENT.__str__())
SF_POOL    = Executors.newCachedThreadPool()
SF_PENDING = Collections.newSetFromMap(ConcurrentHashMap(SF_MAX_CONQUEUE*SF_MAX_CONQUEUE,0.75,SF_MAX_CONQUEUE))
    
class sf_safeQueue(ConcurrentLinkedQueue):
    def pop(self):
        SF_QUEUED.getAndDecrement()
        r = self.poll()
        SF_PENDING.remove(r)
        return r
    
    def append(self, what):
        SF_QUEUED.getAndIncrement()
        SF_PENDING.add(what)
        self.add(what)

class sf_callable(Callable):
    def __init__(self,toDo):
        self.toDo=toDo
        
    def call(self):
        cLog("FStart",self.toDo)
        ret=self.toDo()
        cLog("FDone")
        return ret

class sf_futureWrapper(Future):
    def __init__(self,toDo):
        self.toDo   = toDo
        self.gotten = AtomicBoolean(False)
        self.mutex  = ReentrantLock()

    def __iter__(self):
        return iter(self.get())
    
    def isDone(self):
        return self.toDo.isDone()
    
    def get(self):
        return self.toDo.get()

    def __pos__(self):
        obj=self.get()
        return +obj

    def __neg__(self):
        obj=self.get()
        return -obj

class sf_getter(Future):
    def __init__(self,toDo):
        self.toDo=toDo
        cLog("GStart",self.toDo)
        self.result=self.toDo()
        cLog("GDone")

    def isDone(self):
        return True

    def get(self):
        return self.result

    def __iter__(self):
        return iter(self.get())
        
    def __pos__(self):
        obj=self.get()
        return +obj

    def __neg__(self):
        obj=self.get()
        return -obj

class sf_taskQueue(ThreadLocal):
    def initialValue(self):
        return sf_safeQueue()
    
SF_TASK_QUEUE=sf_taskQueue()

# Back to main stream
class sf_superFuture(Future):

    def __init__(self,toDo):
        self.toDo=toDo
        self.id=SF_TASK_ID.incrementAndGet()
        queue=SF_TASK_QUEUE.get()
        queue.append(self)
        self.mutex=ReentrantLock()
        self.submitted=False
        if len(queue)>SF_MAX_CONQUEUE:
            self.submitAll()

    def getId(self):
        return self.id

    def directSubmit(self):
        self.mutex.lock()
        if self.submitted:
            self.mutex.unlock()
            return
        self.submitted=True
        self.mutex.unlock()
        self.future=sf_getter(self.toDo)
        
    def submit(self):
        self.mutex.lock()
        if self.submitted:
            self.mutex.unlock()
            return
        self.submitted=True
        self.mutex.unlock()
        count=SF_POOL.getActiveCount()
        if count<SF_MAX_CONCURRENT:
            task=sf_callable(self.toDo)
            self.future=sf_futureWrapper(SF_POOL.submit(task))
        else:
            self.future=sf_getter(self.toDo)

    def submitAll(self):
        queue=SF_TASK_QUEUE.get()
        while(len(queue)):
            queue.pop().submit()

    def get(self):
        cLog( "Submit All")
        self.submitAll()
        cLog( "Submitted All")
        t=System.currentTimeMillis()
        c=1
        while not hasattr(self,"future"):
            c+=1
            Thread.yield()
        t=System.currentTimeMillis()-t
        cLog( "Raced: ", c ,t)
        if not self.future.isDone():
            SF_ASLEEP.getAndIncrement();
            cLog("Sleep")
            nap=True
        else:
            nap=False
            cLog("Get")
        back=1
        while not self.future.isDone():
            nap=True
            it=SF_PENDING.iterator()
            if it.hasNext():
                try:
                    toSteel=it.next()
                    it.remove()
                    SF_ASLEEP.getAndDecrement();
                    cLog("Steel",toSteel.toDo)
                    SF_ASLEEP.getAndIncrement();
                    toSteel.directSubmit()
                except Exception, e:
                    cLog("Failed to steel",e.getMessage())
            else:
                if back==1:
                    cLog("Non Pending")
                Thread.sleep(back)
                back+=1
                if back>100:
                    back=100
        r = self.future.get()
        if nap:
            SF_ASLEEP.getAndDecrement();
        cLog("Wake")
        return r

    def __iter__(self):
        return iter(self.get())
            
    def __pos__(self):
        obj=self.get()
        return +obj


    def __neg__(self):
        obj=self.get()
        return -obj

def sf_do(toDo):
    return sf_superFuture(toDo)
    
def sf_parallel(func):
    def inner(*args, **kwargs): #1
        return func(*args, **kwargs) #2
    return sf_do(inner)

def shutdown_and_await_termination(pool, timeout):
    pool.shutdown()
    try:
        if not pool.awaitTermination(timeout, TimeUnit.SECONDS):
            pool.shutdownNow()
            if not pool.awaitTermination(timeout, TimeUnit.SECONDS):
                print >> sys.stderr, "Pool did not terminate"
    except InterruptedException, ex:
        pool.shutdownNow()
        Thread.currentThread().interrupt()
        
def shutdownConcurrnt():
    shutdown_and_await_termination(SF_POOL, 5)