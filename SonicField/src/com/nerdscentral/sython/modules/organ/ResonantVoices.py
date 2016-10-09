###############################################################################
#
# Voices
#
###############################################################################

from organ.Generators import *
from Parallel_Helpers import mix
from organ.Algorithms import do_formant,excite,create_vibrato
from Signal_Generators import phasing_sawtooth,simple_sawtooth,phasing_triangle, limited_triangle
from Filters import byquad_filter
from Reverberation import convolve,reverberate
import math
import random
import functools

# Get IRs
violinIRs = sf.ViolinBodyIRs(())
violaIRs  = sf.ViolaBodyIRs(())
celloIRs  = sf.CelloBodyIRs(())
bassIRs   = sf.BassBodyIRs(())

def _distant_wind(length, freq, qCorrect = 1.25, limit = False, seed = -60):
    length += 250.0
    base = sf.Mix(
        clean_noise(length,freq*4.0),
        sf.Volume(sf.SineWave(length,freq), seed)
    )
    env = []
    # Super imposted last two postions does not matter.
    v = 0.5
    t = 0
    while t < length:
        if random.random() > 0.5:
            v *= 1.1
            if v > 1.0:
                v = 0.9
        else:
            v *= 0.9
        env += [(t, v)]
        # Constrained random walk envelope in 100 ms steps.
        t += 100

    #base = sf.Multiply(sf.NumericShape(env), base)
    out = []
    xq = 1.8 if freq > 640 else 2.0 if freq > 256 else 2.5 if freq > 128 else 3.0
    xq *= qCorrect
    for q in (16, 32, 48):
        out += [
            byquad_filter(
                'peak' if not limit else 'limited peak',
                +base,
                freq,
                0.5,
                q * xq
            )
        ]
    -base
    out = sf.Mix(out)
    out = sf.ButterworthLowPass(out, freq*1.25, 4)
    out = sf.ButterworthLowPass(out, freq*1.25, 4)
    st = sf.Cut(0, length/2.0,+out)
    ed = sf.Cut(length/2.0, length,+out)
    st = sf.Magnitude(st)
    ed = sf.Magnitude(ed)
    rt = st/ed
    ev = sf.NumericShape((0.0, 1.0), (length, rt))
    out = sf.Multiply(ev, out)
    return sf.FixSize(sf.Cut(250,length,out))

def distant_wind(length, freq):

    @sf_parallel
    def doWind(lenth, hfreg):
        outF = _distant_wind(length, hfreq)
        outR = _distant_wind(length, hfreq)
        out  = sf.FixSize(sf.Reverse(outR), outF)
        if freq > 256:
            out = sf.Power(out, 1.25)
        else:
            out = sf.Power(out, 1.05)
        return sf.FixSize(polish(out, hfreq))

    sigs = []
    harms = range(1, 15)
    for harm in harms:
        hfreq = harm * freq
        if hfreq > 18000.0:
            break
        sigs += [doWind(length, hfreq)]

    harms.reverse()
    for harm in harms:
        sig = sigs.pop()
        sig = sf.NumericVolume(sig, 1.0 / ( pow(harm, 4.0)))
        sigs.insert(0, sig)

    out = sf.Mix(sigs)
    return sf.FixSize(polish(out, freq))

def additive_resonance(power, qCorrect, saturate, rollOff, post, limit, seed, flat, length, freq):

    @sf_parallel
    def doWind(lenth, hfq):
        out = _distant_wind(length, hfq, qCorrect, limit, seed)
        out = sf.Power(out, power)
        if saturate:
            os = sf.Saturate(sf.NumericVolume(sf.FixSize(+out), 2.0))
            out = sf.Mix(sf.NumericVolume(out, 1.0-saturate), sf.NumericVolume(os, saturate))
        return sf.FixSize(out)
        
    sigs = []
    harms = []
    for harm in range(1, 100):
        hfreq = harm * freq
        if hfreq > 18000.0:
            break
        harms.append(harm)
        if flat:
            sfw = doWind(length, hfreq)
            srv = sf.Reverse(doWind(length, hfreq))
            sigs += [sf.Mix(sfw, srv)]
        else:
            sigs += [doWind(length, hfreq)]

    # Remember that this cannot act as a a closure around a signal!
    @sf_parallel
    def stage2(harms, sigs):
        ret = []
        harms.reverse()
        for harm in harms:
            sig = sigs.pop()
            sig = sf.NumericVolume(sig, 1.0 / (pow(harm, rollOff)))
            ret.append(sig)
        ret.reverse()
        out = sf.Mix(ret)
        return sf.FixSize(polish(out, freq))   

    ret = stage2(harms, sigs)
    return post(ret, length, freq) if post else ret

