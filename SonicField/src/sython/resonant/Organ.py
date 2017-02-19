#########
# Setup #
#########

import os
import sython.utils.Midi as Midi
from sython.resonant.ResonantPipes import *

def main():  

    ####################################
    # Controls for rendering the piece #
    ####################################
    
    midis=Midi.read_midi_file("temp/bwv806b.mid")
    
    # Length of full piece
    #======================
    length = 4
    
    # Temperament
    #=============
    temperament = Midi.werckmeisterIII
    #temperament = Midi.just_intonation
    #temperament = Midi.bach_lehman
    #temperament = Midi.equal_temperament
    
    # Modulation
    # ==========
    useModWheel = False
    
    # Unify
    #======
    unify = False
    
    # Which track to render.
    # Note this has no effect under unify.
    # ====================================
    track = 2
    
    # Dilate.
    # =======
    dilate = False
    
    # Truncate.
    # Only render this many notes; < 1 implies all.
    truncate = -1
    
    # Do Not Change
    #===============
    
    midis = Midi.repare_overlap(midis)
    #midis = Midi.tempo(midis)
    
    out=[]
    if unify:
        # Make it all one voice if necessary.
        for midi in midis:
            out += midi
        out = sorted(out, key=lambda event: event.tick)
    else:
        track = 2
        out = midis[track]
    
    # Dilate notes to inspect individually.
    if dilate:
        for event in out:
            diff = event.tick_off-event.tick
            event.tick *= 3
            event.tick_off = event.tick+diff

    # Purify, i.e. strip out events which are not notes.
    o2 = []
    for o in out:
        if o.isNote():
            o2.append(o)
    out = o2
    
    # Extract the required beat to get the desired length.
    beat = Midi.set_length([out],length)

    # Scatter, i.e. add some timing jitter to the notes.
    # This helps prevent the music sounding quite so mechanical.
    # High levels of jitter can produce interesting rubato effects; this
    # for this settings of around 128ms seem to make sense.
    out = Midi.scatter(out, beat, 32)
    
    # Truncate, use this when working on tuning sounds so only the first few notes are
    # generated.
    if truncate > 0:
        out = out[0 : truncate]
    
    # This renders the music.
    left,right = [sf.Finalise(sig) for sig in soft_harpsichord(out, beat, temperament, 1.0)]
    
    # Creates controller envelopes based on a particular modulation source in the midi.
    modEnvl = None
    modEnvR = None
    if useModWheel:
        with SFMemoryZone():
            modEnvl = Midi.controllerEnvelope(out,'modwheel', beat, left)
            modEnvr = Midi.controllerEnvelope(out,'modwheel', beat, right)
            # Remove transients from signal use bessel filter
            # to retain the phase of the modulation.
            modEnvl = sf.BesselLowPass(modEnvl, 16, 2).flush()
            modEnvr = sf.BesselLowPass(modEnvr, 16, 2).flush()
    
    @sf_parallel
    def doVib(env, signal):
        '''
        Adds vibrato to a signal based on the passed in envelope.
        
        :param: env, the modulation envolope between 0 (no modulaltio) and 1 (full modulation).
        :type: env, SFSignal.
        
        :param: signal, the signal to modulate.
        :type: signal, SFSignal.
        
        :return: an SFSignal of the modulate version of the passed in signal. 
        
        Note that the returned signal will be flushed.
        '''
        with SFMemoryZone():    
            l=sf.Length(signal)
            trem=sf.FixSize(
                sf.MakeTriangle(
                    sf.FixSize(
                        sf.Mix(
                            sf.SineWave(l, 4.1),
                            sf.SineWave(l, 3.9)
                        )
                    )
                )
            )
            trem=sf.Multiply(+env,trem)
            vib=+trem
            trem=sf.DirectMix(1, sf.Pcnt50(trem))
            signal=sf.Multiply(trem, signal)
            vib=sf.DirectMix(1, sf.NumericVolume(vib, 0.01))
            signal=sf.Resample(vib,signal)
        return sf.Finalise(signal).flush()
    
    if useModWheel:
        left  = doVib(modEnvl, left)
        right = doVib(modEnvr, right)
    
    sf.WriteSignal(left, "temp/left_v{0}_acc".format(track))
    sf.WriteSignal(right,"temp/right_v{0}_acc".format(track))
    sf.WriteFile32((left, right),"temp/temp_v{0}_acc.wav".format(track))
