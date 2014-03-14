import math
import random
import sys
sys.setrecursionlimit(8192)

def randWalk(value,size,uBound):
    value  = float(value)
    size   = float(size)
    uBound = float(uBound)
    r=(random.random()+random.random())/2.0
    r=math.floor(r*size)-math.floor((size/2.0))    
    value+=r
    if value<1:
        value=2
    elif value>uBound:
        value=uBound-2
    return value

def randWalk3(value,uBound):
    return randWalk(value,3,uBound)

def fixSize(signal):
    mag=sf.MaxValue(signal)
    return sf.NumericVolume(signal,1.0/mag)
 
def fixSizeSat(signal):
     return fixSize(sf.Saturate(fixSize(signal)))
    
def saturatedNode(beat,pPitch,pitch,a,d,s,r,v):
    def saturateNode_():
        l=a+d+s+r
        if l>beat*2:
            iPitch=(pitch+pPitch)/2.0
            pos=beat/8
            signal1=sf.Slide((0,iPitch),(pos,pitch),(l,pitch))
            signal2=sf.Slide((0,iPitch),(pos,pitch*2.015),(l,pitch*2.02))
            signal3=sf.Slide((0,iPitch),(pos,pitch*3.025),(l,pitch*3.03))
        else:
            signal1=sf.SineWave(l,pitch)
            signal2=sf.SineWave(l,2*pitch*1.003)
            signal3=sf.SineWave(l,3*pitch*1.005)
    
            vib=sf.Slide((0,6*random.random()),(l,0.5*random.random()))
            vib=sf.Multiply(sf.NumericShape((0,0.002),(l,0.001)),vib)
            vib=sf.DirectMix(1,vib)
            signal1 = sf.Resample(vib,signal1)

            vib=sf.Slide((0,6*random.random()),(l,0.5*random.random()))
            vib=sf.Multiply(sf.NumericShape((0,0.002),(l,0.001)),vib)
            vib=sf.DirectMix(1,vib)
            signal2 = sf.Resample(vib,signal2)
            
            vib=sf.Slide((0,6*random.random()),(l,0.5*random.random()))
            vib=sf.Multiply(sf.NumericShape((0,0.002),(l,0.001)),vib)
            vib=sf.DirectMix(1,vib)
            signal3 = sf.Resample(vib,signal3)
            
        envelope= sf.NumericShape(
                 (0,0),
                 (a,1),
                 (a+d,0.75),
                 (a+d+s,0.25),
                 (a+d+s+r,0)
        )
       
                        
        def doSat(sigIn):
                return sf.Normalise(sf.Clean(sf.Raise(sigIn,2)))    

        signal=sf.Mix(
            doSat(signal1),
            sf.DB_6(doSat(signal2)),
            sf.DB_15(doSat(signal3))
        )
        
        envelope= sf.NumericShape(
                 (0,0),
                 (a,0.5),
                 (a+d,1),
                 (a+d+s,0.5),
                 ((a+d+s+r)*2,0)
        )
        
        voice=sf.Multiply(
            sf.Normalise(sf.Power(sf.WhiteNoise(l),5)),
            envelope
        )
           
        signal=fixSize(signal)
        
        hf=sf.Clip(sf.NumericVolume(signal,3))
        hf=sf.Concatenate(hf,sf.Silence(l))
    
        r1=fixSizeSat(sf.RBJPeaking(hf,pitch*1.3,0.5,105))
        r2=fixSizeSat(sf.RBJPeaking(hf,pitch*2.1,0.5,105))
        r3=fixSizeSat(sf.RBJPeaking(hf,pitch*2.9,0.5,105))
    
        signal=sf.Mix(
            sf.DB_6(signal),
            sf.DB_1(r1),
            sf.DB_4(r2),
            sf.DB_6(r3)
        )
        
        f1=random.random()*200.0+600.0
        f2=random.random()*200.0+1600.0
        f3=random.random()*500.0+2300.0
        
        voice = sf.Clean(voice)
        voice = fixSize(voice)     
        voice = sf.Mix(
            sf.RBJPeaking(voice,f1,0.5,48),
            sf.RBJPeaking(voice,f2,0.3,48),
            sf.RBJPeaking(voice,f3,0.1,48)
        )
        voice =sf.ButterworthBandPass(voice,pitch,pitch*2,1)
        
        signal=sf.BesselLowPass(signal,pitch*2,2)
        signal=fixSize(sf.Mix(fixSize(signal),sf.DB_14(sf.Saturate(fixSize(voice)))))
        
        if l>beat*2:
            trem=sf.Slide((0,6*random.random()),(l*2,0.5*random.random()))
            trem=sf.Multiply(sf.NumericShape((0,0),(l*2,1)),trem)
            trem=sf.DirectMix(1,trem)
            signal = sf.Multiply(signal,trem)

        signal=sf.Clean(sf.NumericVolume(sf.Normalise(signal),v))
        envelope= sf.NumericShape(
                 (0,1),
                 (a+d+s+r-125,1),
                 (a+d+s+r,0)
        )
        signal=sf.Multiply(envelope,signal)        
        return signal
        
    return sf_do(saturateNode_)

