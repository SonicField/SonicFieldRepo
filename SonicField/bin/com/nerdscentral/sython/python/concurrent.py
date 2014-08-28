import threading
import time
from java.util.concurrent import Executors, TimeUnit
from java.util.concurrent import Callable, Future
from java.lang import System
from java.lang import ThreadLocal
from java.lang import Thread
from java.util.concurrent import TimeUnit
from java.util.concurrent.locks import ReentrantLock

SF_MAX_CONCURRENT = int(System.getProperty("synthon.threads"))
SF_MAX_CONQUEUE   = SF_MAX_CONCURRENT
#SF_MAX_CONQUEUE   = -1

print "Concurrent Threads: " + SF_MAX_CONCURRENT.__str__()
SF_POOL = Executors.newCachedThreadPool()

class sf_callable(Callable):
    def __init__(self,toDo):
        self.toDo=toDo
        
    def call(self):
        return self.toDo()

class sf_futureWrapper(Future):
    def __init__(self,toDo):
        self.toDo=toDo
        self.gotten=False

    def __iter__(self):
        return iter(self.get())
        
    def get(self):
        if(self.gotten):
            return self.result
        else:
            self.result=self.toDo.get()
            self.gotten=True
            return self.result
    
    def __pos__(self):
        obj=self.get()
        return +obj

    def __neg__(self):
        obj=self.get()
        return -obj

class sf_getter(Future):
    def __init__(self,toDo):
        self.toDo=toDo
        self.result=self.toDo()

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
        return []

SF_TASK_QUEUE=sf_taskQueue()

class sf_superFuture(Future):

    def __init__(self,toDo):
        self.toDo=toDo
        queue=SF_TASK_QUEUE.get()
        queue.append(self)
        if(len(queue)>SF_MAX_CONQUEUE):
            self.submitAll()

    def submit(self):
        count=SF_POOL.getActiveCount()
        if(count<SF_MAX_CONCURRENT):
            task=sf_callable(self.toDo)
            self.future=sf_futureWrapper(SF_POOL.submit(task))
        else:
            self.future=sf_getter(self.toDo)

    def submitAll(self):
        queue=SF_TASK_QUEUE.get()
        while(len(queue)):
            queue.pop().submit()

    def get(self):
        self.submitAll()
        while not hasattr(self,'future'):
            Thread.yield()
        r = self.future.get()
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

def shutdown_and_await_termination(pool, timeout):
    pool.shutdown()
    try:
        if not pool.awaitTermination(timeout, TimeUnit.SECONDS):
            pool.shutdownNow()
            if (not pool.awaitTermination(timeout, TimeUnit.SECONDS)):
                print >> sys.stderr, "Pool did not terminate"
    except InterruptedException, ex:
        pool.shutdownNow()
        Thread.currentThread().interrupt()
        
def shutdownConcurrnt():
    shutdown_and_await_termination(SF_POOL, 5)