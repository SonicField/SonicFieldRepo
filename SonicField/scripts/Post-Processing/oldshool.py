from Reverberation import convolve,reverberate

@sf_parallel
def excite(sig_,mix,power,sat,satMix):
    sig=sig_
    m=sf.Magnitude(+sig)
    sigh=sf.BesselHighPass(+sig,500,2)
    mh=sf.Magnitude(+sigh)
    sigh=sf.Power(sigh,power)
    sigh=sf.Clean(sigh)
    sigh=sf.BesselHighPass(sigh,1000,2)
    nh=sf.Magnitude(+sigh)
    sigh=sf.NumericVolume(sigh,mh/nh)
    sig=sf.Mix(sf.NumericVolume(sigh,mix),sf.NumericVolume(sig,1.0-mix))
    if sat:
        n=sf.Magnitude(+sig)
	sig=sf.Realise(sf.NumericVolume(sig,sat*m/n))
	sigst=sf.Saturate(+sig)
	sig=sf.Mix(sf.NumericVolume(sigst,satMix),sf.NumericVolume(sig,1.0-satMix))
    n=sf.Magnitude(+sig)
    return sf.Realise(sf.NumericVolume(sig,m/n))

@sf_parallel
def highDamp(sig,freq,fact):
    mag=sf.Magnitude(+sig)
    hfq=sf.RBJHighPass(+sig,freq,1)
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
    sig=sf.Mix(hfq,sf.RBJLowPass(sig,freq,1))
    mag=mag/sf.Magnitude(+sig)
    return sf.NumericVolume(sig,mag)

@sf_parallel
def bandDamp(sig,freqLow,freqHigh,fact):
    mag=sf.Magnitude(+sig)
    hfq=sf.RBJBandPass(+sig,freqLow,freqHigh)
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
    sig=sf.Mix(hfq,sf.RBJNotch(sig,freqLow,freqHigh))
    mag=mag/sf.Magnitude(+sig)
    return sf.NumericVolume(sig,mag)

@sf_parallel
def lowBoost(sig_):
    sig=sig_
    q=0.5
    sig=sf.Mix(
        sf.Pcnt10(sf.FixSize(sf.WaveShaper(-0.03*q,0.2*q,0,-1.0*q,0.2*q,2.0*q,+sig))),
        sig
    )
    sig=sf.RBJPeaking(sig,64,2,2)
    damp=sf.BesselLowPass(+sig,2000,1)
    sig=sf.FixSize(sf.Mix(damp,sig))
    low=sf.BesselLowPass(+sig,256,4)
    m1=sf.Magnitude(+low)
    low=sf.FixSize(low)
    low=sf.Saturate(low)
    m2=sf.Magnitude(+low)
    low=sf.NumericVolume(low,m1/m2)
    sig=sf.BesselHighPass(sig,256,4)
    sig=sf.Mix(low,sig)
    sig=highDamp(sig,5000,0.66)
    return sf.FixSize(sf.Clean(sig))

(left,right)=sf.ReadFile("temp/a.wav")
left,right = (lowBoost(left),lowBoost(right))
left,right = (sf.Finalise(left),sf.Finalise(right))
left =excite(left ,0.5,1.05,1.5,1.0)
right=excite(right,0.5,1.05,1.5,1.0)
#left,right = (highDamp(left,8000,0.5),highDamp(right,8000,0.5))
con=sf.ReadFile("temp/RCA_44BX_2.wav")[0]
left =reverberate(left, +con)
right=reverberate(right,+con)
left  = sf.Finalise(left)
right = sf.Finalise(right)
sf.WriteFile32((left,right),"temp/c.wav")
