#########
# Setup #
#########

import os
import sython.utils.Midi
import sython.organ.Player as Player
from sython.resonant.OrganPipes import *

def main():  
    Notes  = []
    ########################################
    # Timing configuration and temperament #
    ########################################
    
    #midis=read_midi_file("temp/pontchar.mid")
    midis=Midi.read_midi_file("temp/input.mid")
    
    # Length of full piece
    #======================
    length = 4
    
    # Temperament
    #=============
    temperament = Midi.werckmeisterIII
    #temperament = just_intonation
    #temperament = Midi.bach_lehman
    #temperament = equal_temperament
    
    # Modulation
    # ==========
    useModWheel = False
    
    # Do Not Change
    #===============
    
    midis = Midi.repare_overlap(midis)
    #midis = Midi.tempo(midis)
    
    ###########
    # Voicing #
    ###########
    
    # Make it all one voice
    #out=[]
    #for midi in midis:
    #    out += midi
    #out = sorted(out, key=lambda event: event.tick)
    
    out = midis[2]
    
    # dilate notes to inspect individually
    #for event in out:
    #    diff=event.tick_off-event.tick
    #    event.tick*=3
    #    event.tick_off=event.tick+diff
    
    
    # Purify
    o2 = []
    for o in out:
        if o.isNote():
            o2.append(o)
    out = o2
    beat = Midi.set_length([out],length)
    
    # Truncate
    out = out[0:15]
    
    left,right = [sf.Finalise(sig) for sig in distant_bass(out,beat,temperament,1.0)]
    
    if useModWheel:
        left  = sf.ReadSignal("temp/left")
        right = sf.ReadSignal("temp/right")
        modEnvl = Midi.controllerEnvelope(out,'modwheel',beat,+left)
        modEnvr = Midi.controllerEnvelope(out,'modwheel',beat,+right)
        # remove transients from signal use bessel filter
        # to retain the phase of the modulation
        modEnvl = sf.BesselLowPass(modEnvl,16,2)
        modEnvr = sf.BesselLowPass(modEnvr,16,2)
        sf.WriteFile32((+modEnvl,+modEnvr),"temp/env.wav")
    
    @sf_parallel
    def doVib(env,signal):
        l=sf.Length(+signal)
        trem=sf.FixSize(
            sf.MakeTriangle(
                sf.FixSize(
                    sf.Mix(
                        sf.SineWave(l,4.1),
                        sf.SineWave(l,3.9)
                    )
                )
            )
        )
        trem=sf.Multiply(+env,trem)
        vib=+trem
        trem=sf.DirectMix(1,sf.Pcnt50(trem))
        signal=sf.Multiply(trem,signal)
        vib=sf.DirectMix(1,sf.NumericVolume(vib,0.01))
        signal=sf.Resample(vib,signal)
        return sf.Finalise(signal)
    
    if useModWheel:
        left=doVib(modEnvl,left)
        right=doVib(modEnvr,right)
    
    sf.WriteSignal(+left, "temp/left_v_acc")
    sf.WriteSignal(+right,"temp/right_v_acc")
    sf.WriteFile32((left,right),"temp/temp_v_acc.wav")
