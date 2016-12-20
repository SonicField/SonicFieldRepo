from sython.concurrent import sf_parallel
from sython.voices.ResonantVoices import make_addtive_resonance
from sython.voices.ResonantVoices import femail_soprano_ah_filter

@sf_parallel
def test_play():
    generator = make_addtive_resonance(qCorrect = 2.0, post = femail_soprano_ah_filter, rollOff = 5.0)
    return (generator(4000, 220), generator(4000, 440))
        
def main():
    print 'Doing work'
    voxs = test_play()
    voxs = [sf.Multiply(
        vox,
        sf.SimpleShape((0, -90), (16, -30), (512, 0), (sf.Length(+vox) * 0.5, -10), (sf.Length(+vox) * 0.9, -60), (sf.Length(+vox), -90))
    ) for vox in voxs]
    voxs = [sf.FixSize(vox) for vox in voxs]
    vox = sf.MixAt((voxs[0], 0), (voxs[1], 5000))
    sf.WriteFile32((+vox, vox), 'temp/temp.wav')

    print 'Done work'