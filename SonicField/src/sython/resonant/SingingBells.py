from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.resonant.Bells import primeBell
from sython.utils.Generative import BrownianWalk, PrimeRange, tweakRandom
from random import random, shuffle

#sf.SetSampleRate(60000)

@sf_parallel
def makeBlocks(length = 3600000, start = 10, rootFrequency = 256):
    with SFMemoryZone():
        # Ten times this is the hassDelay left and right.
        haasWalkLeft  = BrownianWalk(maxDenominator = 3, maxNumerator = 10)
        haasWalkRight = BrownianWalk(maxDenominator = 3, maxNumerator = 10)
    
        # Balance walk which also gives the volume.
        balWalkLeft  = BrownianWalk()
        balWalkRight = BrownianWalk()
    
        # Length walk.
        lenWalk = BrownianWalk(maxDenominator = 4, maxNumerator = 4)
        
        # Frequency walker.
        fSpread = 8 if rootFrequency > 1023 else 4 if rootFrequency > 255 else 3
        freqWalk = BrownianWalk(maxDenominator = fSpread, maxNumerator = fSpread)
        
        at = start
        sigsLeft  = []
        sigsRight = []
        while at < length:
            atLeft  = at + haasWalkLeft.next() * 10
            atRight = at + haasWalkRight.next()* 10
            thisLen = lenWalk.next() * 10000
            brightness = random() * 2 + 1.0
            hit = random() * 2 + 1.0
            isBowl = random() > 0.5
            isBowl = False
            frequency = freqWalk.next() * rootFrequency
            print "%6g, %6g, %6g, %6g, %6g, %3g, %s," % (frequency, atLeft, atRight, thisLen, brightness, hit, str(isBowl))
            sig = primeBell(frequency = frequency, brightness = 1.0, length = thisLen, hit = 1.0, isBowl = isBowl)
            sigsLeft  += [(sig, atLeft)]
            sigsRight += [(sig, atRight)]
            at += thisLen * 1.25
        print sigsLeft, sigsRight
        return [sf.SwapSignal(sf.FixSize(sf.MixAt(sigs))) for sigs in sigsLeft, sigsRight]
        
def main():
    with SFMemoryZone():
        print 'Doing work'
        start = 10
        voxl, voxr = makeBlocks(start = start, rootFrequency = 64)
        start += 10000
        tvoxl, tvoxr = makeBlocks(start = start, rootFrequency = 256)
        voxl = sf.Mix(voxl,tvoxl)
        voxr = sf.Mix(voxr,tvoxr)
        start += 10000
        tvoxl, tvoxr = makeBlocks(start = start, rootFrequency = 640)
        voxl = sf.Mix(voxl,tvoxl)
        voxr = sf.Mix(voxr,tvoxr)
        
        voxl, voxr = [sf.SwapSignal(sf.FixSize(vox)) for vox in voxl, voxr]

    sf.WriteSignal(voxl, "temp/bells_left_b")
    sf.WriteSignal(voxr, "temp/bells_right_b")

    t = 0
    l = max(sf.Length(voxl),sf.Length(voxr))
    c = 0
    while t<l: 
        e = t + 2400000.0
        e = min(l, e)
        # TODO is left and right are different lengths
        # will this fail?
        leftX  = sf.Cut(t, e, voxl)
        rightX = sf.Cut(t, e, voxr)
        sf.WriteFile32((leftX,rightX), "temp/bells{0}_b.wav".format(c))
        c += 1
        t = e

    print 'Done work'