def make_addtive_resonance(power = 1.1 , qCorrect = 1.25 , saturate = 0.0, rollOff = 4.0,
                           post = None, limit = False, seed = -60, flat = True):
    return functools.partial(additive_resonance, power, qCorrect, saturate, rollOff, post, limit, seed, flat)

def oboe_filter(sig, length, freq):
    sig = sf.RBJPeaking(sig, freq*3, 0.2, 5)
    sig = sf.RBJPeaking(sig, freq*5, 0.5, 4)
    sig = sf.RBJPeaking(sig, freq*7, 1, 4)
    sig = sf.RBJNotch(sig, freq*2, 1.0, 1.0)
    sig = sf.Mix(+sig, sf.RBJNotch(sig, freq, 1.0, 1.0))
    sig = sf.RBJLowPass(sig, freq*9, 1.0)
    br = freq * 9
    if br > 5000.0:
       br = 5000.0
    sig = sf.RBJLowPass(sig, br, 1.0)
    return sf.FixSize(sf.Clean(sig))

def tremulus_oboe_filter(sig, length, freq):
    rate = 3.25
    # Clip off extreme spectra leakage (which in can have big negative compenents).
    env = sf.NumericShape((0, 0), (5, 1), (length-5, 1), (length, 0))
    sig = sf.Multiply(env, sig)
    # Filter gentally to keep the singal stable
    sig = sf.RBJLimitedPeaking(sig, freq*3, 0.2, 2.5)
    sig = sf.RBJLimitedPeaking(sig, freq*3, 0.2, 2.5)
    sig = sf.FixSize(sig)
    sig = sf.RBJLimitedPeaking(sig, freq*5, 0.5, 2)
    sig = sf.RBJLimitedPeaking(sig, freq*5, 0.5, 2)
    sig = sf.FixSize(sig)
    sig = sf.RBJLimitedPeaking(sig, freq*7, 1, 2)
    sig = sf.RBJLimitedPeaking(sig, freq*7, 1, 2)
    sig = sf.FixSize(sig)
    sig = sf.RBJNotch(sig, freq*2, 1.0, 1.0)
    sig = sf.Mix(+sig, sf.RBJNotch(sig, freq, 1.0, 1.0))
    sig = sf.FixSize(sig)
    sig = sf.RBJLowPass(sig, freq*9, 1.0)
    br = freq * 9
    if br > 5000.0:
       br = 5000.0
    sig = sf.RBJLowPass(sig, br, 1.0)
    sig = sf.FixSize(sig)
    shape = sf.SineWave(length, rate)
    shape = sf.NumericVolume(shape, 0.5)
    shape = sf.DirectMix(1.0, shape)
    filt = byquad_filter('high', +sig, freq * 4)
    filt = sf.Multiply(filt, +shape)
    sig = sf.Mix(sig, filt)
    mag = 0.01
    ev=sf.NumericVolume(shape, mag)
    ev=sf.DirectMix(1.0, ev)
    sig=sf.FrequencyModulate(sig, ev)
    return sf.FixSize(sig)

@sf_parallel
def violin_filter(sig, length, freq):
    sigs=[]
    bodies = violinIRs
    for body in bodies:
        sigs.append(convolve(+sig,+body))
    sig = sf.Mix(
        sf.FixSize(sf.Clean(sf.Mix(sigs))),
        sig
    )
    
    if length > 250:
        # TODO feels a bit crushed - more stages?
        vibStart  = length*0.5  if length>600 else vibAbove*0.75
        vibMiddle = length*0.75 if length>600 else vibAbove
        vibAmount = 0.5 if length > 1000 else 0.25
        trueLen = sf.Length(+sig)
        l = trueLen
        env = sf.NumericShape((0, 0), (vibStart, 0), (vibMiddle, 1), (length, 0.75), (l, 0))
        env = sf.NumericVolume(env, vibAmount)
        trem = sf.SineWave(l,2.0 + random.random())
        trem = sf.Multiply(env, trem)
        vib = +trem
        trem = sf.DirectMix(1, sf.Pcnt50(trem))
        sig = sf.Multiply(trem, sig)
        vib = sf.DirectMix(1, sf.NumericVolume(vib, 0.01))
        sig = sf.Resample(vib, sig)

    return sf.FixSize(sf.Clean(sig))

@sf_parallel
def tuned_wind(length,freq):

    sigs=[]
    for i in range(1,3):
        sig=byquad_filter(
            'peak',
            byquad_filter(
                'peak',
                sf.Mix(
                    clean_noise(length,freq),
                    sf.Pcnt10(sf.SineWave(length,freq))
                ),
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
            rate=2.5,
            at=0.45,
            depth=0.5,
            pitch_depth=0.02
        )
        
        sigs.append(sig)
    sig=mix(sigs)
    return sf.FixSize(polish(sig,freq))

