#########
# Setup #
#########

import os
import Midi
import organ.Player as Player
from organ.Post   import \
    do_final_mix, \
    mix, \
    post_process, \
    post_process_tremolate
from organ.ResonantVoices import \
    make_addtive_resonance, \
    oboe_filter, \
    tremulus_oboe_filter, \
    violin_filter


def distant(midi_in,beat,temperament,velocity):
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=distant_wind,
        bend=True,
        mellow=False,
        velocity_correct=velocity*1.5,
        flat_env=True,
        quick_factor=1.5,
        pan = -1
    )
    return post_process(notes1)

def distant_bass(midi_in,beat,temperament,velocity):
    l = 512
    plr = make_addtive_resonance(qCorrect=1.0, rollOff=2.0, saturate=0.5, power=1.1)
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=plr,
        bend=False,
        mellow=False,
        velocity_correct=velocity,
        flat_env=True,
        quick_factor=1.5,
        pure=True,
        pan = -1
    )
    return post_process(notes1)

def string(midi_in,beat,temperament,velocity):
    l = 512
    plr = make_addtive_resonance(qCorrect=2.0, rollOff=1.5, power=1.0, post=violin_filter)
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=plr,
        bend=True,
        mellow=False,
        velocity_correct=velocity*1.5,
        flat_env=True,
        quick_factor=1.0,
        pan = -1
    )
    return post_process(notes1)

def oboe_lead(midi_in,beat,temperament,velocity):
    plr = make_addtive_resonance(qCorrect=2.0, rollOff=1.5, saturate=0.1, power=1.4, post=oboe_filter)
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=plr,
        bend=False,
        mellow=False,
        velocity_correct=velocity*1.5,
        flat_env=True,
        quick_factor=0.5,
        pure=True,
        pan = -1
    )
    return post_process_tremolate(notes1)

def oboe_soft_lead(midi_in,beat,temperament,velocity):
    plr = make_addtive_resonance(qCorrect=2.0, rollOff=1.0, saturate=0.1, power=1.4,
                                 post=tremulus_oboe_filter, limit = True, flat = True, seed = -60)
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=plr,
        bend=False,
        mellow=False,
        velocity_correct=velocity*1.5,
        flat_env=True,
        quick_factor=0.5,
        pure=True,
        pan = -1
    )
    return post_process(notes1)

def oboe_second(midi_in,beat,temperament,velocity):
    midi=Midi.legato(midi_in,beat,200)
    plr = make_addtive_resonance(qCorrect=1.5, rollOff=1.5, saturate=0.1, power=1.4, post=oboe_filter)
    notes1=Player.play(
        midi,
        beat,
        temperament,
        voice=plr,
        bend=False,
        mellow=False,
        velocity_correct=velocity*1.5,
        flat_env=True,
        quick_factor=1.0,
        pure=True,
        pan = -1
    )
    return post_process(notes1)

    
Notes  = []
########################################
# Timing configuration and temperament #
########################################

#midis=read_midi_file("temp/pontchar.mid")
midis=Midi.read_midi_file("temp/lead.mid")

# Length of full piece
#======================
length = 1.5

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

out = midis[1]

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
#out = out[0:5]

left,right = [sf.Finalise(sig) for sig in oboe_soft_lead(out,beat,temperament,1.0)]

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

sf.WriteSignal(+left, "temp/left_t1_acc")
sf.WriteSignal(+right,"temp/right_t1_acc")
sf.WriteFile32((left,right),"temp/temp_t1_acc.wav")
