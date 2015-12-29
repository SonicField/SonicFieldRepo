from random import ranomd
def simple_sawtooth(length,pitch):
    signals=[]
    v=1
    opitch=pitch
    it=1.0
    phase=random()
    while pitch<18000:
        signals.append(sf.NumericVolume(sf.PhasedSineWave(length,pitch,phase),1.0/it))
        pitch+=opitch
        it+=1
    return sf.Finalise(sf.Mix(signals))
    
def phasing_sawtooth(length,pitch):
    signals=[]
    v=1
    opitch=pitch
    it=1.0
    while pitch<18000:
        phase=random()
        signals.append(sf.NumericVolume(sf.PhasedSineWave(length,pitch,phase),1.0/it))
        pitch+=opitch
        it+=1
    return sf.Finalise(sf.Mix(signals))