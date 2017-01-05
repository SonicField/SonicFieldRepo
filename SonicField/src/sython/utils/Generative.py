import random
import math

def randWalk(value,size,uBound):
    value  = float(value)
    size   = float(size)
    uBound = float(uBound)
    r=(random.random()+random.random())/2.0
    r=math.floor(r*size)-math.floor((size/2.0))    
    value+=r
    if value<1:
        value=2
    elif value>uBound:
        value=uBound-2
    return value

def balancedRandom(scale):
    return (random.random() * scale) + (scale/2.0)

class BrownianWalk(object):
    
    def __init__(self, startNumerator = 1, startDenominator = 1, maxNumerator = 8, maxDenominator = 8, maxStep = 3):
        if maxNumerator < 3:
            raise ValueError("maxNumerator must be > 2 and is %s" % str(maxNumerator))
        if maxNumerator < 3:
            raise ValueError("maxDenominator must be > 2 and is %s" % str(maxDenominator))
        self._num = startNumerator
        self._denom = startDenominator
        self._maxNum = maxNumerator
        self._maxDenom = maxDenominator
        self._maxStep = maxStep
    
    def __iter__(self):
        return self
    
    def next(self):
        ret = float(self._num) / float(self._denom)
        self._num =  randWalk(self._num, self._maxStep, self._maxNum)
        self._denom =  randWalk(self._denom, self._maxStep, self._maxDenom)
        return ret

# Sieve of Eratosthenes
# Code by David Eppstein, UC Irvine, 28 Feb 2002
# http://code.activestate.com/recipes/117119/

def Primes():
    """ Generate an infinite sequence of prime numbers.
    """
    # Maps composites to primes witnessing their compositeness.
    # This is memory efficient, as the sieve is not "run forward"
    # indefinitely, but only as long as required by the current
    # number being tested.
    #
    D = {}
    
    # The running integer that's checked for primeness
    q = 2
    
    while True:
        if q not in D:
            # q is a new prime.
            # Yield it and mark its first multiple that isn't
            # already marked in previous iterations
            # 
            yield q
            D[q * q] = [q]
        else:
            # q is composite. D[q] is the list of primes that
            # divide it. Since we've reached q, we no longer
            # need it in the map, but we'll mark the next 
            # multiples of its witnesses to prepare for larger
            # numbers
            # 
            for p in D[q]:
                D.setdefault(p + q, []).append(p)
            del D[q]
        
        q += 1

def tweakRandom(values, scale):
    ret = []
    for value in values:
        ret += [(1.0 + balancedRandom(scale)) * value]
    return tuple(ret)

class PrimeRange(object):
    
    def __init__(self, biggerThan, lessThan, divisor):
        biggerThan = float(biggerThan)
        lessThan = float(lessThan)
        divisor = float(divisor)
        ret = []
        for v in Primes():
            v = float(v)
            if v < biggerThan:
                continue
            if v >= lessThan:
                break
            ret += [v / divisor]

        if not ret:
            raise ValueError('Empty prime range')
        self._value = tuple(ret)

    def __call__(self):
        return self._value