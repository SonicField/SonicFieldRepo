from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.utils.Reverberation import reverberate
from sython.utils.Splitter import writeWave
from com.nerdscentral.audio.core import SFData
'''
@sf_parallel
def reverbInner(signal,convol,grainLength):
    with SFMemoryZone():    
        mag=sf.Magnitude(signal)
        if mag>0:
            signal_=sf.Concatenate(signal, sf.Silence(grainLength))
            signal_=sf.FrequencyDomain(signal_)
            signal_=sf.CrossMultiply(convol, signal_)
            signal_=sf.TimeDomain(signal_)
            newMag=sf.Magnitude(signal_)
            if newMag>0:
                signal_=sf.NumericVolume(signal_,mag/newMag)        
                # tail out clicks due to amplitude at end of signal 
                return signal_.flush()
            else:
                return sf.Silence(sf.Length(signal_)).flush()
        return signal

@sf_parallel   
def reverberate(signal,convol):
    with SFMemoryZone():
        grainLength = sf.Length(convol)
        convol_=sf.FrequencyDomain(sf.Concatenate(convol,sf.Silence(grainLength)))
        signal_=sf.Concatenate(signal,sf.Silence(grainLength))
        out=[]
        grains = []
        for grain in sf.Granulate(signal_,grainLength):
            signal_i, at =grain
            grains += [ (sf.SwapSignal(signal_i), at) ]

        for grain in grains:
            signal_i, at =grain
            out.append((reverbInner(signal_i, convol_, grainLength), at))
        return sf.Clean(sf.FixSize(sf.MixAt(out))).flush()
'''

@sf_parallel
def excite(sig,mix,power):
    with SFMemoryZone():    
        m=sf.Magnitude(sig)
        sigh=sf.BesselHighPass(sig,500,2)
        mh=sf.Magnitude(sigh)
        sigh=sf.Power(sigh,power)
        sigh=sf.Clean(sigh)
        sigh=sf.BesselHighPass(sigh,1000,2)
        nh=sf.Magnitude(sigh)
        sigh=sf.NumericVolume(sigh,mh/nh)
        sig=sf.Mix(sf.NumericVolume(sigh,mix),sf.NumericVolume(sig,1.0-mix))
        n=sf.Magnitude(sig)
        return sf.NumericVolume(sig, m/n).flush()