def run(pitch,beat,minutes,startP,initial,overV):
    notesL=[]
    notesR=[]
    oPitch=float(pitch)
    pitchScaleDenom = 1.0
    pitchScaleNume  = float(startP)
    lengthScale     = 4.0
    volumeScale     = 4.0
    oVolume         = 4.0
    at=beat*float(initial)
    pPitch=float(pitch)

    while at/60000 < minutes:
        pitchScale = pitchScaleNume/pitchScaleDenom
        rvs        = 1.0/volumeScale
        volume     = rvs*oVolume
        pitch      = pitchScale*oPitch
        length     = lengthScale*beat

        
        # Create a consistent envelope
        a          = length*0.25
        d          = length*0.5
        s          = length*1.0
        r          = length*2.0
        if a<50:
            a=50
        if d<50:
            d=50
        if a>d-50:
           a=d/2
        
        r=r-s-d-a
        s=s-d-a
        d=d-a       
        
        vCorrection = 1/pitchScale
        
        # Do not over correct very & v high low frequencies 
        #  or very quiet notes. This aim it to stop loud highs
        #  dominating (psycho-acoustics)
        if rvs<0.2:
            if vCorrection<1:
                vCorrection=1
        
        if vCorrection>4:
            vCorrection=4
                                  
        print (
            at,
            "PitchNume: ",  pitchScaleNume,
            "PitchDenom: ", pitchScaleDenom,
            "Volume: ",     volumeScale,
            "Pitch: ",      pitch,
            "Length: ",     length,
            "Rvs: ",        rvs,
            "VCorr: ",      vCorrection
        ).__str__()    
            
        signal = saturatedNode(
            beat,
            pPitch,
            pitch,
            a,
            d,
            s,
            r,
            volume * vCorrection
        )

        lr=random.random()
        rl=1.0-lr
        notesL.append([sf.NumericVolume(signal,lr),at+30*rl])
        notesR.append([sf.NumericVolume(signal,rl),at+30*lr])

        at+=length * 8
        
        pitchScaleDenom = randWalk3(pitchScaleDenom,10)

        # permit half steps
        pitchScaleNume  = 0.5*randWalk3(pitchScaleNume*2.0,32)
        
        # and quater steps
        lengthScale     = 0.25*randWalk3(lengthScale*4,32)

        volumeScale     = randWalk3(volumeScale,8)
        
        pPitch          = pitch

    return (
        sf.NumericVolume(sf.Normalise(sf.Clean(sf.MixAt(notesL))),overV),
        sf.NumericVolume(sf.Normalise(sf.Clean(sf.MixAt(notesR))),overV)
    )

def compressInner(signal,amount):
    def compressInnerDo():
        if sf.MaxValue(signal)<0.001:
            return signal
        signal_=sf.Normalise(signal)
        stf=sf.Normalise(sf.ButterworthLowPass(signal_,128,2))
    
        offset=1.0-amount    
        sr=sf.Reverse(sf.Follow(sf.Reverse(stf),256,1024))
        sw=sf.Follow(stf,50,1024)
        shape=sf.Mix(sr,sw)
        div=1.0/sf.MaxValue(shape)
        shape=sf.NumericVolume(shape,div)
        shape=sf.DirectMix(offset,sf.NumericVolume(shape,amount))
        return sf.Normalise(sf.Divide(signal_,shape))
    return sf_do(compressInnerDo)

def compress(signal,amount):
    def compressDo():
        cpo=amount
        amount_=cpo*cpo 

        signalM=sf.BesselBandPass(signal,200,2000,4)
        signalM=compressInner(signalM, amount_)
        signalH=sf.BesselHighPass(signal    ,2000,4)
        signalH=compressInner(signalH, amount_)
        signalL=sf.BesselLowPass( signal    , 200,4)
        signalL=compressInner(signalL, amount_)
    
        signalM=sf.Clean(signalM)
        signalH=sf.Clean(signalH)
        signalL=sf.Clean(signalL)

        return sf.Normalise(sf.MixAt(
                (sf.Pcnt40(signalL),3.5),
                (sf.Pcnt20(signalM),0.0),
                (sf.Pcnt40(signalH),0.0)
            ))
    return sf_do(compressDo)
    

