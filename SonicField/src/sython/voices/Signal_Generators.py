from random import random
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
    
def simple_triangle(length,pitch):
    signals=[]
    v=1
    opitch=pitch
    it=1.0
    inv=1.0
    phase=random()
    while pitch<18000:
        signals.append(sf.NumericVolume(sf.PhasedSineWave(length,pitch,phase),1.0*inv/(it*it)))
        pitch+=opitch*0.2
        inv*=-1.0
        it+=2.0
    return sf.Finalise(sf.Mix(signals))
    
def phasing_triangle(length,pitch):
    signals=[]
    v=1
    opitch=pitch
    it=1.0
    inv=1.0
    while pitch<18000:
        phase=random()
        signals.append(sf.NumericVolume(sf.PhasedSineWave(length,pitch,phase),inv/(it*it)))
        pitch+=opitch*2.0
        inv*=-1.0
        it+=2.0
    return sf.Finalise(sf.Mix(signals))

def limited_triangle(length,pitch,harmonics):
    pitch=float(pitch)
    limit=float(harmonics)*pitch
    if limit>18000:
        limit=18000
    signals=[]
    v=1
    opitch=pitch
    it=1.0
    inv=1.0
    while pitch<limit:
        signals.append(sf.NumericVolume(sf.PhasedTableSineWave(length,pitch,0.0),inv/(it*it)))
        pitch+=opitch*2.0
        it+=2.0
        inv*=-1.0
    return sf.Finalise(sf.Mix(signals))
