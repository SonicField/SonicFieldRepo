import math
import random
from com.nerdscentral.audio import SFData
    
def fixSize(signal):
    mag=sf.MaxValue(signal)
    return sf.NumericVolume(signal,1.0/mag)

def nullMixer():
    while(1):
        yield 0

def oscilator(damping,asym=1.0,mixer=0):
    damping = float(damping)
    lower   = -1.0
    upper   =  1.0
    weight  =  0.1
    value   =  0.0
    middle  = value
    gain    = 1.0
    prev    = 0.0
    cross   = 0.0
    pos     = 0.0
    gainV   = 0.9999
    xcount  = 0
    asym    = float(asym)
    
    yield 0,0,0
        
    while(1):
        if(value>middle):
            weight-=damping*asym
        else:
            weight+=damping

        if(mixer != 0):
            value+=mixer.next()
            
        value+=weight
        
        out=value*gain
        
        yield out,weight,xcount

        if(out<lower):
            value=prev
            gain*=gainV
        elif(out>upper):
            value=prev
            gain*=gainV
        elif(prev>0 and value<0):
            gain/=gainV
            xcount+=1
         
        pos+=1
        prev=value

def wobble(damping):
    wosc=oscilator(damping,1.0)
    while(1):
        s,t,xs=wosc.next()
        #print s
        yield s*0.00001

def invasion(d1,d2,seconds): 
    osc1=oscilator(d1,2,wobble(0.000020))
    osc2=oscilator(d1,2,wobble(0.000015))
    osc3=oscilator(d1,2,wobble(0.000010))
    osc4=oscilator(d1,2,wobble(0.000005))
    
    osc5=oscilator(d2,1.5,wobble(0.000020))
    osc6=oscilator(d2,1.5,wobble(0.000015))
    osc7=oscilator(d2,1.5,wobble(0.000010))
    osc8=oscilator(d2,1.5,wobble(0.000005))
        
    length=96000*seconds
    
    xs=0
    def drone(osc,len):
        def doDrone():
            data=SFData.build(len)
            print "Doing Drone"
            for x in range(0,length):
                s,t,xs=osc.next()
                data.setSample(x,s)
            # Go to a lot of effort to remove
            # clicks due to DC offset of the start and end
            l=sf.Length(data)
            data=sf.ButterworthHighPass(sf.Normalise(data),10,2)
            data=sf.Multiply(
                data,
                sf.NumericShape((0,0),(256,0),(l/2,1),(l-256,0),(l,0))
            )
            data=sf.Multiply(
                sf.Saturate(data),
                sf.NumericShape((0,0),(256,1),(l-256,1),(l,0))
            )
            return sf.Realise(data)
        return sf_do(doDrone)
    
    data1=drone(osc1,length)
    data2=drone(osc2,length)
    data3=drone(osc3,length)
    data4=drone(osc4,length)
    data5=drone(osc5,length)
    data6=drone(osc6,length)
    data7=drone(osc7,length)
    data8=drone(osc8,length)
    
    def mix1():
        return sf.Realise(
            fixSize(
                sf.MixAt(
                    (sf.Pcnt10(data2),30),
                    (sf.Pcnt20(data3),20),
                    (data1,0),
                    (data4,0),
                    (sf.Pcnt10(data6),30),
                    (sf.Pcnt20(data7),20),
                    (data5,0),
                    (data8,0)
                )
            )
        )
    
    def mix2():
        return sf.Realise(
            fixSize(
                sf.MixAt(
                    (sf.Pcnt10(data1),30),
                    (sf.Pcnt20(data4),20),
                    (data2,0),
                    (data3,0),
                    (sf.Pcnt10(data6),30),
                    (sf.Pcnt20(data7),20),
                    (data5,0),
                    (data8,0)
                    
                )
            )
        )
        
    dataL=sf_do(mix1)
    dataR=sf_do(mix2)
    return (dataL,dataR)

dataL1,dataR1=invasion(0.000025,0.000015,45)
dataL2,dataR2=invasion(0.000020,0.000007,45)
dataL3,dataR3=invasion(0.000011,0.000010,45)
dataL4,dataR4=invasion(0.000010,0.000012,45)
dataL=sf.Normalise(
    sf.MixAt(
        (dataL1, 0),
        (dataL2, 30000),
        (dataL3, 60000),
        (dataL1, 90000),
        (dataL4,120000),
        (dataL1,150000),
        (dataL4,160000)
    )
)

dataR=sf.Normalise(
    sf.MixAt(
        (dataR1,     0),
        (dataR2, 30000),
        (dataR3, 60000),
        (dataR1, 90000),
        (dataR4,120000),
        (dataR1,150000),
        (dataR4,160000)
    )
)
sf.WriteFile32((dataL,dataR),"temp/temp.wav")
dataL=0
dataR=0

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
 
(left,right)=sf.ReadFile("temp/temp.wav")

(convoll,convolr)=sf.ReadFile("temp/revb.wav")
wleft =reverberate(left,convoll)
wright=reverberate(right,convolr)

left=sf.Normalise(sf.MixAt(
    (sf.Pcnt40(wleft),10),
    (sf.Pcnt5(wright),40),
    (sf.Pcnt5(wleft),120),
    (sf.Pcnt45(left),0),
    (sf.Pcnt5(right),110)
))

right=sf.Normalise(sf.MixAt(
    (sf.Pcnt40(wright),10),
    (sf.Pcnt5(wleft),40),
    (sf.Pcnt5(wright),130),
    (sf.Pcnt45(right),0),
    (sf.Pcnt5(left),105)
))

sf.WriteFile32((left,right),"temp/temp_post.wav")

