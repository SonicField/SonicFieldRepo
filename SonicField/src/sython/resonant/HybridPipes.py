
def space_flute(midi_in,beat,temperament,velocity):
    notes1=Player.play(
        midi_in,
        beat,
        temperament,
        voice=folk_flute,
        bend=False,
        mellow=False,
        velocity_correct=velocity*0.25,
        flat_env=False,
        quick_factor=2.0,
        pan=0.5
    )
    notes2=Player.play(
        midi_in,
        beat,
        temperament,
        voice=tuned_wind,
        bend=False,
        mellow=True,
        velocity_correct=velocity*0.3,
        flat_env=True,
        quick_factor=0.5,
        pitch_shift=0.25,
        pan=0.8
    )
    notes3=Player.play(
        midi_in,
        beat,
        temperament,
        voice=tuned_wind,
        bend=False,
        mellow=True,
        velocity_correct=velocity*0.3,
        flat_env=True,
        quick_factor=0.5,
        pitch_shift=0.5,
        pan=0.2
    )
    notes4=Player.play(
        midi_in,
        beat,
        temperament,
        voice=upper_accent,
        bend=True,
        mellow=True,
        velocity_correct=velocity*0.05,
        flat_env=True,
        quick_factor=2.0,
        pitch_shift=2.0
    )
    left1,right1 = post_process(notes1)
    left2,right2 = post_process(notes2)
    left3,right3 = post_process(notes3)
    left4,right4 = post_process(notes4)
    return mix(left1,left2,left3,left4),mix(right1,right2,right3,right4)

