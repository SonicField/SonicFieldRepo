import __builtin__
import math
import random
import sys
sys.path.append("/Users/alexanderturner/SonicFieldRepo/SonicField/patches/python")
from reverberate import reverberate
from java.math import BigInteger

def nextPrime(numb):
    bn=BigInteger(int(numb).__str__())
    bn=bn.nextProbablePrime()
    return bn.doubleValue()

def make():
    convol=sf.Multiply(
        sf.SimpleShape(
            (0,-90),
            (90,-20),
            (128,0),
            (65536,-60)
        ),
        sf.Normalise(sf.Power(sf.WhiteNoise(65536),8))
    )
    
    revb=revb0=sf.NumericShape(
        (0,0),
        (0.025,1),
        (0.05,0),
        (0.75,-1),
        (0.1,0)
    )
    sigs=[]
    for x in range(1,4):
        revb=reverberate(revb,convol)
        sigs.append(revb)
        revb=sf.Normalise(sf.RTrim(sf.MixAt((revb0,0),(sf.Pcnt10(revb),10*random.random()))))
        revb=sf.ButterworthHighPass(revb,16,1)
        revb=sf.BesselLowPass(revb,2000,1)
        print x
    return sf.Normalise(sf.Mix(sigs))

sf.WriteFile32((sf_do(make),sf_do(make)),"temp/revb.wav")