def reverbInner(signal,convol,grainLength):
    def reverbInnerDo():
        mag=sf.Magnitude(signal)
        if mag>0:
            signal_=sf.Concatenate(signal,sf.Silence(grainLength))
            signal_=sf.FrequencyDomain(signal_)
            signal_=sf.CrossMultiply(convol,signal_)
            signal_=sf.TimeDomain(signal_)
            newMag=sf.Magnitude(signal_)
            signal_=sf.NumericVolume(signal_,mag/newMag)        
            # tail out clicks due to amplitude at end of signal 
            l=sf.Length(signal_)
            sf.Multiply(
                sf.NumericShape(
                    (0,1),
                    (l-100,1),
                    (1,0)
                ),
                signal_
            )
            return signal_
        else:
            return signal
            
    return sf_do(reverbInnerDo)

def reverberate(signal,convol):
    def reverberateDo():
        grainLength = sf.Length(convol)
        convol_=sf.FrequencyDomain(sf.Concatenate(convol,sf.Silence(grainLength)))
        signal_=sf.Concatenate(signal,sf.Silence(grainLength))
        out=[]
        for grain in sf.Granulate(signal_,grainLength):
            (signal_,at)=grain
            out.append((reverbInner(signal_,convol_,grainLength),at))
        return sf.Normalise(sf.MixAt(out))
    return sf_do(reverberateDo)

def doRun1():
   return run(128,1024          ,30,1,0,1.0)
def doRun2():
   return run(128.0*4.0/3.0,1024,30,2,1,1.0)
def doRun3():
   return run(256.0*3.0/2.0,1024,30,1,5,0.5)
def doRun4():
   return run(512.0*5.0/4.0,1024,30,1,9,0.25)

random.seed(0.128)

"""
x1=sf_do(doRun1)
x2=sf_do(doRun2)
(left1,right1) = x1.get()
sf.WriteSignal(left1,"temp/l1")
sf.WriteSignal(right1,"temp/r1")

(left2,right2) = x2.get()
sf.WriteSignal(left2,"temp/l2")
sf.WriteSignal(right2,"temp/r2")

x3=sf_do(doRun3)
x4=sf_do(doRun4)

(left3,right3) = x3.get()
sf.WriteSignal(left3,"temp/l3")
sf.WriteSignal(right3,"temp/r3")

(left4,right4) = x4.get()
sf.WriteSignal(left4,"temp/l4")
sf.WriteSignal(right4,"temp/r4")
"""

left1=sf.ReadSignal("temp/l1")
left2=sf.ReadSignal("temp/l2")
left3=sf.ReadSignal("temp/l3")
left4=sf.ReadSignal("temp/l4")
left  = sf.Clean(sf.Normalise(fixSize(sf.Mix(left1,left2,left3,left4))))

sf.WriteSignal(left,"temp/l")
left=""

right1=sf.ReadSignal("temp/r1")
right2=sf.ReadSignal("temp/r2")
right3=sf.ReadSignal("temp/r3")
right4=sf.ReadSignal("temp/r4")
right = sf.Clean(sf.Normalise(fixSize(sf.Mix(right1,right2,right3,right4))))

sf.WriteSignal(right,"temp/r")
right=""

sf.WriteFile32((sf.ReadSignal("temp/l"),sf.ReadSignal("temp/r")),"temp/temp.wav")

(left,right)=sf.ReadFile("temp/temp.wav")

(convoll,convolr)=sf.ReadFile("temp/revb.wav")

"""
convoll=sf.Mix(
    convoll,
    sf.Pcnt15(sf.DirectRelength(convoll,0.2)),
    sf.Pcnt15(sf.Raise(sf.DirectRelength(convolr,0.2),2))
)
convolr=sf.Mix(
    convolr,
    sf.Pcnt15(sf.DirectRelength(convolr,0.2)),
    sf.Pcnt15(sf.Raise(sf.DirectRelength(convolr,0.2),2))
)
convoll=sf.Normalise(sf.Saturate(sf.Normalise(convoll)))
convolr=sf.Normalise(sf.Saturate(sf.Normalise(convolr)))
"""

wleft =reverberate(left,convoll)
wright=reverberate(right,convolr)

left=sf.Normalise(sf.MixAt(
    (sf.Pcnt60(wleft),10),
    (sf.Pcnt10(wright),40),
    (sf.Pcnt10(wleft),120),
    (sf.Pcnt15(left),0),
    (sf.Pcnt5(right),110)
))

right=sf.Normalise(sf.MixAt(
    (sf.Pcnt70(wright),10),
    (sf.Pcnt10(wleft),40),
    (sf.Pcnt10(wright),130),
    (sf.Pcnt20(right),0),
    (sf.Pcnt5(left),105)
))

left  = compress(left,0.66)
right = compress(right,0.66)

sf.WriteFile32((left,right),"temp/temp_post.wav")

(left,right)=sf.ReadFile("temp/temp_post.wav")
