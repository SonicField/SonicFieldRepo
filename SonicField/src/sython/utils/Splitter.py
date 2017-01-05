from sython.concurrent import sf_parallel
import sys
        
def main():
    voxl, voxr = sf.ReadSignal('temp/draken_left'), sf.ReadSignal('temp/draken_right')
    print 'Doing work', voxl, voxr
    t = 0
    l = max(sf.Length(voxl),sf.Length(voxr))
    c = 0
    while t<l: 
        e = t + 1800000.0
        e = min(l, e)
        # TODO is left and right are different lengths
        # will this fail?
        leftX  = sf.Cut(t, e, voxl)
        rightX = sf.Cut(t, e, voxr)
        sf.WriteFile32((leftX,rightX), "temp/draken_{0}.wav".format(c))
        c += 1
        t = e

    print 'Done work'