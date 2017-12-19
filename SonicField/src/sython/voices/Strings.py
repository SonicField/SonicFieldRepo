import math
import random
from sython.voices.Signal_Generators import phasing_sawtooth
from Reverberation import reverberate
# Get IRs
violinIRs = sf.ViolinBodyIRs(())
violaIRs  = sf.ViolaBodyIRs(())
celloIRs  = sf.CelloBodyIRs(())
bassIRs   = sf.BassBodyIRs(())

#####################################################
# Create a nordic noir stay string                  #
# For more details see https://youtu.be/NtLsy9Bn_AE #
#####################################################
# e   = exponential envelope end
# a   = attack end
# d   = decay end
# dat = decay level
# s   = sustain end
# sat = sustain level
# r   = release end
# whiteAmount = amount of white noise (0 to 1)
# vibStart    = start time of vibrato
# vibMiddle   = middle time of vibrato
# vibAmount   = quantity of vibrato (0 to 1 sensible)
# vibRate     = rate of vibrato in cycles per second

# TODO remove this
def sf_do(what):
    return what()

@sf_parallel
def nordic_string(pitch,e,a,d,dat,s,sat,r,whiteAmount,vibStart,vibMiddle,vibAmount,vibRate=2.0,bright=True):
    
    def raw_string(length,pitch):
        def raw_stringA(l,p):
            return phasing_sawtooth(l,p)
        pitch=float(pitch)
        s1=raw_stringA(length,pitch)
        s2=raw_stringA(length,pitch*2.0)
        s3=raw_stringA(length,pitch*4.0)
        s4=sf.WhiteNoise(length)
        signal=sf.FixSize(
            sf.Mix(
                sf.Pcnt20(s4),
                sf.Pcnt50(s1),
                sf.Pcnt20(s2),
                sf.Pcnt10(s3)
            )
        )
        signal=sf.Clean(sf.ResonantFilter(signal,0.95,0.15,sf.Period(pitch)))
        multi=sf.Finalise(
            sf.DirectRelength(
                sf.ButterworthLowPass(sf.WhiteNoise(length/500.0),2500,6),
                0.001
            )
        )
        multi=sf.Cut(0,sf.Length(+signal),multi)
        signal=sf.Resample(
            sf.DirectMix(1,sf.LinearVolume(multi,0.001)),
            signal
        )
        return sf.Realise(sf.FixSize(sf.Clean(signal)))
    
    
    def play_string_clean(a,length,pitch,whiteAmount):
        def rsd():
            return raw_string(length,pitch)
        
        signal=0
        if(pitch>500):
            signal=sf.FixSize(sf.Mix(sf_do(rsd),sf_do(rsd),sf_do(rsd)))
        else:
            signal=sf.FixSize(sf.Mix(sf_do(rsd),sf_do(rsd)))

        if not bright:
            if(pitch>440):    
                signal=sf.ButterworthHighPass(signal,pitch*0.5,6)
                signal=sf.ButterworthHighPass(signal,2000,1)
                signal=sf.ButterworthLowPass(signal,8000,1)
            if(pitch<128):
                signal=sf.ButterworthHighPass(signal,pitch*0.5,1)
                signal=sf.ButterworthHighPass(signal,500,1)
                signal=sf.ButterworthLowPass(signal,2000,1)
            else:
                signal=sf.ButterworthHighPass(signal,pitch*0.5,3)
                signal=sf.ButterworthHighPass(signal,1500,1)
                signal=sf.ButterworthLowPass(signal,4000,1)
        
            signal=sf.ButterworthLowPass(signal,pitch*10.0,1)
            
        signal=sf.Mix(
            sf.Pcnt25(+signal),
            sf.Pcnt75(sf.RBJNotch(signal,pitch,0.5))
        )    

        white=sf.WhiteNoise(length)
        white=sf.ButterworthHighPass(white,pitch*2.0,2)
        white=sf.ButterworthLowPass(white,pitch*6.0,1)
        white=sf.FixSize(white)
        white=sf.Multiply(white,+signal)
        white=sf.LinearVolume(white,whiteAmount)
        signal=sf.LinearVolume(signal,1.0-whiteAmount)
        signal=sf.FixSize(sf.Mix(signal,white))
    
        sq=sf.Mix(
            sf.PhasedSineWave(length,pitch*0.95,random.random()),
            sf.PhasedSineWave(length,pitch*1.05,random.random())
        )
        envb=sf.LinearShape((0,0.25),(a,0),(length,0))
        sq=sf.Multiply(envb,sf.FixSize(sq))
    
        enva=sf.LinearShape((0,0.75),(a,1),(length,1))
        signal=sf.Multiply(enva,signal)
    
        signal=sf.Mix(sq,sf.FixSize(signal))

        env=sf.LinearShape((0,0),(16,1),(length-16,1),(length,0))
        signal=sf.Multiply(env,signal)
        
        sigs=[]
        bodies=[]
        if(pitch<128):
            bodies=bassIRs
        elif(pitch<440):
            bodies=celloIRs
        else:
            bodies=violinIRs

        if bright:
            bs=[]
            for b in bodies:
                bs.append(sf.Power(b,1.25))
            bodies=bs
            signal=sf.Power(signal,1.5)
        
        for body in bodies:
            sigs.append(reverberate(+signal,+body))  
        -signal

        signal=sf.FixSize(sf.Mix(sigs))
        return signal

    c_log("Performing Note: ",pitch,e,a,d,dat,s,sat,r,whiteAmount,vibStart,vibMiddle,vibAmount,vibRate)
    envA=sf.ExponentialShape(
        (0,-60),
        (e,0),
        (d,0),
        (s,0),
        (r,0)
    )
    envB=sf.LinearShape(
        (0,0),
        (a,1),
        (d,dat),
        (s,sat),
        (r,0)
    )
    env=sf.Multiply(envA,envB)
    sigs=[]
    for x in range(0,5):
        sigs.append(play_string_clean(a,r,pitch,whiteAmount))
    signal=sf.FixSize(sf.Mix(sigs))
    signal=sf.Multiply(signal,env)
    if(vibAmount>0):
        l=sf.Length(+signal)
        env=sf.LinearShape((0,0),(vibStart,0),(vibMiddle,1),(r,0.75),(l,0))
        env=sf.LinearVolume(env,vibAmount)
        trem=sf.SineWave(l,2.0+random.random())
        trem=sf.Multiply(env,trem)
        vib=+trem
        trem=sf.DirectMix(1,sf.Pcnt50(trem))
        signal=sf.Multiply(trem,signal)
        vib=sf.DirectMix(1,sf.LinearVolume(vib,0.01))
        signal=sf.Resample(vib,signal)
    
    if(pitch>128):
        signal=sf.ButterworthHighPass(signal,pitch*0.75,6)
        if not bright:
            signal=sf.BesselLowPass(signal,pitch,1)
    else:
        signal=sf.ButterworthHighPass(signal,pitch*0.75,3)
        
    env=sf.LinearShape((0,0),(16,1),(r-16,1),(r,0))
    signal=sf.Multiply(env,signal)
    return sf.Realise(sf.FixSize(sf.Clean(signal)))

