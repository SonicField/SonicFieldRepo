import math
import random
import sys
from sython.concurrent import sf_parallel
from sython.utils.Parallel_Helpers import mix,realise,finalise
from sython.utils.Reverberation import convolve

@sf_parallel
def echo_division(sig_):
    sig=sig_
    m1=sf.Magnitude(+sig)
    if m1==0.0:
        return sig
    length=sf.Length(+sig)
    convol=sf.ReadFile("temp/impulses/swell.wav")
    sigW=convolve(+sig ,convol[0])
    sig=realise(sig,sigW)
    m2=sf.Magnitude(+sig)
    sig=realise(sf.NumericVolume(sig,m1/m2))
    return sig
  
@sf_parallel
def tremolate(sig_,rate,mag):
    sig=sig_
    m1=sf.Magnitude(+sig)
    if m1==0.0:
        return sig
    length=sf.Length(+sig)
    ev=sf.NumericVolume(sf.MakeTriangle(sf.PhasedSineWave(length+64,rate,random.random())),mag)
    ev=sf.Cut(0,length,ev)
    fv=sf.Pcnt2(+ev)
    ev=sf.DirectMix(1.0,ev)
    sig=sf.FrequencyModulate(sig,fv)
    sig=sf.Multiply(ev,sig)
    convol=sf.ReadFile("temp/impulses/swell.wav")
    sigW=convolve(+sig ,convol[0])
    sig=mix(sig,sigW)
    m2=sf.Magnitude(+sig)
    sig=realise(sf.NumericVolume(sig,m1/m2))
    return sig

@sf_parallel
def pitch_move(sig):
    l=sf.Length(+sig)
    if l>1024:
        move=sf.NumericShape(
            (0,0.995+random.random()*0.01),
            (l,0.995+random.random()*0.01)
        )
    elif l>512:
        move=sf.NumericShape(
            (0,0.9975+random.random()*0.005),
            (l,0.9975+random.random()*0.005)
        )
    else:
        return sig
    return sf.Clean(sf.Resample(move,sig))

