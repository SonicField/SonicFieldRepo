#########
# Setup #
#########

import os
import sython.utils.Midi as Midi
from com.nerdscentral.audio.core import SFMemoryZone
from sython.resonant.ResonantPipes import *
from com.nerdscentral.audio.core import SFData

@sf_parallel
def _doVib(env, signal):
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

def main():  

    ####################################
    # Controls for rendering the piece #
    ####################################
    
    midis=Midi.read_midi_file("temp/input.mid")
    
    # Length of full piece
    #======================
    length = 16.0
    
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
    
    # Which tracks to render.
    # Note this has no effect under unify.
    # ====================================
    tracks = [1]
    
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
    
    if unify:
        tracks = [1]
    
    for track in tracks:
        # Clear as much memory as possible before each render; remember that
        # there should no no allocations at all in the first pass so there is no
        # harm in calling this for each itereation.
        SFData.flushAll()
        
        # All outputs are written to files which have the track in the name so we can
        # completely discard all data chunks on each itereation.
        with SFMemoryZone():
            out=[]
            if unify:
                # Make it all one voice if necessary.
                for midi in midis:
                    out += midi
                out = sorted(out, key=lambda event: event.tick)
            else:
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
            
            if useModWheel:
                left  = _doVib(modEnvl, left)
                right = _doVib(modEnvr, right)
            
            sf.WriteSignal(left, "temp/left_v{0}_acc".format(track))
            sf.WriteSignal(right,"temp/right_v{0}_acc".format(track))
            sf.WriteFile32((left, right),"temp/temp_v{0}_acc.wav".format(track))
