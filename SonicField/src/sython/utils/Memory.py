from java.io import File
from com.nerdscentral.audio.core import SFConstants
import os
import time
from java.util.concurrent.atomic import AtomicLong

TEMP_FILES = {}
def popTempFiles():
    global TEMP_FILES
    for dir in SFConstants.TEMP_DATA_DIRS:
        dir = File(dir)
        tmpf = File.createTempFile("cached" + str(os.getpid()), ".pool", dir)
        fileName = tmpf.getCanonicalPath()
        TEMP_FILES[dir.getCanonicalPath()] = fileName
        tmpf.deleteOnExit()

popTempFiles()

_TIME0 = time.time()
_READ_COUNT = AtomicLong()
_WRITE_COUNT = AtomicLong()

def maybeLog():
    global _TIME0, _READ_COUNT, _WRITE_COUNT
    t1 = time.time()
    if t1 -_TIME0 > 5.0:
        _TIME0 = t1
        print "Cache-pool Writes: %s, Reads %s" % (_WRITE_COUNT.get() / SFConstants.ONE_MEG, _READ_COUNT.get() / SFConstants.ONE_MEG)

def writeSawppedCache(signal):
    global _TIME0, _WRITE_COUNT
    dir = SFConstants.getRotatingTempDir().getCanonicalPath()
    fileName = TEMP_FILES[dir]
    pos = sf.WriteToSignalPool(signal, fileName);
    _WRITE_COUNT.getAndAdd(signal.getLength())
    maybeLog()
    return (fileName, pos)
    
def readSwappedCache(cachedInfo):
    global _TIME0, _READ_COUNT
    signal = sf.ReadFromSignalPool(cachedInfo[0], cachedInfo[1]).realise()
    _READ_COUNT.getAndAdd(signal.getLength())
    maybeLog()
    return signal

