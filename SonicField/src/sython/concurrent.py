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
from com.nerdscentral.audio.core import SFConstants

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

# Marks when the concurrent system came up to make logging more human readable
SF_STARTED        = System.currentTimeMillis()

# Causes scheduler operation to be logged
TRACE             = str(System.getProperty("sython.trace")).lower()=="true"
SFConstants.TRACE=TRACE

# Removes all parallel execution so debugging is easier.
SF_LINEAR         = str(System.getProperty("sython.linear")).lower()=="true"

# TODO - make these configurable from the command line

# Do we work steal?

if System.getProperty("sython.steal") is None:
    SF_DO_STEALING = True
else:
    SF_DO_STEALING = str(System.getProperty("sython.steal")).lower()=="true"

# Sets the maximum number of jobs queued for parallel execution before
# starting linear execution of new jobs
if System.getProperty("sython.linear_limit") is None:
    if TRACE:
        SF_LINEAR_LIMIT   = 1024
    else:
        SF_LINEAR_LIMIT   = 1024*1024
else:
    SF_LINEAR_LIMIT = int(System.getProperty("sython.linear_limit"))

# Maximum recursion depth of stealing
SF_MAX_STEAL          = 64

# A thread pool used for the executors 
SF_POOL    = Executors.newCachedThreadPool()

# A set of tasks which might be available for stealing. Use a concurrent set so
# that it shares information between threads in a stable and relatively 
# efficient way. Note that a task being in this set does not guarantee it is
# not being executed. A locking flag on the 'superFuture' task management
# objects disambiguates this to prevent double execution. 
SF_PENDING   = Collections.newSetFromMap(ConcurrentHashMap(SF_MAX_CONCURRENT*128,0.75,SF_MAX_CONCURRENT+1))

# All parallel jobs get a job number which is globally unique
# and increasing. This is used for logging and cycle checking
# and any other house keeping which requires a unique id across
# jobs.
SF_JOB_NUMB  = AtomicLong()

# Tracks how many threads are waiting for other threads
SF_NWAITING  = AtomicLong()

# EXECUTION
# =========

# Force 'nice' interleaving when logging from multiple threads

class Sf_Lock():
    def __init__(self):
        self.lock_=ReentrantLock()
    
    def lock(self):
        self.lock_.lock()
        
    def unlock(self):
        self.lock_.unlock()
        
    def __enter__(self):
        self.lock()
        return self
    
    def __exit__(self, type, value, traceback):
        self.unlock()

SF_LOG_LOCK=Sf_Lock()

print "Thread\tQueue\tActive\tWaiting\tTime\tMessage..."
def d_log(*args):
    with SF_LOG_LOCK:
        print "\t".join(str(x) for x in [Thread.currentThread().getId(),SF_PENDING.size(),SF_POOL.getActiveCount(),SF_NWAITING.get(),(System.currentTimeMillis()-SF_STARTED)] + list(args))

# Define the logger method as more than pass only is tracing is turned on
if TRACE:
    c_log=d_log
else:
    def c_log(*args):
        pass

c_log( "Concurrent Threads: " + SF_MAX_CONCURRENT.__str__())
    
# Decorates ConcurrentLinkedQueue with tracking of total (global) number of
# queued elements. Also remaps the method names to be closer to python lists
class Sf_SafeQueue(ConcurrentLinkedQueue):
    # Note that this is actually the reverse of a python pop, this is actually
    # equivalent to [1,2,3,4,5].pop(0).
    def pop(self):
        r = self.poll()
        SF_PENDING.remove(r)
        return r
    
    def append(self, what):
        SF_PENDING.add(what)
        self.add(what)

# Die straight away if anything goes wrong
# Sonic Field has no concept of recovering from exceptions which 
# reach teh schedular
def dieNow():
    import sys,traceback
    e=sys.exc_info()
    # All bets are off
    d_log("Failed to execute:",str(e))
    print >> sys.stderr, traceback.format_exc(e)
    # no point wasting time trying to continue
    # TODO dump stack traces of all threads in a nice
    # way when this happens.
    System.exit(1)

