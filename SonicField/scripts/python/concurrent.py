import threading
import time
from java.util.concurrent import Executors, TimeUnit
from java.util.concurrent import Callable

SF_MAX_CONCURRENT = 6
#SF_POOL = Executors.newFixedThreadPool(SF_MAX_CONCURRENT)
#SF_POOL = Executors.newCachedThreadPool()
print SF_POOL
System.exit(0)

class sf_callable(Callable):
    def __init__(self,toDo):
        self.toDo=toDo
        
    def call(self):
        return self.toDo()

def sf_do(toDo):
    task=sf_callable(toDo)
    return SF_POOL.submit(task)

from java.util.concurrent import TimeUnit

def shutdown_and_await_termination(pool, timeout):
    pool.shutdown()
    try:
        if not pool.awaitTermination(timeout, TimeUnit.SECONDS):
            pool.shutdownNow()
            if (not pool.awaitTermination(timeout, TimeUnit.SECONDS)):
                print >> sys.stderr, "Pool did not terminate"
    except InterruptedException, ex:
        # (Re-)Cancel if current thread also interrupted
        pool.shutdownNow()
        # Preserve interrupt status
        Thread.currentThread().interrupt()
        
def shutdownConcurrnt():
    shutdown_and_await_termination(SF_POOL, 5)