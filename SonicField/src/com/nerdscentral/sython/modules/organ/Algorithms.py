import math
import random
import sys
from Parallel_Helpers import mix,realise,finalise
from Reverberation import reverberate

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
    return freq*q

@sf_parallel
def do_formant(sig,f1,f2,f3,freq,intensity=4):
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
    x=sf.Check(x)
    return x
    
@sf_parallel
def echo_division(vox_):
    vox=vox_
    m1=sf.Magnitude(+vox)
    if m1==0.0:
        return vox
    length=sf.Length(+vox)
    convol=sf.ReadFile("temp/swell.wav")
    voxW=reverberate(+vox ,convol[0])
    vox=realise(vox,voxW)
    c_log("Reference count:",vox.getReferenceCount())
    m2=sf.Magnitude(+vox)
    vox=realise(sf.NumericVolume(vox,m1/m2))
    return vox
  
@sf_parallel
def tremolate(vox_,rate,mag):
    vox=vox_
    m1=sf.Magnitude(+vox)
    if m1==0.0:
        return vox
    length=sf.Length(+vox)
    ev=sf.NumericVolume(sf.MakeTriangle(sf.PhasedSineWave(length+64,rate,random.random())),mag)
    ev=sf.Cut(0,length,ev)
    fv=sf.Pcnt2(+ev)
    ev=sf.DirectMix(1.0,ev)
    vox=sf.FrequencyModulate(vox,fv)
    vox=sf.Multiply(ev,vox)
    convol=sf.ReadFile("temp/swell.wav")
    voxW=reverberate(+vox ,convol[0])
    vox=realise(mix(vox,voxW))
    m2=sf.Magnitude(+vox)
    vox=realise(sf.NumericVolume(vox,m1/m2))
    return vox

@sf_parallel
def polish(sig,freq):
    if freq > 128:
        sig=sf.ButterworthHighPass(sig,freq*0.66,6)
    elif freq > 64:
        sig=sf.ButterworthHighPass(sig,freq*0.66,4)
    else:
        sig=sf.ButterworthHighPass(sig,freq*0.66,2)   
    return sf.Clean(sig)

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