# Python implements Callable to alow Python closers to be executed in Java
# thread pools
class Sf_Callable(Callable):
    def __init__(self,toDo):
        self.toDo=toDo
    
    # This method is that which causes a task to be executed. It actually
    # executes the Python closure which defines the work to be done
    def call(self):
        try:
            c_log("FStart",self.toDo)
            ret=self.toDo()
            c_log("FDone")
            return ret
        except:
            dieNow()

# Holds the Future created by submitting a Sf_Callable to the SF_POOL for
# execution. Note that this is also a Future for consistency, but its work
# is delegated to the wrapped future. 
class Sf_FutureWrapper(Future):
    def __init__(self,toDo,job_number):
        self.toDo=toDo
        self.job_number=job_number

    def __iter__(self):
        return iter(self.get())
    
    def isDone(self):
        return self.toDo.isDone()
 
    def __str__(self):
        return "FutureWrapper:"+str(self.job_number)

    def get(self):
        return self.toDo.get()

# Also a Future (see Sf_FutureWrapper) but these execute the python closer
# in the thread which calls the constructor. The results is available
# when the execute method exits. These are the primary mechanism for preventing
# The Embrace Of Meh.
class Sf_Getter(Future):
    def __init__(self,toDo,job_number):
        self.toDo=toDo
        self.job_number=job_number

    def execute(self):
        try:
            c_log("GStart",self.job_number,self.toDo)
            self.result=self.toDo()
            c_log("GDone",self.job_number)
        except:
            dieNow()

    def isDone(self):
        return hasattr(self,'result')
        
    def __str__(self):
        return "Getter:"+str(self.job_number)

    def get(self):
        return self.result

# Queues of tasks which have not yet been submitted are thread local. It is only
# when a executor thread would become blocked that we go to work stealing. This
# class managed that thread locallity.
# TODO, should this, can this, go to using Python lists rather than concurrent
# linked queues.
class Sf_TaskQueue(ThreadLocal):
    def initialValue(self):
        return Sf_SafeQueue()

# Measures the depth of recursion of the stealing system so that we do not
# blow out the stack.
class Sf_DepthCount:
    def __init__(self):
        self.count=0
        
    def incr(self):
        self.count+=1
        return self.count
    
    def decr(self):
        self.count-=1
        return self.count
        
    def value(self):
        return self.count

    class Sf_StealDepth(ThreadLocal):
        def initialValue(self):
            return Sf_DepthCount()

    @staticmethod
    def new_counter():
        return Sf_DepthCount.Sf_StealDepth()

# Keeps track of the level of recursion of stealing for the current 
# thread to protect against stack overflow
SF_STEAL_RECURSION = Sf_DepthCount.new_counter()

def sf_check_steal_overflow():
    return SF_STEAL_RECURSION.get().incr()<=SF_MAX_STEAL

def sf_release_steal_overflow():
    SF_STEAL_RECURSION.get().decr()

# The thread local queue of tasks which have not yet been submitted for
# execution
SF_TASK_QUEUE=Sf_TaskQueue()

