# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
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

"""
Work Stealing Lazy Task Scheduler By Dr Alexander J Turner

Basic Concepts:
===============

- Tasks are closures. 
- The computation of the task is called 'execution'.
- They can be executed in any order* on any thread.
- The data they contain is immutable.
- Making a task able to be executed is called submission.
- Execution is lazy**.
- Tasks are executed via a pool of threads and optionally one 'main' thread.
- Ensuring the work of a task is complete and acquiring the result (if any)
  is called 'getting' the task.

Scheduling Overview:
====================

* Any order, means that subitted tasks can be executed in any order though there
can be an order implicit in their dependencies.

IE taskA can depend on the result of taskB. Therefore.
- taskA submits taskA for execution.
- taskB submits taskC, taskD and taskE for execution.
- taskB now 'gets' the results of taskC, taskD and taskE

In the above case it does not matter which order taskC, taskD and taskE are
executed.

** Lazy execution means that executing a task is not done always at task 
submission time. Tasks are submitted if one of the following conditions is 
met.
- the maximum permitted number of non executing tasks has been reached. See
  'the embrace of meh' below.
- the thread submitting the task 'gets' and of the tasks which have not yet
  been submitted.
- a thread would is in the process of 'getting' a task but would block waiting
  for the task to finish executing. In this results in 'work stealing' (see
  below) where any other pending tasks for any threads any be executed by the
  thread which would otherwise block.

Embrace of meh
==============

Meh, as in a turm for not caring or giving up. This is a form of deadlock in
pooled future based systems where the deadlock is causes by a circular 
dependency involving the maximum number of executors in the pool rather than a
classic mutex deadlock. Consider this scenario:

- There are only two executors X and Y
- taskA executes on X
- taskA sumbits and then 'gets' taskB
- taskB executes on Y
- taskB submits and then 'gets' taskC
- taskC cannot execute because X and Y are blocked
- taskB cannot progress because it is waiting for taskC
- taskA cannot progress because it is waiting for taskB
- the executors cannt free to execute task as they are blocked by taskA and 
  taskB
- Meh

The solution used here is a soft upper limit to the number of tasks submitted
to the pool of executors. When that upper limit is reached, new tasks are 
not submitted for asynchronous execution but are executed immediately by the
thread which submits them. This prevents exhaustion of the available executors
and therefore prevents the embrace of meh.

Exact computation of the number of running executors is non tricky with the
code used here (java thread pools etc). Therefore, the upper limit is 'soft'.
In other words, sometimes more executors are used than the specified limit. This
is not a large issue here because the OS scheduler simply time shares between 
the executors - which are in fact native threads.

The alternative of an unbounded upper limit to the number of executors in not
viable; every simple granular synthesis or parallel FFT work can easily 
exhaust the maximum number of available native threads on modern machines. Also,
current operating systems are not optimised for time sharing between huge
numbers of threads. Finally, there is a direct link between the number of
threads running and the amount of memory used. For all these reasons, a soft
limited thread pool with direct execution works well.

Work Stealing
=============
Consider:
- taskA threadX submits taskB
- taskA threadX gets taskB
- taskB threadY submits taskC and taskD
- taskB threadY gets taskC
- there are no more executors so taskC is executed on threadY
- at this point taskC is pending execution and taskA threadX is waiting for the
- result of taskB on threadY.
- threadX can the stop waiting for taskB and 'steal' taskC.

Note that tasks can be executed in any order on any thread.

"""

# CONSTANTS AND CONTROL VARIABLES
# ===============================

# The maximum number of executors. Note that in general the system
# gets started by some 'main' thread which is then used for work as 
# well, so the total number of executors is often one more than this 
# number. Also note that this is a soft limit, synchronisation between
# submissions for execution is weak so it is possible for more executors
# to scheduled.
SF_MAX_CONCURRENT = int(System.getProperty("sython.threads"))

# Tracks the number of current queue but not executing tasks
SF_QUEUED         = AtomicLong()

# Tracks the number of executors which are sleeping because they are blocked
# and are not currently stealing work
SF_ASLEEP         = AtomicLong()

# Marks when the concurrent system came up to make logging more human readable
SF_STARTED        = System.currentTimeMillis()

# Causes scheduler operation to be logged
TRACE             = str(System.getProperty("sython.trace")).lower()=="true"

# A thread pool used for the executors 
SF_POOL    = Executors.newCachedThreadPool()

# A set of tasks which might be available for stealing. Use a concurrent set so
# that it shares information between threads in a stable and relatively 
# efficient way. Note that a task being in this set does not guarantee it is
# not being executed. A locking flag on the 'superFuture' task management
# objects disambiguates this to prevent double execution. 
SF_PENDING = Collections.newSetFromMap(ConcurrentHashMap(SF_MAX_CONCURRENT*128,0.75,SF_MAX_CONCURRENT))

# EXECUTION
# =========

