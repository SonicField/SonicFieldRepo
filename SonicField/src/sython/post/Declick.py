from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.utils.Reverberation import reverberate
from sython.utils.Splitter import writeWave
from com.nerdscentral.audio.core import SFData
import new

def main():
    left  = sf.ReadSignal("temp/left_v1_acc")
    right = sf.ReadSignal("temp/right_v1_acc")

    @sf_parallel
    def declick(signal):
        up = SFData.build(signal.getLength())
        down = SFData.build(signal.getLength())
        thresh = 0.02
        old = 0.0
        width = 192 * 2

        # TODO: Convert all this into Java for speed.
        for pos in xrange(signal.getLength()):
            new = signal.getSample(pos)
            diff = old - new
            if abs(diff) > thresh:
                for x in xrange(width):
                    v = 1.0 - (x / width)
                    up.setSample(pos + x, v)
                    v = v + up.getSample(pos -x)
                    if v > 1.0:
                        v = 1.0
                    up.setSample(pos - x, v)
            old = new

        up = sf.FixSize(sf.Mix(
            sf.RBJLowPass(up, 100, 1.0),
            sf.Reverse(sf.RBJLowPass(sf.Reverse(up), 100, 1.0))
        )).realise()

        minV = 0.0
        for pos in xrange(signal.getLength()):
            v = up.getSample(pos)
            if v < minV:
                minV = v
        
        for pos in xrange(signal.getLength()):
            v = up.getSample(pos)
            up.setSample(pos, v - minV)     
        up = sf.FixSize(up)

        for pos in xrange(signal.getLength()):
            down.setSample(pos, 1.0 - up.getSample(pos))

        filt = sf.RBJLowPass(signal, 1000, 1.0)
        filt = sf.Multiply(filt, up)
        nFlt = sf.Multiply(signal, down)
        return sf.FixSize(sf.Mix(filt, nFlt).realise())

    left  = declick(left)
    right = declick(right)
    sf.WriteFile32((left, right),"temp/declicked.wav")