# The main coordination class for the schedular. Whilst it is a future
# it actually deligates execution to Sf_FutureWrapper and Sf_Getter objects
# for synchronous and asynchronous operation respectively
class Sf_SuperFuture(Future):

    def steal(self,nap=False):
        if not SF_DO_STEALING:
            return True
        if not sf_check_steal_overflow():
            sf_release_steal_overflow()
            c_log("Steal Overflowed",self.job_number)
            return True
        # Iterate over the global set of pending super futures
        # Note that the locking logic in the super futures ensure 
        # no double execute.
        it=SF_PENDING.iterator()
        while it.hasNext():
            try:
                toSteal=it.next()
                ojn=toSteal.job_number
                c_log("In Steal",ojn)
                it.remove()
                c_log("Steal",toSteal.toDo)
                # The stollen task must be performed in this thread as 
                # this is the thread which would otherwise block so is
                # availible for further execution
                toSteal.directSubmit()
                # Now we manage state of 'nap' which is used for logging
                # Based on if we can steal more (i.e. would still block)
                # Nap also controls the thread back off
                if hasattr(self,"future") and self.future.isDone():
                    nap=False
                    break
                else:
                    nap=True
            except:
                dieNow()
        sf_release_steal_overflow()
        return nap

    # - Wrap the closure (toDo) which is the actual task (work to do)
    # - Add that task to the thread local queue by adding self to the queue
    #   thus this object is a proxy for the task.
    # - Initialise a simple mutual exclusion lock.
    # - Mark this super future as not having been submitted for execution. This
    #   is part of the mechanism which prevents work stealing resulting in a
    #   task being executed twice.
    def __init__(self,toDo):
        mutex=Sf_Lock()
        with mutex:
            # This unique number can be used for tracking cycles
            self.job_number=SF_JOB_NUMB.incrementAndGet()
            self.mutex=mutex
            self.toDo=toDo
            self.submitted=False
            queue=SF_TASK_QUEUE.get()
            queue.append(self)

    # Testing Only
    # ============
    # Forward isDone to the wrapped future. Therefore,
    # this is effectively isDone for this super future as well. We can safely
    # call isDone on the super future test if calling 'get' will imediately
    # return with the pre-executed and completed work in the future.
    #
    # This is not likely to be of use for normal code because we should not
    # interfere with the scheduler by inspecting if work is done or not.
    # However, this method is very useful for testing.
    def isDone(self):
        return self.toDo.isDone()

    # Used by work stealing to submit this task for immediate execution on the
    # the executing thread. The actual execution is delegated to an Sf_Getter
    # which executes the task in its constructor. This (along with submit) use
    # the mutex to manage the self.submitted field in a thread safe way. No
    # two threads can execute submit a super future more than once because
    # self.submitted is either true or false atomically across all threads. 
    # The lock has the other effect of synchronising memory state across cores
    # etc.
    def directSubmit(self):
        # Ensure this cannot be executed twice
        with self.mutex:
            if self.submitted:
                return
            self.submitted=True
            # Execute
            self.future=Sf_Getter(self.toDo,self.job_number)
        self.future.execute()
    
    # Normal (non work stealing) submission of this task. This might or might not
    # result in immediate execution. If the total number of active executors is
    # at the limit then the task will execute in the calling thread via a
    # Sf_Getter (see directSubmit for more details). Otherwise, the task is 
    # submitted to the execution pool for asynchronous execution.
    #
    # It is important to understand that this method is not called directly
    # but is called via submitAll. submitAll is the method which subits tasks
    # from the thread local queue of pending tasks.
    def submit(self):
        # Ensure this cannot be submitted twice
        with self.mutex:
            if self.submitted:
                return
            self.submitted=True
            
            # See if we have reached the parallel execution soft limit
            count=SF_POOL.getActiveCount()
            c_log("Submit")
            if count<SF_MAX_CONCURRENT:
                # No, so submit to the thread pool for execution
                task=Sf_Callable(self.toDo)
                self.future=Sf_FutureWrapper(SF_POOL.submit(task),self.job_number)
                c_log("Submitted")
                return
            # Yes, execute in the current thread
            self.future=Sf_Getter(self.toDo,self.job_number)
        # Execute outside the lock
        self.future.execute()
        c_log("Direct Submitted")

    # Submit all the tasks in the current thread local queue of tasks. This is
    # lazy executor. This gets called when we need results. 
    def submitAll(self):
        with self.mutex:
            s=self.submitted
        if s:
            c_log('Submitted already',self)
        # drain the thread local queue before submitting so 
        # tasks cannot get recursively submitted
        queue=[]
        t_queue=SF_TASK_QUEUE.get()
        if t_queue.size()==0:
            c_log('Empty queue',self)     
        while len(t_queue):
            queue.append(t_queue.pop())
        while len(queue):
            queue.pop().submit()
            
        # If this Sf_SuperFuture is being submitted on a different thread to the
        # one which created it then it will not be submitted by submitting the
        # the thread local queue. However, we make the assumption that the 
        # Sf_SuperFuture is submitted on exit of this method. To fix this issue
        # we submit it here; note that double submission has not effect.
        self.submit()
    
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
        c_log( "Submit All",self)
        # Submit the current thread local task queue
        self.submitAll()
        c_log( "Submitted All",self)

        # This is where the work stealing logic starts
        # isDone() tells us if the thread would block if get() were called on
        # the future. We will try to work steal if the thread would block so as
        # not to 'waste' the thread. This if block is setting up the
        # log/tracking information
        if not self.future.isDone():
            c_log("Sleep",self)
            nap=True
        else:
            nap=False
            c_log("Get",self)
        # back control increasing backoff of the thread trying to work steal.
        # This is not the most efficient solution but as Sonic Field tasks are
        # generally large, this approach is OK for the current use. We back of
        # between 1 and 100 milliseconds. At 100 milliseconds we just stick at
        # polling every 100 milliseconds.
        back=1
        # This look steal work until the get() on the current task will 
        # not block.
        #
        # Note that this loop is where deadlock can happen because if a ring
        # of tasks forms this will loop/sleep for ever.
        #
        x=0
        wOn=False
        while not self.future.isDone():
            nap=self.steal(nap)
            # If the thread would block again or we are not able to steal as
            # nothing pending then we back off. 
            if nap==True:
                if x%10==100:
                    c_log("Non Pending")
                if back==100:
                    # Hacky way of detecting deadlock eventually
                    w=SF_NWAITING.incrementAndGet()
                    c_log('Waiting on',self,"->",self.future,w)
                    for i in range(0,10):
                        if w>SF_POOL.getActiveCount():
                            c_log('Deadlock')
                        Thread.sleep(10)                  
                    SF_NWAITING.decrementAndGet()
                else:                
                    Thread.sleep(back)
                back+=1
                if back>100:
                    back=100
            else:
                back=1
            x+=1
        
        # To get here we know this get will not block
        r = self.future.get()
        c_log("Wake")
        # Return the result if any
        return r

    # Proxy all other methods to the result of the future
    def __getattr__(self,name):
        # This prevents a recursive proxying of future
        if name=='future':
            raise Exception('No such method')
        c_log("Proxying: ",name)
        return getattr(self.get(),name)

    # If the return of the get is iterable then we delegate to it so that 
    # this super future appears to be its embedded task
    def __iter__(self):
        return iter(self.get())
        
    def __str__(self):
        return "SuperFuture:"+str(self.job_number)
            
    # Similarly for resource control for the + and - reference counting 
    # semantics for SF_Signal objects
    def __pos__(self):
        obj=self.get()
        return +obj

    def __neg__(self):
        obj=self.get()
        return -obj
    