@sf_parallel
def nordic_string_super_soft(pitch,length,volume):
    if(pitch<256):
        w=0.75
    else:
        if(pitch<720):
            w=0.33
        else:
            w=0.25
    sig = sf.LinearVolume(
        nordic_string(
            pitch,
            64,                 # e
            length*0.25,        # a
            length*0.50,        # d
            1.0,                # d at
            length*0.75,        # s
            1.0,                # s at
            length,             # r
            w,                  # white amount  
            length*0.50,        # vib start
            length*0.75,        # vib middle
            0.5                 # vib amount
        ),
        volume
    )
    env   = sf.LinearShape((0,0),(length*0.25,1),(length,1))
    return sf.Finalise(sf.Multiply(env,sig))

@sf_parallel
def nordic_string__soft_short(pitch,length,volume):
    def nordic_stringSoftShortInner():
        if(pitch<256):
            w=0.5
        else:
            if(pitch<720):
                w=0.25
            else:
                w=0.15
        return sf.Clean(sf.LinearVolume(
            nordic_string(
                pitch,
                32,                 # e
                64,                 # a
                length*0.15,        # d
                1.0,                # d at
                length*0.5,         # s
                0.5,                # s at
                length,             # r
                w,                  # white amount  
                length*0.50,        # vib start
                length*0.75,        # vib middle
                0.5                 # vib amount
            ),
            volume
        ))
    return sf_do(nordic_stringSoftShortInner)

@sf_parallel
def nordic_string_hard_long(pitch,length,volume):
    if(pitch<256):
        w=0.1
    else:
        if(pitch<720):
            w=0.1
        else:
            w=0.05
    sig=nordic_string(
        pitch,
        32,                 # e
        64,                 # a
        length*0.25,        # d
        1.0,                # d at
        length*0.75,        # s
        0.75,               # s at
        length,             # r
        w,                  # white amount  
        length*0.25,        # vib start
        length*0.75,        # vib middle
        0.5                 # vib amount - no vib in this case
    )
    return sf.Clean(sf.LinearVolume(sig,volume))

@sf_parallel
def nordic_string_voice(pitch,length,volume):
    if(pitch<256):
        w=0.1
    else:
        if(pitch<720):
            w=0.1
        else:
            w=0.05
        
    sig=nordic_string(
        pitch,
        32,                 # e
        64,                 # a
        128,                # d
        1.0,                # d at
        length,             # s
        0.75,               # s at
        length*1.25,        # r
        w,                  # white amount  
        length*0.25,        # vib start
        length*0.75,        # vib middle
        0.5                 # vib amount - no vib in this case
    )
    return sf.Clean(sf.LinearVolume(sig,volume))

@sf_parallel
def nordic_string_hard_short(pitch,length,volume):
    if(pitch<256):
        w=0.1
    else:
        if(pitch<720):
            w=0.1
        else:
            w=0.05
    return sf.Clean(sf.LinearVolume(
        nordic_string(
            pitch,
            32,                 # e
            64,                 # a
            length*0.125,       # d
            1.0,                # d at
            length*0.75,         # s
            0.75,               # s at
            length,             # r
            w,                  # white amount  
            length*0.25,        # vib start
            length*0.75,        # vib middle
            0                   # vib amount - no vib in this case
        ),
        volume
    ))

@sf_parallel
def nordic_string_pluck(pitch,length,volume):
    sig=nordic_string(
            pitch,
            8,                  # e
            16,                 # a
            32,                 # d
            0.5,                # d at
            length*0.75,        # s
            0.25,               # s at
            length,             # r
            0,                  # white amount  
            length*0.50,        # vib start
            length*0.90,        # vib middle
            1                   # vib amount - no vib in this case
    )
    envH=sf.LinearShape((0,0),(32,1),(length,0))
    envL=sf.LinearShape((0,1),(32,0),(length,1))
    sig=sig.get()
    sigL=sf.ButterworthLowPass(+sig,pitch,1)
    sigL=sf.ButterworthLowPass(sigL,pitch*3,1)
    sigH=sf.Multiply(sig,envH)
    sigL=sf.Multiply(sigL,envL)
    sig=sf.Mix(sigL,sigH)
    sig=sf.LinearVolume(sig,volume)
    return sig