def main():
    ####################################
    #
    # Load the file and clean
    #
    ####################################
    
    with SFMemoryZone():    
        #left, right = sf.ReadFile("temp/dry.wav")
        left  = sf.ReadSignal("temp/left_v1_acc")
        right = sf.ReadSignal("temp/right_v1_acc")
        
        left =sf.Multiply(sf.NumericShape((0,0),(64,1),(sf.Length(+left ),1)),left )
        right=sf.Multiply(sf.NumericShape((0,0),(64,1),(sf.Length(+right),1)),right)
        
        left =sf.Concatenate(sf.Silence(1024),left).flush()
        right=sf.Concatenate(sf.Silence(1024),right).flush()
    
    
    ####################################
    #
    # Room Size And Nature Controls
    #
    ####################################
    
    dry     = False
    clear   = True
    bright  = True
    vBright = False
    church  = False
    ambient = False
    megaThe = False
    terys   = False
    post    = True
    spring  = True
    bboost  = False
    mix     = 0.0
      
    if ambient:  
        (convoll,convolr)=sf.ReadFile("temp/impulses/v-grand-l.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/v-grand-r.wav")
    elif church:    
        (convoll,convolr)=sf.ReadFile("temp/impulses/bh-l.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/bh-r.wav")
    else:
        (convoll,convolr)=sf.ReadFile("temp/impulses/Vocal-Chamber-L.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/Vocal-Chamber-R.wav")
    
    if spring:
        spring=sf.ReadFile("temp/impulses/classic-fs2a.wav")[0]
        convoll=sf.Mix(
           convoll,
            +spring
        )
        
        convorr=sf.Mix(
            convorr,
            sf.Invert(spring)
        )
    
    if megaThe:
        ml,mr=sf.ReadFile("temp/impulses/mega-thederal.wav")
        convoll=sf.Finalise(
            sf.Mix(
                convoll,
                ml
            )
        )
        
        convorr=sf.Finalise(
            sf.Mix(
                convorr,
                mr
            )
        )
    
    if bboost:
        left =sf.RBJPeaking(left,100,1,6)
        right=sf.RBJPeaking(right,100,1,6)
    
        #left =sf.RBJLowShelf(left,256,1,6)
        #right=sf.RBJLowShelf(right,256,1,6)
    
    if bright or vBright or clear:
        convoll=excite(convoll,0.75,2.0)
        convolr=excite(convolr,0.75,2.0)
        convorl=excite(convorl,0.75,2.0)
        convorr=excite(convorr,0.75,2.0)
    
    with SFMemoryZone():
        with SFMemoryZone():
            ll  = reverberate(left ,convoll)
            lr  = reverberate(left ,convolr)
            rl  = reverberate(right,convorl)
            rr  = reverberate(right,convorr)
            SFData.flushAll()
            wleft =sf.FixSize(sf.Mix(ll,rl)).keep()
            wright=sf.FixSize(sf.Mix(rr,lr)).keep()
            sf.WriteSignal(left ,"temp/left-l.sig")
            sf.WriteSignal(right,"temp/right-r.sig")    
            sf.WriteSignal(wleft ,"temp/wleft-l.sig")
            sf.WriteSignal(wright,"temp/wright-r.sig")    
        
        SFData.flushAll()
        
        if bright:
            wright = excite(wright,0.15,1.11)
            wleft  = excite(wleft ,0.15,1.11)
            right  = excite(right,0.15,1.05)
            left   = excite(left ,0.15,1.05)
            
        if vBright:
            right  = excite(right,0.25,1.15)
            left   = excite(left ,0.25,1.15)

        SFData.flushAll()
        
        with SFMemoryZone():
            wleft  =sf.FixSize(sf.Mix(sf.NumericVolume(left,mix ),sf.NumericVolume(wleft,1.0-mix)))
            wright =sf.FixSize(sf.Mix(sf.NumericVolume(right,mix),sf.NumericVolume(wright,1.0-mix)))
            writeWave(sf.FixSize(wleft), sf.FixSize(wright), "temp/bright-wet.wav")
            writeWave(sf.FixSize(left),  sf.FixSize(right),  "temp/bright-dry.wav")
            wleft=sf.SwapSignal(wleft)
            wright=sf.SwapSignal(wright)

        SFData.flushAll()

        if not dry:
            with SFMemoryZone():
                if ambient:
                    (convoll,convolr)=sf.ReadFile("temp/impulses/ultra-l.wav")
                    (convorl,convorr)=sf.ReadFile("temp/impulses/ultra-r.wav")
                elif church:
                    (convoll,convolr)=sf.ReadFile("temp/impulses/v-grand-l.wav")
                    (convorl,convorr)=sf.ReadFile("temp/impulses/v-grand-r.wav")
                else:
                    (convoll,convolr)=sf.ReadFile("temp/impulses/bh-l.wav")
                    (convorl,convorr)=sf.ReadFile("temp/impulses/bh-r.wav")
            
                with SFMemoryZone():
                    left  = sf.BesselLowPass(left ,392,1)
                    right = sf.BesselLowPass(right,392,1)
                    ll  = reverberate(+left ,convoll)
                    lr  = reverberate( left ,convolr)
                    rl  = reverberate(+right,convorl)
                    rr  = reverberate( right,convorr)
                    SFData.flushAll()
                    vwleft =sf.FixSize(sf.Mix(ll,rl))
                    vwright=sf.FixSize(sf.Mix(rr,lr))
                    SFData.flushAll()
                    sf.WriteSignal(wleft ,"temp/vwet-l.sig")
                    sf.WriteSignal(wright,"temp/vwet-r.sig")
                    vwleft=sf.SwapSignal(vwleft)
                    vwright=sf.SwapSignal(vwright)

                SFData.flushAll()
                    
                with SFMemoryZone():
                    wleft =sf.FixSize(sf.Mix(wleft ,sf.Pcnt20(vwleft )))
                    wright=sf.FixSize(sf.Mix(wright,sf.Pcnt20(vwright)))
                    sf.WriteSignal(wleft ,"temp/grand-l.sig")
                    sf.WriteSignal(wright,"temp/grand-r.sig")
                    writeWave(wleft, wright, 'temp/grand')

                SFData.flushAll()
        else:
            if post:
                sf.WriteSignal(left ,"temp/grand-l.sig")
                sf.WriteSignal(right,"temp/grand-r.sig")
    
    if post:
        print "Warming"
        
        left  = sf.ReadSignal("temp/grand-l.sig")
        right = sf.ReadSignal("temp/grand-r.sig")
        
        def highDamp(sig,freq,fact):
            with SFMemoryZone():
                hfq=sf.BesselHighPass(+sig,freq,4)
                ctr=sf.FixSize(sf.Follow(sf.FixSize(+hfq),0.25,0.5))
                ctr=sf.Clean(ctr)
                ctr=sf.RBJLowPass(ctr,8,1)
                ctr=sf.DirectMix(
                    1,
                    sf.NumericVolume(
                        sf.FixSize(sf.Invert(ctr)),
                        fact
                    )
                )
                hfq=sf.Multiply(hfq,ctr)
                return sf.Mix(hfq,sf.BesselLowPass(sig,freq,4)).flush()
        
        @sf_parallel
        def filter(sig):
            with SFMemoryZone():
                q=0.5
                sig=sf.Mix(
                    sf.Pcnt10(sf.FixSize(sf.WaveShaper(-0.03*q,0.2*q,0,-1.0*q,0.2*q,2.0*q,+sig))),
                    sig
                )
                sig=sf.RBJPeaking(sig,64,2,2)
                damp=sf.BesselLowPass(sig,2000,1)
                sig=sf.FixSize(sf.Mix(damp,sig))
                low=sf.BesselLowPass(sig,256,4)
                m1=sf.Magnitude(low)
                low=sf.FixSize(low)
                low=sf.Saturate(low)
                m2=sf.Magnitude(low)
                low=sf.NumericVolume(low,m1/m2)
                sig=sf.BesselHighPass(sig,256,4)
                sig=sf.Mix(low,sig)
                sig=highDamp(sig,5000,0.66)
                return sf.FixSize(sf.Clean(sig)).flush()
            
        left  = filter(left)
        right = filter(right)
        writeWave(left, right, 'temp/filtered')
        