# Perform a function in parallel. To prevent for formation of cycles of 
# futures (effectively deadlock) this code goes over the parameters looking
# for anything which is a future and then waits for those futures to complete
# before branching off into another threa
#
# do not capitalise this as that looks ugly for decorators.
class sf_parallel(object):

    def __init__(self,func):
        self.func=func

    @staticmethod
    def recursive_get_futures(to_get):
        if isinstance(to_get,Sf_SuperFuture):
            c_log('Getting: ',to_get)
            to_get.get()
        elif isinstance(to_get,(list,tuple)):
            for g in to_get:
                sf_parallel.recursive_get_futures(g)
        elif isinstance(to_get,dict):
            for key, value in to_get.iteritems():
               sf_parallel.recursive_get_futures(value)
                    
    def __call__(self,*args, **kwargs):
        # we need to make sure that all the futures in the current
        # context are completed before we schedule a new one or 
        # we risk forming cycles. This might seem highly inefficient
        # but it does not work out that way due to the lazy execution system
        #
        # One might thing that just checking the args is enough but because
        # functions in python are closures, it is not enough!
        if SF_DO_STEALING:
            sf_parallel.recursive_get_futures(locals())
        def closure():
            return self.func(*args, **kwargs)
        c_log('Parallel',self.func,closure) 
        if SF_PENDING.size()> SF_LINEAR_LIMIT or SF_LINEAR:
            return closure()
        else:
            return Sf_SuperFuture(closure)

# Shut the execution pool down. This waits for it to shut down
# but if the shutdown takes longer than timeout then it is 
# forced down.

# The default shutdown for the main pool  
def sf_shutdown():
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
    shutdown_and_await_termination(SF_POOL, 5)

import __builtin__
__builtin__.c_log=c_log
__builtin__.d_log=d_log
__builtin__.sf_parallel=sf_parallel