# Define the logger method as more than pass only is tracing is turned on
if TRACE:
    # Force 'nice' interleaving when logging from multiple threads
    SF_LOG_LOCK=ReentrantLock()
    print "Thread\tQueue\tAsleep\tTime\tMessage..."
    def cLog(*args):
        SF_LOG_LOCK.lock()
        print "\t".join(str(x) for x in [Thread.currentThread().getId(),SF_QUEUED.get(),SF_ASLEEP.get(),(System.currentTimeMillis()-SF_STARTED)] + list(args))
        SF_LOG_LOCK.unlock()
else:
    def cLog(*args):
        pass

cLog( "Concurrent Threads: " + SF_MAX_CONCURRENT.__str__())
    
# Decorates ConcurrentLinkedQueue with tracking of total (global) number of
# queued elements. Also remaps the method names to be closer to python lists
class sf_safeQueue(ConcurrentLinkedQueue):
    # Note that this is actually the reverse of a python pop, this is actually
    # equivalent to [1,2,3,4,5].pop(0).
    def pop(self):
        SF_QUEUED.getAndDecrement()
        r = self.poll()
        SF_PENDING.remove(r)
        return r
    
    def append(self, what):
        SF_QUEUED.getAndIncrement()
        SF_PENDING.add(what)
        self.add(what)

# Python implements Callable to alow Python closers to be executed in Java
# thread pools
class sf_callable(Callable):
    def __init__(self,toDo):
        self.toDo=toDo
    
    # This method is that which causes a task to be executed. It actually
    # executes the Python closure which defines the work to be done
    def call(self):
        cLog("FStart",self.toDo)
        ret=self.toDo()
        cLog("FDone")
        return ret

# Holds the Future created by submitting a sf_callable to the SF_POOL for
# execution. Note that this is also a Future for consistency, but its work
# is delegated to the wrapped future. 
class sf_futureWrapper(Future):
    def __init__(self,toDo):
        self.toDo   = toDo

    def __iter__(self):
        return iter(self.get())
    
    def isDone(self):
        return self.toDo.isDone()
    
    def get(self):
        return self.toDo.get()

# Also a Future (see sf_futureWrapper) but these execute the python closer
# in the thread which calls the constructor. Therefore, the results is available
# when the constructor exits. These are the primary mechanism for preventing
# The Embrace Of Meh.
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

# Queues of tasks which have not yet been submitted are thread local. It is only
# when a executor thread would become blocked that we go to work stealing. This
# class managed that thread locallity.
# TODO, should this, can this, go to using Python lists rather than concurrent
# linked queues.
class sf_taskQueue(ThreadLocal):
    def initialValue(self):
        return sf_safeQueue()

# The thread local queue of tasks which have not yet been submitted for
# execution
SF_TASK_QUEUE=sf_taskQueue()

