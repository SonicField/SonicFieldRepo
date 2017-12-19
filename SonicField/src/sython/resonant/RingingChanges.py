from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.resonant.Bells import primeBell
from sython.utils.Generative import BrownianWalk
from random import random, shuffle, randint
from sython.utils.Splitter import writeWave


#sf.SetSampleRate(60000)

def randSwap(sequ):
    fm = randint(0, len(sequ) -1)
    to = randint(0, len(sequ) -1)
    tmp = sequ[to]
    sequ[to] = sequ[fm]
    sequ[fm] = tmp

@sf_parallel
def makeBlocks(length = 60000 * 1): # Minutes
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
        # For Choas and order
        #for baseF in [64.0, 128.0, 256.0, 512.0, 1024.0]:
        #    freqs += [baseF, baseF * 4 / 3, baseF * 3 / 2]
        # For Further Chaos
        for baseF in range(20,0,-1):
            freqs += [baseF * 64]

        bells = [Walker(f) for f in freqs]
        steps = []
        # For Choas and order
        '''
        step = 256
        for x in range(0, len(freqs)):
            if (x + 1) % 4 == 0:
                # Or *= 2 - which will sounds better?
                step += 128
            steps += [step]
        '''
        steps = []
        for _ in range(0,4):
            steps += [ ((x % 4) + 1)* 128 for x in range(0, len(freqs))]

        at = 1000
        sigsLeft  = []
        sigsRight = []
        count = 0
        while at < length:
            print 'At: ', at , ' of ', length
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
                    sigsLeft  += [(sf.LinearVolume(sig,bellFo['balLeft']),  atLeft)]
                    sigsRight += [(sf.LinearVolume(sig,bellFo['balRight']), atRight)]
                    at += step

            for _ in range(0, 4):
                randSwap(bells)
                randSwap(steps)


        return [sf.FixSize(sf.MixAt(sigs)).keep() for sigs in sigsLeft, sigsRight]

def main():
    print 'Doing work'

    voxl, voxr = makeBlocks()

    sf.WriteSignal(voxl, "temp/change_l")
    sf.WriteSignal(voxr, "temp/change_r")

    #voxl = sf.ReadSignal('temp/change_l')
    #voxr = sf.ReadSignal('temp/change_r')

    writeWave(voxl, voxr, 'temp/changes')

    print 'Done work'
