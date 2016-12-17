import math
import random
import sys
from sython.concurrent import sf_parallel

@sf_parallel
def excite(sig_,mix_ammount,power):
    sig=sig_
    m=sf.Magnitude(+sig)
    sigh=sf.BesselHighPass(+sig,500,2)
    mh=sf.Magnitude(+sigh)
    sigh=sf.Power(sigh,power)
    sigh=sf.Clean(sigh)
    sigh=sf.BesselHighPass(sigh,1000,2)
    nh=sf.Magnitude(+sigh)
    sigh=sf.NumericVolume(sigh,mh/nh)
    sig=mix(sf.NumericVolume(sigh,mix_ammount),sf.NumericVolume(sig,1.0-mix_ammount))
    n=sf.Magnitude(+sig)
    return realise(sf.NumericVolume(sig,m/n))
    
def find_nearest_overtone(fMatch,freq):
    q=float(fMatch)/float(freq)
    q=int(q)
    if q==0:
        return q==1
    return freq*q

@sf_parallel
def do_formant(sig,f1,f2,f3,freq,intensity=4):
    if not sf.Check(+sig):
        d_log('Incoming broken')
    y=+sig
    f1b=f1
    f2b=find_nearest_overtone(f2,freq)
    f3b=find_nearest_overtone(f3,freq)
    for x in range(1,intensity):
        s1=sf.RBJBandPass(+sig,f1b,0.25)
        s2=sf.RBJBandPass(+sig,f2b,0.5)
        s3=sf.RBJBandPass(+sig,f3b,0.5)
        sig=sf.FixSize(
            mix(
                sf.Pcnt10(sig),
                sf.Pcnt50(sf.FixSize(s1)),
                sf.Pcnt20(sf.FixSize(s2)),
                sf.Pcnt30(sf.FixSize(s3))
            )
        )
        s1=sf.RBJPeaking(+sig,f1b,1.0,5)
        s2=sf.RBJPeaking(+sig,f2b,2.0,5)
        s3=sf.RBJPeaking( sig,f3b,2.0,5)
        sig=sf.FixSize(
            mix(
                sf.Pcnt50(sf.FixSize(s1)),
                sf.Pcnt20(sf.FixSize(s2)),
                sf.Pcnt30(sf.FixSize(s3))
            )
        )

    x=polish(sig,freq)
    x=sf.FixSize(x)
    if not sf.Check(+x):
        -x
        x=y
        d_log('Formant Failed',f1,f2,f3,freq,intensity)
    else:
        -y
    return x

@sf_parallel
def create_vibrato(sig,length,longer_than=0.0,rate=4.5,at=None,depth=0.2,pitch_depth=0.1):
    if length<longer_than:
        return sig
        
    if at is None:
        at=length*0.5

    ev=sf.NumericVolume(
        sf.SineWave(
            length,
            rate
        ),
        depth
    )
    ev=sf.Multiply(
        ev,
        sf.NumericShape((0,0),(length*0.5,1),(length,1))
    )
    fv=sf.NumericVolume(+ev,pitch_depth)
    ev=sf.DirectMix(1.0,ev)
    sig=sf.FrequencyModulate(sig,fv)
    return sf.Multiply(ev,sig)