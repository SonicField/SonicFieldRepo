import threading
import collections

LengthFrequency = namedtuple('LengthFrequency', ['length', 'frequency'])

class SignalCache(object):

    def __init__(self):
        # Stors the realised signals keyed by frequency and length
        self.cache = {}
        # Stores LengthFrequency objects in the cache ordered by frequency.
        self.fMap = []
        self.readLock = threading.RLock()
        self.writeLock = threading.RLock()
    
    def getOrCreate(self, length, frequency, generator):
        with self.readLock:
            # Unlikely!
            wanted = LengthFrequency(length,frequency)
            if wanted in self.cache:
                return cache[wanted]
