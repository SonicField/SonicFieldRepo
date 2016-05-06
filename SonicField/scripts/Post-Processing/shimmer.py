import math
from Reverberation import convolve

if not 'random' in dir():
    import random
    random.seed(System.currentTimeMillis())

def bandRand(min,max):
    min=float(min)
    max=float(max)
    r1=random.random()
    r2=random.random()
    r=float(r1+r2)*0.5
    r=r*(max-min)
    r=r+min
    return r

@sf_parallel
def chorus(
    left,
    right,
    minDepth = 10.0,
    maxDepth = 50.0,
    maxRate  =  0.1,
    minRate  =  0.05,
    nChorus  =  4.0,
    minVol   =  0.7,
    maxVol   =  1.0):
    def inner(signal_):
        def inner_():
            signal=sf.Clean(signal_)
            sigs=[]
            l=sf.Length(+signal)
            for inst in range(0,int(nChorus)):
                @sf_parallel
                def in_inner():
                    print "Do"
                    lfo=sf.PhasedSineWave(l,bandRand(minRate,maxRate),random.random())
                    lfo=sf.NumericVolume(lfo,bandRand(minDepth,maxDepth))
                    nsg=sf.TimeShift(+signal,lfo)
                    lfo=sf.PhasedSineWave(l,bandRand(minRate,maxRate),random.random())
                    lfo=sf.NumericVolume(lfo,bandRand(minVol,maxVol))
                    lfo=sf.DirectMix(1,lfo)
                    nsg=sf.Multiply(lfo,nsg)
                    print "Done"
                    return sf.Finalise(nsg)
                sigs.append(in_inner())
            ret=sf.Finalise(sf.Mix(sigs))
            -signal
            return ret
        return inner_()
    
    return inner(left),inner(right)
    
#left,right= sf.ReadFile("temp/test.wav")
left  = sf.ReadSignal("temp/left_v")
right = sf.ReadSignal("temp/right_v")
left  = sf.Cut(0,60000,left)
right = sf.Cut(0,60000,right)
@sf_parallel
def spatialise(osg):
    osi = sf.Invert(+osg)
    dhz = 0.5
    dly = 250.0
    md1 = sf.PhasedSineWave(sf.Length(+osg),0.01,0.0)
    md1 = sf.NumericVolume(+md1,100)
    os1 = sf.AnalogueChorus(+osg,dly,md1,1.25,0.7,18000.0)
    md1 = sf.PhasedSineWave(sf.Length(+osg),0.01,0.5)
    md1 = sf.NumericVolume(+md1,100)
    os2 = sf.AnalogueChorus( osg,dly,md1,1.25,0.7,18000.0)
    oso = [sf.Mix([os1[0],os2[0]]),sf.Mix([os1[1],os2[1]])]
    oso = [ sf.FixSize(sf.Mix(sf.FixSize(s),+osi)) for s in oso ]
    -osi
    return oso
    
def doWork(left,right):
    left1,right1 = spatialise(left)
    left2,right2 = spatialise(right)

    left=sf.MixAt(
        (left1,0),
        (sf.Pcnt10(left2),50)
    )

    right=sf.MixAt(
        (right1,0),
        (sf.Pcnt10(right2),40)
    )

    left,right=chorus(
        left,
        right,
        minDepth = 02.0,
        maxDepth = 10.0,
        minVol   =  0.7,
        maxVol   =  1.0,
        nChorus  = 16.0
    )
    return left,right

lefts  = sf.Granulate(left ,60*5000,0)
rights = sf.Granulate(right,60*5000,0)
outl = []
outr = []
for (left,atl),(right,alr) in zip(lefts,rights):
    left,right=doWork(left,right)
    outl.append((left ,atl))
    outr.append((right,atl))

@sf_parallel
def ma(l):
    return sf.Finalise(sf.MixAt(l))
left  = ma(outl)
right = ma(outr)

sf.WriteSignal(+left, "temp/c_left")
sf.WriteSignal(+right,"temp/c_right")
sf.WriteFile32((left,right),"temp/c.wav")
