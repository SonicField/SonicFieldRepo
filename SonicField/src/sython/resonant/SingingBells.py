from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.voices.ResonantVoices import make_addtive_resonance
from sython.voices.ResonantVoices import femail_soprano_ah_filter
from sython.utils.Algorithms import excite
from sython.utils.Generative import BrownianWalk, PrimeRange, tweakRandom
from random import random, shuffle

#sf.SetSampleRate(60000)

dullRange = PrimeRange(biggerThan = 9, lessThan = 30,  divisor = 10.0)
brightRange = PrimeRange(biggerThan = 9, lessThan = 120, divisor = 10.0)

@sf_parallel
def bell(frequency = 440 , brightness = 1.0, length = 10000, hit = 1.0, isBowl = True):
    
    with SFMemoryZone():
        saturate = 0.0
        qCorrect = 3.0
        if frequency > 2000:
            # Ouch.
            frequency *= 0.5
            saturate = 0.1
            qCorrect = 1.0
        
        qc = 1.0 if brightness < 2.0 else 3.0
        harmonics = dullRange if brightness < 2.0 else brightRange
        harmonics = tweakRandom(harmonics(), 0.05)
        
        if isBowl:
            length += 4000 + length
        
        with SFMemoryZone():
            gen = make_addtive_resonance(
                qCorrect = qCorrect, 
                post = None, 
                rollOff = 3.0, 
                power = brightness, 
                harmonics = harmonics,
                saturate = saturate
            )
            sig = gen(length, frequency).keep()
        
        # Hit processing with resonance if is a bowl.
        peak = 2000 if isBowl else 1
        env = sf.NumericShape(
            (0, frequency if isBowl else 18000),
            (peak, 18000 if isBowl else frequency * 4.0),
            (length, frequency)
        )
        res = sf.NumericShape((0,1.0),(length,3.0 if isBowl else 1.5))
        #sig = sf.ShapedLadderLowPass(sig,env,res)
        sig = sf.ShapedRBJLowPass(sig, env, res)
    
        # A mixture of linear and exponential enveloping.
        env = sf.Multiply(
            sf.NumericShape(
                (0, 0),
                (peak, 1),
                (length, 0)),
            sf.SimpleShape((0, 0), (peak, 0), (length, -30))
        )
        out = sf.FixSize(sf.Multiply(env, sig))
        return sf.SwapSignal(out)

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
            sig = bell(frequency = frequency, brightness = 1.0, length = thisLen, hit = 1.0, isBowl = isBowl)
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