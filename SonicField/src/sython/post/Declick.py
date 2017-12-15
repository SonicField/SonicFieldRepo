from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone, SFConstants
from sython.utils.Reverberation import reverberate
from sython.utils.Splitter import writeWave
from com.nerdscentral.audio.core import SFData
import new
import os


def declick(signal, thresh = 0.02, cutoff = 1000):
    with SFMemoryZone():
        events = 0
        signal = sf.FixSize(signal)
        up = signal.replicateEmpty()
        down = signal.replicateEmpty()
        old = 0.0
        oldOld = 0.0
        width = 192 * 1
    
        # TODO: Convert all this into Java for speed.
        for pos in xrange(signal.getLength()):
            new = signal.getSample(pos)
            diff = old - new
            sdiff = (old - oldOld) - diff
            if abs(diff) > thresh or abs(sdiff) > thresh:
                events += 1
                #print 'Click Event, diff: %10.6f  sdiff: %10.6f count: %d sample: %d' % (diff, sdiff, events, pos)
                for x in xrange(width):
                    v = 1.0 - (x / width)
                    up.setSample(pos + x, v)
                    v = v + up.getSample(pos - x)
                    if v > 1.0:
                        v = 1.0
                    up.setSample(pos - x, v)
            oldOld = old
            old = new

        if events == 0:
            print 'No Click Events'
            return signal.keep()
    
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
    
        filt = sf.RBJLowPass(signal, cutoff, 1.0)
        filt = sf.Multiply(filt, up)
        nFlt = sf.Multiply(signal, down)
        print 'Declicked %i events.' % events
        return sf.FixSize(sf.Mix(filt, nFlt)).keep()


def main():
    left  = sf.ReadSignal("temp/left_v1_acc")
    right = sf.ReadSignal("temp/right_v1_acc")

    left  = declick(left)
    right = declick(right)
    sf.WriteFile32((left, right),"temp/declicked.wav")
    sf.WriteSignal(left, "temp/declicked_l")
    sf.WriteSignal(right,"temp/declicked_r")

