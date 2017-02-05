from sython.concurrent import sf_parallel
import sys
        
def writeWave(voxl, voxr, name):
    print 'Doing split', voxl, voxr
    t = 0
    l = min(sf.Length(voxl),sf.Length(voxr))
    c = 0
    while t<l: 
        e = t + 2400000.0
        e = min(l, e)
        # TODO is left and right are different lengths
        # will this fail?
        leftX  = sf.Cut(t, e, voxl)
        rightX = sf.Cut(t, e, voxr)
        sf.WriteFile32((leftX,rightX), "{0}_{1}.wav".format(name, c))
        c += 1
        t = e

    print 'Done split'

def main():
    writeWave(sf.ReadSignal('temp/c_left'), sf.ReadSignal('temp/c_right'), 'temp/final')