from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone, SFConstants
from sython.utils.Reverberation import reverberate
from sython.utils.Splitter import writeWave
from sython.post.Declick import declick
from com.nerdscentral.audio.core import SFData
from __builtin__ import None

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
        sigh=sf.LinearVolume(sigh,mh/nh)
        sig=sf.Mix(sf.LinearVolume(sigh,mix),sf.LinearVolume(sig,1.0-mix))
        n=sf.Magnitude(sig)
        return sf.LinearVolume(sig, m/n).keep()

def main():
    paths = range(0, 8)
    #paths = [0]
    for path in paths:
        _main(path)

def _main(path):

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
    # A super short hand made impulse.
    tiny = False
    # A small chamber (smaller than the default vocal chamber).
    # This also shortens off the spring a lot to remove long rumbling tails
    # if the spring is used with this.
    small   = True
    # Use a church impulse response.
    church  = False
    # Use an very long 'ambient' impulse response.
    ambient = False
    # Use another very long impulse response.
    megaThe = 0
    #  An impulse from Perth City Hall.
    perth = 0.02
    # Use an impulse response from an abandoned factory.
    terrys  = 0.0
    # Bright medium Ir.
    club = 0.0
    # Enhanced rich church - very long.
    ultraChurch = 0.0
    # Post process which is a multi-band compress and adds warmth (valve like waveshaping).
    post    = False
    # Use a spring reverb' impulse response.
    spring  = False
    # EQ up the bass a little. This helps compensate for domination of highs when brightening.
    bboost  = False
    # The mix in the final. 0.0 implies pure wet; 1.0 is pure dry. Use 0.0 if you want to mix by hand.
    mix     = 0.50
    # The spring impulse response has a boomy signature at around 100Hz, this takes some of that out.
    lightenSpring = False
    # Will the outgoing volumes match the incoming ratio of magnituds
    matchMagnitudes = True
    # To multiply each track with dbsPerTrack * path.
    dbsPerTrack = -1.00
    # Use the declicker.
    doDeclick = False

    ####################################
    #
    # Load the file and clean
    #
    ####################################

    with SFMemoryZone():
        #right, left = sf.ReadFile("temp/dry.wav")
        right  = sf.ReadSignal(SFConstants.STORE_DIRECTORY + "right_v1_{0}_acc".format(path))
        left = sf.ReadSignal(SFConstants.STORE_DIRECTORY + "left_v1_{0}_acc".format(path))

        lrBalance = None
        if matchMagnitudes:
            rMag = sf.Magnitude(right)
            lMag = sf.Magnitude(left)
            tMag = lMag + rMag
            lrBalance = (lMag / tMag, rMag / tMag)
            print 'Will correct magnitudes to:', lrBalance
        declickThresh = [0.013, 0.015,  0.03, 0.04, 0.05, 0.06, 0.08, 0.10][path]
        cutoff        = [800,    1000,  1000, 2000, 2000, 3000, 3000, 4000][path]
        if doDeclick:
            left  = declick(left,  thresh = declickThresh, cutoff = cutoff)
            right = declick(right, thresh = declickThresh, cutoff = cutoff)
        left  = sf.Multiply(sf.LinearShape((0, 0), (64, 1),(sf.Length(+left ), 1)), left )
        right = sf.Multiply(sf.LinearShape((0, 0), (64, 1),(sf.Length(+right), 1)), right)

        left =sf.Concatenate(sf.Silence(1025),left).keep()
        right=sf.Concatenate(sf.Silence(1024),right).keep()

    if ambient:
        (convoll,convolr)=sf.ReadFile("temp/impulses/v-grand-l.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/v-grand-r.wav")
    elif church:
        (convoll,convolr)=sf.ReadFile("temp/impulses/bh-l.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/bh-r.wav")
    elif tiny:
        (convoll,convolr)=sf.ReadFile("temp/impulses/tiny-l.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/tiny-r.wav")
    elif small:
        (convoll,convolr)=sf.ReadFile("temp/impulses/Small-Chamber-L.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/Small-Chamber-R.wav")
    else:
        (convoll,convolr)=sf.ReadFile("temp/impulses/Vocal-Chamber-L.wav")
        (convorl,convorr)=sf.ReadFile("temp/impulses/Vocal-Chamber-R.wav")

    if spring:
        spring = None
        if small:
            spring=sf.ReadFile("temp/impulses/short-spring.wav")[0]
        else:
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

    def mixIR(filePath, vol):
        if not vol:
            return convoll, convorr
        ml, mr=sf.ReadFile(filePath)
        retl = sf.Finalise(
            sf.Mix(
                convoll,
                sf.LinearVolume(ml, vol)
            )
        )

        retr = sf.Finalise(
            sf.Mix(
                convorr,
                sf.LinearVolume(mr, vol)
            )
        )
        return retl, retr

    convoll, convorr = mixIR("temp/impulses/terrys.wav", terrys)
    convoll, convorr = mixIR("temp/impulses/mega-thederal.wav", megaThe)
    convoll, convorr = mixIR("temp/impulses/perth_city_hall_balcony_ir_edit.wav", perth)
    convoll, convorr = mixIR("temp/impulses/womans_club.wav", club)
    convoll, convorr = mixIR("temp/impulses/ultra-church.wav", ultraChurch)

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

        with SFMemoryZone():
            wleft  =sf.FixSize(sf.Mix(sf.LinearVolume(left,mix ),sf.LinearVolume(wleft,1.0-mix))).keep()
            wright =sf.FixSize(sf.Mix(sf.LinearVolume(right,mix),sf.LinearVolume(wright,1.0-mix))).keep()

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
                    vwleft =sf.FixSize(sf.Mix(ll,rl)).keep()
                    vwright=sf.FixSize(sf.Mix(rr,lr)).keep()

                with SFMemoryZone():
                    wleft =sf.FixSize(sf.Mix(wleft ,sf.Pcnt20(vwleft ))).keep()
                    wright=sf.FixSize(sf.Mix(wright,sf.Pcnt20(vwright))).keep()

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
                    sf.LinearVolume(
                        sf.FixSize(sf.Invert(ctr)),
                        fact
                    )
                )
                hfq=sf.Multiply(hfq,ctr)
                return sf.Mix(hfq,sf.BesselLowPass(sig,freq,4)).keep()

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
                low=sf.LinearVolume(low,m1/m2)
                sig=sf.BesselHighPass(sig,256,4)
                sig=sf.Mix(low,sig)
                sig=highDamp(sig,5000,0.66)
                return sf.FixSize(sf.Clean(sig)).keep()

        left  = filter(left)
        right = filter(right)
    else:
        left = wleft
        right = wright

    with SFMemoryZone():
        if doDeclick:
            left  = declick(left,  thresh = declickThresh, cutoff = cutoff)
            right = declick(right, thresh = declickThresh, cutoff = cutoff)
    
        if lrBalance:
            print 'Correcting magnitudes to:', lrBalance
            left  = sf.LinearVolume(left,  lrBalance[0])
            right = sf.LinearVolume(right, lrBalance[1])
    
        if dbsPerTrack:
            amount = dbsPerTrack * path
            print 'Scaling magnitudes to:', lrBalance
            left  = sf.ExponentialVolume(left,  amount)
            right = sf.ExponentialVolume(right, amount)

        left = left.keep()
        right = right.keep()


    writeWave(right, left, SFConstants.STORE_DIRECTORY + 'reverberated_{0}'.format(path))

