#########
# Setup #
#########

import os
import copy
import sython.utils.Midi as Midi
from com.nerdscentral.audio.core import SFMemoryZone, SFConstants
from sython.resonant.ResonantPipes import *
from com.nerdscentral.audio.core import SFData
from sython.organ.Player import RESTART_INFO
from sython.utils.Memory import writeSawppedCache, readSwappedCache


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
        vib=sf.DirectMix(1, sf.LinearVolume(vib, 0.01))
        signal=sf.Resample(vib,signal)
    return sf.FixSize(sf.Finalise(signal)).keep()

def main():

    ####################################
    # Controls for rendering the piece #
    ####################################

    midis=Midi.read_midi_file(SFConstants.STORE_DIRECTORY + "bwv872-a.mid")

    # Length of full piece
    #======================
    length = 2.0

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
    splitTo = 8

    # When we split, do we aggregate by pitch?
    splitPitch = True

    # How much to mess with timing in milliseconds.
    scatter = 32

    # Here to add the second voice. Values less than this will have it;
    # typicyally 0.5 means the bott0m half.
    lowerRank = 0.5

    # Do Not Change
    #===============

    midis = Midi.repare_overlap(midis, blip=2.0 if fastBlip else 5.0)
    #midis = Midi.tempo(midis)

    if unify:
        tracks = [1]

    for track in tracks:
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

            # Distribute the notes such that each split rendering is a different pitch range
            if splitPitch:
                oLen = len(outRaw)
                tmpOut = [[] for _ in xrange(splitTo)]
                cutAt = int(oLen / splitTo)
                idx = 0
                for event in sorted(outRaw, key=lambda event: event.key):
                    if len(tmpOut[idx]) >= cutAt:
                        idx += 1
                        if idx == splitTo:
                            idx -= 1
                            print 'IDX Overflow at:', len(tmpOut[idx])
                    tmpOut[idx].append(event)

                for idx in xrange(splitTo):
                    tmpOut[idx] = sorted(tmpOut[idx], key=lambda event: event.tick)

                # TODO: Make this less hacky.
                outRaw = []
                for idx in xrange(oLen):
                    row = tmpOut[idx % splitTo]
                    rowIdx = int(idx / splitTo)
                    if len(row) > rowIdx:
                        outRaw.append(row[rowIdx])

            @sf_parallel
            def inner(split):
                RESTART_INFO.restartIndex = (track * 10  + split) * 1000
                rank = float(split) / float(splitTo)
                place = -1
                if splitPitch:
                    place = 0.1 + (float(split + 0.5) / float(splitTo)) * 0.8
                count = splitTo
                out = []
                for nt in outRaw:
                    if count % splitTo == split:
                        out.append(nt)
                    count += 1

                voiceA = double_pure_harpsichord
                voiceB = distant_flute_pipe
                # This renders the music.
                signals = []
                
                with SFMemoryZone():
                    left,right = (sf.Finalise(sig) for sig in voiceA(out, beat, temperament, 1.0, place))
                    signals.append((writeSawppedCache(left), writeSawppedCache(right)))
                
                outL = []
                outH = []
                for event in out:
                    eventH = copy.copy(event)
                    eventL = copy.copy(event)
                    eventH.key += 12
                    eventL.key -= 12
                    outH.append(eventH)
                    outL.append(eventL)

                with SFMemoryZone():
                    left, right = (sf.LinearVolume(sf.Finalise(sig), 0.1 * rank) for sig in voiceB(outH, beat, temperament, 1.0, place))
                    signals.append((writeSawppedCache(left), writeSawppedCache(right)))

                with SFMemoryZone():
                    left, right = [sf.LinearVolume(sf.Finalise(sig), 0.1 * (1.0 - rank)) for sig in voiceB(outL, beat, temperament, 1.0, place)]
                    signals.append((writeSawppedCache(left), writeSawppedCache(right)))
                
                with SFMemoryZone():
                    left  = sf.FixSize(sf.Mix([readSwappedCache(s[0]) for s in  signals]))
                    right = sf.FixSize(sf.Mix([readSwappedCache(s[1]) for s in  signals]))
                    if splitPitch:
                        left  = sf.LinearVolume(left, place)
                        right = sf.LinearVolume(right, 1.0 - place)
                    left  = left.keep()
                    right = right.keep()

                # Creates controller envelopes based on a particular modulation source in the midi.

                if useModWheel:
                    modEnvl = None
                    modEnvR = None
                    with SFMemoryZone():
                        modEnvl = Midi.controllerEnvelope(out,'modwheel', beat, left)
                        modEnvr = Midi.controllerEnvelope(out,'modwheel', beat, right)
                        # Remove transients from signal use bessel filter
                        # to retain the phase of the modulation.
                        modEnvl = sf.BesselLowPass(modEnvl, 16, 2)
                        modEnvr = sf.BesselLowPass(modEnvr, 16, 2)

                    left  = _doVib(modEnvl, left)
                    right = _doVib(modEnvr, right)


                sf.WriteSignal(left, SFConstants.STORE_DIRECTORY + "left_v{0}_{1}_acc".format(track, split))
                sf.WriteSignal(right,SFConstants.STORE_DIRECTORY + "right_v{0}_{1}_acc".format(track, split))
                sf.WriteFile32((right, left),SFConstants.STORE_DIRECTORY + "temp_v{0}_{1}_acc.wav".format(track, split))
                return True
            rets = [inner(split) for split in xrange(0, splitTo)]
            #rets = [inner(split) for split in xrange(0, 1)]
            all(ret.get() for ret in rets)
            
            