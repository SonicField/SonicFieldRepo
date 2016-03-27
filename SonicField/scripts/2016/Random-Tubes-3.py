#########
# Setup #
#########

import os

from Filters import byquad_filter
from organ.Generators import clean_noise
from Parallel_Helpers import mix,realise
from organ.Algorithms import do_formant,excite,create_vibrato
from organ.Voices import celest_flute as voice2
from organ.Voices import folk_flute as voice3
import random
import math

def makeNote():
    pass

def randWalk(value,size,uBound):
    value  = float(value)
    size   = float(size)
    uBound = float(uBound)
    r=(random.random()+random.random())/2.0
    r=math.floor(r*size)-math.floor((size/2.0))    
    value+=r
    if value<1:
        value=2
    elif value>uBound:
        value=uBound-2
    return value

def randWalk3(value,uBound):
    return randWalk(value,3,uBound)

@sf_parallel
def voice1(length,freq):

    sigs=[]
    for i in range(1,3):
        sig=byquad_filter(
            'peak',
            byquad_filter(
                'peak',
                sf.Pcnt1(sf.SineWave(length,freq-2.0+random.random()*4.0)),
                1.0,
                64
            ),
            freq,
            0.1,
            64
        )
        
        sig=byquad_filter(
            'peak',
            sig,
            freq,
            1.0,
            128
        )
    
        sig=sf.FixSize(excite(sig,1.0,2.0))
        sig=sf.FixSize(sf.Saturate(sf.NumericVolume(sig,2.0)))
        
        sig=create_vibrato(
            sig,length,
            longer_than=0.5,
            rate=2 + random.random(),
            at=0.45,
            depth=0.1,
            pitch_depth=0.01
        )     
        
        sigs.append(sig)
    sig=mix(sigs)
    return sf.Finalise(sig)
    #return sf.FixSize(polish(sig,freq))


@sf_parallel
def generate(
        voice,
        pPitch,
        pitch,
        a,
        d,
        s,
        r,
        v
    ):
    base=voice(r,pitch)
    env = sf.NumericShape(
        (0,0),
        (a,1),
        (d,0.4*random.random()+0.1),
        (s,0.1*random.random()+0.05),
        (r,0)
    )
    sig = sf.Multiply(base,env)
    
    # must be longer than the max delay
    sig=sf.Concatenate(sig,sf.Silence(200))
    
    dhz = 8.0 + random.random()
    dly = 1000.0/float(dhz);
    mod = sf.SineWave(sf.Length(+sig),1.0+random.random()*0.5)
    sig = sf.AnalogueChorus(sig,dly,mod,0.80,1.0)
    sgs = [ sf.FixSize(s) for s in sig ]

    return [ sf.NumericVolume(sf.FixSize(sig),v) for sig in sgs ]


def run(
        voice,
        pitch,
        beat,
        minutes,
        maxDenom=8,
        maxNume=8,
        octaves=2,
        verbose=False,
        vOver=1.0
    ):
    print voice, pitch, beat, octaves, vOver
    notesL=[]
    notesR=[]
    oPitch=float(pitch)
    pitchScaleDenom = 3.0
    pitchScaleNume  = 3.0
    lengthScale     = 4.0
    volumeScale     = 4.0
    oVolume         = 4.0
    octaves        /= 2.0
    at=beat*2.0
    pPitch=float(pitch)

    while at/60000 < minutes:
        pitchScale = pitchScaleNume/pitchScaleDenom
        rvs        = 1.0/volumeScale
        volume     = rvs*oVolume
        pitch      = pitchScale*oPitch
        length     = lengthScale*beat


        # Create a consistent envelope
        rlen       = length * 0.75 + random.random() * length
        a          = rlen * (0.10 + random.random() * 0.1) 
        d          = rlen * (0.30 + random.random() * 0.1)
        s          = rlen * (0.70 + random.random() * 0.1)
        r          = rlen
               
        velocity_correction = 1/pitchScale
        
        # Do not over correct very & v high low frequencies 
        # or very quiet notes. This aim it to stop loud highs
        # dominating (psycho-acoustics)
        if rvs<0.2:
            if velocity_correction<1:
                velocity_correction=1
        
        if velocity_correction>4:
            velocity_correction=4
                                  
            if verbose:
                print
                at,
                "PitchNume: ",  pitchScaleNume,
                "PitchDenom: ", pitchScaleDenom,
                "Volume: ",     volumeScale,
                "Pitch: ",      pitch,
                "Length: ",     length,
                "Env:",         (a,d,s,r),
                "Rvs: ",        rvs,
                "VCorr: ",      velocity_correction

        signals = generate(
            voice,
            pPitch,
            pitch,
            a,
            d,
            s,
            r,
            volume * velocity_correction
        )

        lr=random.random()
        rl=1.0-lr
    
        notesL.append((signals,lr,at+30*rl))
        notesR.append((signals,rl,at+30*lr))
        
        at+=length

        # Get a new pitch but within the allowed octave
        # range
        count = 1
        ratio = pitchScaleNume/pitchScaleDenom
        while True:
            pitchScaleDenom = randWalk3(pitchScaleDenom,maxDenom)
            pitchScaleNume  = randWalk3(pitchScaleNume,maxNume)
            t = pitchScaleNume/pitchScaleDenom
            if t == ratio:
                continue
            t = abs(math.log(t,2))
            if t <= octaves:
                break
            count += 1
            if count > 100:
                print 'Waringing, over iteration getting pitch'

        lengthScale = randWalk3(lengthScale,8)
        volumeScale = randWalk3(volumeScale,8)
        pPitch      = pitch

    @sf_parallel
    def post(notes,i):
        r = []
        for s,v,a in notes:
            r += [(sf.NumericVolume(s.get()[i],v),a)]
        return r
    
    return (
        sf.Finalise(sf.NumericVolume(sf.MixAt(post(notesL,0)),vOver)),
        sf.Finalise(sf.NumericVolume(sf.MixAt(post(notesR,1)),vOver))
    )

random.seed()
mins = 60
voce = (voice3,voice3,voice2,voice2,voice2)
scnt = len(voce)
sped = 2048
sigs = [
    run(voce[x],64*pow(2,x),2048*(scnt-x),mins,vOver = 1.0/(x+1.0))
    for x in range(0,scnt)
]

sigs = (
    sf.Finalise(sf.Mix([sig[0]  for sig in sigs])),
    sf.Finalise(sf.Mix([sig[1]  for sig in sigs]))
)
print sigs 
sf.WriteFile32(sigs,"temp/rand.wav")
