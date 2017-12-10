from java.io import File
from com.nerdscentral.audio.core import SFConstants
import os

def writeSawppedCache(signal):
    dir = SFConstants.getRotatingTempDir()
    tmpf = File.createTempFile("cached" + str(os.getpid()), ".sig", dir)
    tmpf.deleteOnExit();
    name = tmpf.getCanonicalPath()
    sf.WriteSignal(signal, tmpf.getCanonicalPath());
    return name
    
def readSwappedCache(cachedFile):
    return sf.ReadSignal(cachedFile).realise()
