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
    
    midis=Midi.read_midi_file("temp/bwv856.mid")
    
    # Length of full piece
    #======================
    length = 3
    
    # Temperament
    #=============
    #temperament = Midi.werckmeisterIII
    #temperament = Midi.just_intonation
    temperament = Midi.bach_lehman
    #temperament = Midi.equal_temperament
    
    # Modulation
    # ==========
    useModWheel = False
    
    # Unify
    #======
    unify = True
    
    # Which tracks to render.
    # Note this has no effect under unify.
    # ====================================
    tracks = [1]
    
    # Dilate.
    # =======
    dilate = False
    
    # Truncate.
    # Only render this many notes; < 1 implies all.
    truncate = 0

    # Fast blip - needed for some piano music.
    fastBlip = False
    
    # Render in multiple tracks using round robin.
    # A setting of 1 gives on track, 2 gives 2 etc.
    splitTo = 4
    
    # How much to mess with timing in milliseconds.
    scatter = 32
    
    # Do Not Change
    #===============
    
    midis = Midi.repare_overlap(midis, blip=2.0 if fastBlip else 5.0)
    #midis = Midi.tempo(midis)
    
    if unify:
        tracks = [1]
    
    for track in tracks:
        # Clear as much memory as possible before each render; remember that
        # there should no no allocations at all in the first pass so there is no
        # harm in calling this for each itereation.
        SFData.flushAll()
        
        # All outputs are written to files which have the track in the name so we can
        # completely discard all data chunks on each iteration.
        with SFMemoryZone():
            outRaw=[]
            if unify:
                # Make it all one voice if necessary.
                for midi in midis:
                    outRaw += midi
                outRaw = sorted(outRaw, key=lambda event: event.tick)
            else:
                outRaw = midis[track]
            
            # Dilate notes to inspect individually.
            if dilate:
                for event in outRaw:
                    diff = event.tick_off-event.tick
                    event.tick *= 10
                    event.tick_off = event.tick+diff
        
            # Purify, i.e. strip out events which are not notes.
            o2 = []
            for o in outRaw:
                if o.isNote():
                    o2.append(o)
            outRaw = o2
            
            # Extract the required beat to get the desired length.
            beat = Midi.set_length([outRaw],length)
        
            # Scatter, i.e. add some timing jitter to the notes.
            # This helps prevent the music sounding quite so mechanical.
            # for this settings of around 128ms seem to make sense.
            if scatter:
                out = Midi.scatter(outRaw, beat, scatter)
            
            # Truncate, use this when working on tuning sounds so only the first few notes are
            # generated.
            if truncate > 0:
                outRaw = outRaw[0 : truncate]

            for split in xrange(0, splitTo):
                count = 1
                out = []
                for nt in outRaw:
                    if count % splitTo == split:
                        out.append(nt)
                    count += 1                
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
                left  = sf.FixSize(left)
                right = sf.FixSize(right)
                sf.WriteSignal(left, "temp/left_v{0}_{1}_acc".format(track, split))
                sf.WriteSignal(right,"temp/right_v{0}_{1}_acc".format(track, split))
                sf.WriteFile32((left, right),"temp/temp_v{0}_{1}_acc.wav".format(track, split))
