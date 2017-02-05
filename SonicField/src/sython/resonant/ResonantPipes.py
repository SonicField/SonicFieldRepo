import sython.organ.Player as Player

from sython.organ.Post   import \
    do_final_mix, \
    mix, \
    post_process, \
    post_process_tremolate

from sython.voices.ResonantVoices import \
    distant_wind, \
    make_addtive_resonance, \
    oboe_filter, \
    violin_filter, \
    harpsichord_filter, \
    tuned_wind

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

def distant_bass_accented(midi_in,beat,temperament,velocity):
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
        pure=False,
        pan = 0.2
    )
    
    plr = make_addtive_resonance(qCorrect=2.0, rollOff=4.0, saturate=0.0, power=1.0)
    notes2=Player.play(
        midi_in,
        beat,
        temperament,
        voice=plr,
        bend=False,
        mellow=False,
        velocity_correct=velocity,
        flat_env=True,
        quick_factor=1.0,
        pure=True,
        pitch_shift=4.0,
        pan = 0.8
    )
    
    left1,right1 = post_process(notes1)
    left2,right2 = post_process(notes2)
    return mix(left1,left2),mix(right1,right2)

def distant_string(midi_in,beat,temperament,velocity):
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

def distant_oboe(midi_in,beat,temperament,velocity):
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

def distant_oboe2(midi_in, beat, temperament, velocity):
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
  
  
def harpsichord(midi_in, beat, temperament, velocity):
    harmonics = [pow(x,1.001) for x in xrange(1,100)]
    plr = make_addtive_resonance(qCorrect=4.0, rollOff=1.0, saturate=0.1, power=1.0, 
                                 post=harpsichord_filter, harmonics=harmonics, seed = -40)
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=plr,
        bend=True,
        mellow=False,
        velocity_correct=velocity*1.5,
        flat_env=True,
        quick_factor=0,
        pure=True,
        pan = -1
    )
    return post_process(notes1)  

    
