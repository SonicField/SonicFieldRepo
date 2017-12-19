from sython.concurrent import sf_parallel
from sython.voices.ResonantVoices import make_addtive_resonance
from sython.voices.ResonantVoices import femail_soprano_ah_filter
from sython.utils.Algorithms import excite
from random import random, shuffle

def balancedRandom(scale):
    return (random() * scale) + (scale/2.0)

@sf_parallel
def engineSection(highOnly=False):
    lgth = 30000 + random()*1000
    generatorH = make_addtive_resonance(qCorrect = 1.0, post = femail_soprano_ah_filter, rollOff = 3.0, harmonics = xrange(1,10))
    high = sf.Pcnt10(generatorH(lgth, 400 + 200*random()))
    if highOnly:
        return high
    
    #generatorL = make_addtive_resonance(qCorrect = 2.0, post = femail_soprano_ah_filter, rollOff = 3.0)
    harmonics = [1, 1.4, 3.1, 5.2, 7.4, 9.6]
    harmonics = [x + (x * balancedRandom(0.2)) for x in harmonics]
    generatorB = make_addtive_resonance(qCorrect = 2.0, rollOff = 2.0, harmonics=harmonics, saturate = 0.1, power = 2.0)
    return sf.Mix( generatorB(lgth, 32), high)
    #generator = make_addtive_resonance(qCorrect = 2.0, rollOff = 5.0, power=1.0)
    #return (generatorL(4000, 220), generatorH(4000, 440), generatorH(2000, 440*4/5), generatorL(4000, 440 * 3/4))

@sf_parallel
def channel():
    sigs = []
    at = 0
    for x in range(1,200):
        sig=engineSection(x==1)
        lgth = sf.Length(sig)
        sig = sf.Multiply(
            sf.LinearShape((0,0),(lgth*0.5,1),(lgth,0)),
            sig
        )
        sigs += [(sig, at)]
        at += lgth * 0.5
    rest = sigs[2:]
    for _ in range(1,20):
        shuffle(rest)
        for sig, _ in rest:
            sigs += [(sig, at)]
            at += sf.Length(sig) * 0.5
    return sf.Realise(sf.FixSize(sf.MixAt(sigs)))
        
def main():
    print 'Doing work'
    voxl = channel()
    voxr = channel()
    sf.WriteSignal(voxl, "temp/draken_left")
    sf.WriteSignal(voxr, "temp/draken_right")
    t = 0
    l = max(sf.Length(voxl),sf.Length(voxr))
    c = 0
    while t<l: 
        e = t + 1800000.0
        e = min(l, e)
        # TODO is left and right are different lengths
        # will this fail?
        leftX  = sf.Cut(t, e, voxl)
        rightX = sf.Cut(t, e, voxr)
        sf.WriteFile32((leftX,rightX), "temp/draken_{0}.wav".format(c))
        c += 1
        t = e

    print 'Done work'