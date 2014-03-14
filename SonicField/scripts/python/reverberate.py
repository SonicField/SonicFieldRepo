def reverberate(signal,convol):
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
