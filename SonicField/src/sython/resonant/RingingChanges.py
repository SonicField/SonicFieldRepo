from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.resonant.Bells import primeBell
from sython.utils.Generative import BrownianWalk
from random import random, shuffle, randint

#sf.SetSampleRate(60000)

def randSwap(sequ):
    fm = randint(0, len(sequ) -1)
    to = randint(0, len(sequ) -1)
    tmp = sequ[to]
    sequ[to] = sequ[fm]
    sequ[fm] = tmp

@sf_parallel
def makeBlocks(length = 360000 * 20):
    with SFMemoryZone():
        
        class Walker(object):
            def __init__(self, frequency):
                
                self.haasWalkLeft  = BrownianWalk(maxDenominator = 3, maxNumerator = 10)
                self.haasWalkRight = BrownianWalk(maxDenominator = 3, maxNumerator = 10)
    
                # Balance walk which also gives the volume.
                self.balWalkLeft  = BrownianWalk()
                self.balWalkRight = BrownianWalk()
    
                # The frequency.
                self.frequency = frequency

                # Length walk.
                self.lenWalk = BrownianWalk(maxDenominator = 4, maxNumerator = 4)
                        
            def next(self):
                return {
                    'haasLeft'  : self.haasWalkLeft.next() * 10.0,
                    'haasRight' : self.haasWalkRight.next() * 10.0,
                    'balLeft'   : self.balWalkLeft.next(),
                    'balRight'  : self.balWalkRight.next(),
                    'length'    : self.lenWalk.next() * 2000,
                    'frequency' : self.frequency
                    }
        
        freqs = []
        for baseF in [64.0, 128.0, 256.0, 512.0, 1024.0]:
            freqs += [baseF, baseF * 4 / 3, baseF * 3 / 2]
            
        bells = [Walker(f) for f in freqs]
        step = 256
        steps = []
        for x in range(0, len(freqs)):
            if (x + 1) % 4 == 0:
                # Or *= 2 - which will sounds better?
                step += 128
            steps += [step]
        
        at = 1000
        sigsLeft  = []
        sigsRight = []
        count = 0
        while at < length:
            for step, bell in zip(steps, bells):
                with SFMemoryZone():
                    bellFo = bell.next()
                    bright = 3.0
                    if bellFo['frequency'] < 256:
                        bright = 4.0
                    sig = primeBell(
                        frequency = bellFo['frequency'], 
                        brightness = bright,
                        length = bellFo['length'],
                        hit = 2.0, 
                        isBowl = False
                    )
    
                    atLeft  = at + bellFo['haasLeft']
                    atRight = at + bellFo['haasRight']
                    sigsLeft  += [(sf.NumericVolume(sig,bellFo['balLeft']).flush(),  atLeft)]
                    sigsRight += [(sf.NumericVolume(sig,bellFo['balRight']).flush(), atRight)]
                    at += step
            count += 1
            if count % 2 == 0:
                randSwap(steps)
                randSwap(bells)
                for place in range(1, len(bells)):
                    n0 = bells[place -1]
                    n1 = bells[place]
                    if n1.frequency < n0.frequency:
                        tmp = n1.frequency
                        n1.frequency = n0.frequency
                        n0.frequency = tmp
                        
                for place in range(1, len(steps)):
                    n0 = steps[place -1]
                    n1 = steps[place]
                    if n1< n0:
                        tmp = n1
                        steps[place] = n0
                        steps[place - 1] = tmp
    
        return [sf.FixSize(sf.MixAt(sigs)).flush() for sigs in sigsLeft, sigsRight]
        
def main():
    print 'Doing work'
    '''
    voxl, voxr = makeBlocks()

    sf.WriteSignal(voxl, "temp/change_l")
    sf.WriteSignal(voxr, "temp/change_r")
    '''
    
    voxl = sf.ReadSignal("temp/change_l")
    voxr = sf.ReadSignal("temp/change_r")
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
        sf.WriteFile32((leftX,rightX), "temp/change{0}.wav".format(c))
        c += 1
        t = e

    print 'Done work'