# The main coordination class for the schedular. Whilst it is a future
# it actually deligates execution to sf_futureWrapper and sf_getter objects
# for synchronous and asynchronous operation respectively
class sf_superFuture(Future):

    # - Wrap the closure (toDo) which is the actual task (work to do)
    # - Add that task to the thread local queue by adding self to the queue
    #   thus this object is a proxy for the task.
    # - Initialise a simple mutual exclusion lock.
    # - Mark this super future as not having been submitted for execution. This
    #   is part of the mechanism which prevents work stealing resulting in a
    #   task being executed twice.
    def __init__(self,toDo):
        self.toDo=toDo
        queue=SF_TASK_QUEUE.get()
        queue.append(self)
        self.mutex=ReentrantLock()
        self.submitted=False

    # Used by work stealing to submit this task for immediate execution on the
    # the executing thread. The actual execution is delegated to an sf_getter
    # which executes the task in its constructor. This (along with submit) use
    # the mutex to manage the self.submitted field in a thread safe way. No
    # two threads can execute submit a super future more than once because
    # self.submitted is either true or false atomically across all threads. 
    # The lock has the other effect of synchronising memory state across cores
    # etc.
    def directSubmit(self):
        # Ensure this cannot be executed twice
        self.mutex.lock()
        if self.submitted:
            self.mutex.unlock()
            return
        self.submitted=True
        self.mutex.unlock()
        # Execute
        self.future=sf_getter(self.toDo)
    
    # Normal (non work stealing) submition of this task. This might or might not
    # result in immediate execution. If the total number of active executors is
    # at the limit then the task will execute in the calling thread via a
    # sf_getter (see directSubmit for more details). Otherwise, the task is 
    # submitted to the execution pool for asynchronous execution.
    #
    # It is important to understand that this method is not called directly
    # but is called via submitAll. submitAll is the method which subits tasks
    # from the thread local queue of pending tasks.
    def submit(self):
        # Ensure this cannot be submitted twice
        self.mutex.lock()
        if self.submitted:
            self.mutex.unlock()
            return
        self.submitted=True
        self.mutex.unlock()
        
        # See if we have reached the parallel execution soft limit
        count=SF_POOL.getActiveCount()
        cLog("Submit")
        if count<SF_MAX_CONCURRENT:
            # No, so submit to the thread pool for execution
            task=sf_callable(self.toDo)
            self.future=sf_futureWrapper(SF_POOL.submit(task))
        else:
            # Yes, execute in the current thread
            self.future=sf_getter(self.toDo)
        cLog("Submitted")

    # Submit all the tasks in the current thread local queue of tasks. This is
    # lazy executor. This gets called when we need results. 
    def submitAll(self):
        queue=SF_TASK_QUEUE.get()
        while(len(queue)):
            queue.pop().submit()

    # The point of execution in the lazy model. This method is what consumers
    # of tasks call to get the task executed and retrieve the result (if any).
    # This therefore acts as the point of synchronisation. This method will not
    # return until the task wrapped by this super future has finished executing.
    #
    # A note on stealing. Consider that we steal taskA. TaskA then invokes
    # get() on taskB. taskB is not completed. The stealing can chain here; 
    # whilst waiting for taskB to complete the thread can just steal another
    # task and so on. This is why we can use the directSubmit for stolen tasks.  
    def get(self):
        cLog( "Submit All")
        # Submit the current thread local task queue
        self.submitAll()
        cLog( "Submitted All")
        t=System.currentTimeMillis()
        c=1
        # There is a race condition where by the submitAll has been called
        # which recursively causes another instance of get on this super future
        # which results in there being no tasks to submit but we get to this 
        # point before self.future has been set. This tends to resolve itself
        # very quickly as the empty calls to submit all do not do very much work 
        # so the origianl progresses and sets the future. Rather than a complex
        # and potentially brittle locking system, we just spin waiting for the
        # future to be set. This works fine on my Mac as it only ever seems to 
        # spin once, so the cost is pretty much the same as a locking approach
        # basically, one quantum. If this starts to spin a lot in the future
        # a promise/future approach could be used. 
        while not hasattr(self,"future"):
            c+=1
            Thread.yield()
        t=System.currentTimeMillis()-t
        cLog( "Raced: ", c ,t)
        
        # This is where the work stealing logic starts
        # isDone() tells us if the thread would block if get() were called on
        # the future. We will try to work steal if the thread would block so as
        # not to 'waste' the thread. This if block is setting up the
        # log/tracking information
        if not self.future.isDone():
            SF_ASLEEP.getAndIncrement();
            cLog("Sleep")
            nap=True
        else:
            nap=False
            cLog("Get")
        # back control increasing backoff of the thread trying to work steal.
        # This is not the most efficient solution but as Sonic Field tasks are
        # generally large, this approach is OK for the current use. We back of
        # between 1 and 100 milliseconds. At 100 milliseconds we just stick at
        # polling every 100 milliseconds.
        back=1
        # This look steal work until the get() on the current task will 
        # not block
        while not self.future.isDone():
            # Iterate over the global set of pending super futures
            # Note that the locking logic in the super futures ensure 
            # no double execute.
            it=SF_PENDING.iterator()
            while it.hasNext():
                try:
                    # reset back (the back-off sleep time)
                    back=1
                    toSteal=it.next()
                    it.remove()
                    cLog("Steal",toSteal.toDo)
                    # Track number of tasks available to steal for logging
                    SF_ASLEEP.getAndDecrement();
                    # The stollen task must be performed in this thread as 
                    # this is the thread which would otherwise block so is
                    # availible for further execution
                    toSteal.directSubmit()
                    # Now we manage state of 'nap' which is used for logging
                    # based on if we can steal more (i.e. would still block)
                    # Nap also controls the thread back off
                    if self.future.isDone():
                        nap=False
                        break
                    else:
                        nap=True
                        SF_ASLEEP.getAndIncrement();
                except Exception, e:
                    # All bets are off
                    cLog("Failed to Steal",str(e))
                    # Just raise and give up
                    raise
            # If the thread would block again or we are not able to steal as
            # nothing pending then we back off. 
            if nap==True:
                if back==1:
                    cLog("Non Pending")
                Thread.sleep(back)
                back+=1
                if back>100:
                    back=100
        if nap:
            SF_ASLEEP.getAndDecrement();
        # To get here we know this get will not block
        r = self.future.get()
        cLog("Wake")
        # Return the result if any
        return r

    # Proxy all other methods to the result of the future
    def __getattr__(self,name):
        cLog("Proxying: ",name)
        return self.get().__getattr__(name)

    # If the return of the get is iterable then we delegate to it so that 
    # this super future appears to be its embedded task
    def __iter__(self):
        return iter(self.get())
            
    # Similarly for resource control for the + and - reference counting 
    # semantics for SF_Signal objects
    def __pos__(self):
        obj=self.get()
        return +obj

    def __neg__(self):
        obj=self.get()
        return -obj

# Wrap a closure in a super future which automatically 
# queues it for future lazy execution
def sf_do(toDo):
    return sf_superFuture(toDo)
    
# An experimental decorator approach equivalent to sf_do
class sf_parallel(object):

    def __init__(self,func):
        self.func=func

    def __call__(self,*args, **kwargs):
        def closure():
            return self.func(*args, **kwargs) 
        return sf_superFuture(closure)

# Shut the execution pool down. This waits for it to shut down
# but if the shutdown takes longer than timeout then it is 
# forced down.
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

# The default shutdown for the main pool  
def shutdownConcurrnt():
    shutdown_and_await_termination(SF_POOL, 5)