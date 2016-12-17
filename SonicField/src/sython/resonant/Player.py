from sython.concurrent import sf_parallel
from sython.voices.ResonantVoices import make_addtive_resonance
from sython.voices.ResonantVoices import femail_soprano_ah_filter

@sf_parallel
def test_play():
    generator = make_addtive_resonance(post = femail_soprano_ah_filter)
    return generator(10000, 440)
        
def main():
    print 'Doing work'
    vox = test_play()
    sf.WriteFile32((+vox, vox), 'temp/temp.wav')

    print 'Done work'