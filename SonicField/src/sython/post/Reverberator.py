from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.utils.Reverberation import reverberate
from sython.utils.Splitter import writeWave
from com.nerdscentral.audio.core import SFData

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
        left, right = sf.ReadFile("temp/dry.wav")
        #left  = sf.ReadSignal("temp/right_v1_acc")
        #right = sf.ReadSignal("temp/left_v1_acc")
        
        left =sf.Multiply(sf.NumericShape((0,0),(64,1),(sf.Length(+left ),1)),left )
        right=sf.Multiply(sf.NumericShape((0,0),(64,1),(sf.Length(+right),1)),right)
        
        left =sf.Concatenate(sf.Silence(1024),left).flush()
        right=sf.Concatenate(sf.Silence(1024),right).flush()
    
    
    ####################################
    #
    # Room Size And Nature Controls
    #
    ####################################
    
    # If true, do not perform secondary reverb.
    dry     = False
    # Perform excitation of primary convolution even when not bright or vbright.
    clear   = False
    # Perform brightening.
    bright  = True
    # Extra brightening of the dry signal.
    # vBright without bright will not brighten the wet signal.
    vBright = False
    # Use a church impulse response.
    church  = False
    # Use an very long 'ambient' impulse response.
    ambient = False
    # Use another very long impulse response.
    megaThe = False
    # Use an impulse response from an abandoned factory.
    terrys  = False
    # Post process which is a multi-band compress and adds warmth (valve like waveshaping).
    post    = True
    # Use a spring reverb' impulse response.
    spring  = False
    # EQ up the bass a little. This helps compensate for domination of highs when brightening.
    bboost  = False
    # The mix in the final. 0.0 implies pure wet; 1.0 is pure dry. Use 0.0 if you want to mix by hand.
    mix     = 0.25
    # The spring impulse response has a boomy signature at around 100Hz, this takes some of that out.
    lightenSpring = True
    
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
        if lightenSpring:
            spring =sf.RBJPeaking(spring,100,1,-1)

        convoll=sf.Mix(
           convoll,
            +spring
        )
        
        convorr=sf.Mix(
            convorr,
            sf.Invert(spring)
        )

    if terrys:
        ml,mr=sf.ReadFile("temp/impulses/terrys.wav")
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
            wleft =sf.FixSize(sf.Mix(ll,rl)).keep()
            wright=sf.FixSize(sf.Mix(rr,lr)).keep()
        
        if bright:
            wright = excite(wright,0.15,1.11)
            wleft  = excite(wleft ,0.15,1.11)
            right  = excite(right,0.15,1.05)
            left   = excite(left ,0.15,1.05)
            
        if vBright:
            right  = excite(right,0.25,1.15)
            left   = excite(left ,0.25,1.15)

        writeWave(wleft, wright, 'temp/step-1-reverb')

        SFData.flushAll()
        
        with SFMemoryZone():
            wleft  =sf.FixSize(sf.Mix(sf.NumericVolume(left,mix ),sf.NumericVolume(wleft,1.0-mix))).flush()
            wright =sf.FixSize(sf.Mix(sf.NumericVolume(right,mix),sf.NumericVolume(wright,1.0-mix))).flush()

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
                    vwleft =sf.FixSize(sf.Mix(ll,rl)).flush()
                    vwright=sf.FixSize(sf.Mix(rr,lr)).flush()
                    
                with SFMemoryZone():
                    wleft =sf.FixSize(sf.Mix(wleft ,sf.Pcnt20(vwleft ))).flush()
                    wright=sf.FixSize(sf.Mix(wright,sf.Pcnt20(vwright))).flush()

    writeWave(wleft, wright, 'temp/step-2-reverb')

    if post:
        print "Warming"
        
        left, right = wleft, wright
        
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
        writeWave(left, right, 'temp/step-3-filtered')
        
