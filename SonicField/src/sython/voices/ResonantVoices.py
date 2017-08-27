###############################################################################
#
# Voices
#
###############################################################################

from com.nerdscentral.audio.core import SFMemoryZone
from sython.utils.Parallel_Helpers import mix
from sython.utils.Algorithms import do_formant
from sython.utils.Algorithms import excite
from sython.utils.Algorithms import create_vibrato
from sython.utils.Algorithms import polish
from sython.utils.Algorithms import compress, decompress
from sython.voices.Signal_Generators import phasing_sawtooth
from sython.voices.Signal_Generators import simple_sawtooth
from sython.voices.Signal_Generators import phasing_triangle
from sython.voices.Signal_Generators import limited_triangle
from sython.voices.Signal_Generators import clean_noise
from sython.utils.Filters import byquad_filter
from sython.utils.Reverberation import convolve
from sython.utils.Algorithms import polish
import math
import random
import functools
from __builtin__ import None

# The probability of using rather than replacing a cached harmonic.
_CACHE_PROBABILITY = 0.9
# TODO: Turn on caching when the frequency problem if fixed.
_PERFORM_CACHE = True

# Get IRs
violinIRs = sf.ViolinBodyIRs(())
violaIRs  = sf.ViolaBodyIRs(())
celloIRs  = sf.CelloBodyIRs(())
bassIRs   = sf.BassBodyIRs(())

# TODO: Refactor this caching logic to a utility.
def _logRoundUp(freq):
    lfq = math.log(freq) * 100.0
    return math.exp(math.ceil(lfq)* 0.01)

def _repitch(freq, targetFreq, sig, length = None):
    ret = sf.DirectResample(sig, float(targetFreq) / float(freq))
    return sf.Cut(0, length, ret) if length and sf.Length(ret) > length else ret

def _distant_wind(length, freq, qCorrect = 1.25, limit = False, seed = -60):
    with SFMemoryZone():
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
        with SFMemoryZone():
            for q in (16, 32, 48):
                out += [
                    byquad_filter(
                        'peak' if not limit else 'limited peak',
                        base,
                        freq,
                        0.5,
                        q * xq
                    )
                ]
            out = sf.Mix(out)
            out = sf.ButterworthLowPass(out, freq*1.25, 4)
            out = sf.ButterworthLowPass(out, freq*1.25, 4).keep()
            st = sf.Cut(0, length/2.0, out)
            ed = sf.Cut(length/2.0, length, out)
            st = sf.Magnitude(st)
            ed = sf.Magnitude(ed)
        rt = st/ed
        ev = sf.NumericShape((0.0, 1.0), (length, rt))
        out = sf.Multiply(ev, out)
        return sf.FixSize(sf.Cut(250,length,out)).keep()

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

_resonanceCache = {}

def additive_resonance(power, qCorrect, saturate, rollOff, post, limit, seed, flat, harmonics, length, freq):

    lowComp = freq < 128
    if lowComp:
        freq *= 2.0
        length *= 0.5

    @sf_parallel
    def doWind(length, hfq, vol, reverse=False):
        key = (length, _logRoundUp(hfq), vol, reverse)
        if key in _resonanceCache:
            if random.random() < _CACHE_PROBABILITY:
                return _repitch(key[1], hfq, _resonanceCache[key])

        with SFMemoryZone():
            # Put in a little extra and then trim it off reliably.
            upRatio = key[1] * 1.01 / float(hfq)
            out = _distant_wind(length * upRatio, hfq, qCorrect, limit, seed)
            out = sf.Power(out, power)
            if saturate:
                os = sf.Saturate(sf.NumericVolume(sf.FixSize(+out), 2.0))
                out = sf.Mix(sf.NumericVolume(out, 1.0-saturate), sf.NumericVolume(os, saturate))
            out = sf.Realise(out)
            out = sf.NumericVolume(sf.FixSize(out), vol)
            if reverse:
                out = sf.Reverse(out)
            ret = compress(out)
            if _PERFORM_CACHE:
                # Store no more than the length we need which we work out as the 
                # inverse scale of that used to make the signal. This should 
                # always give enough signal so out of bounds does not happen.
                toCache = _repitch(hfq, key[1], ret, sf.Length(ret) / upRatio)
                toCache = toCache.pin()
                # FIXME: Figure out how to make this long enough.
                # See call to _distant_wind above.
                _resonanceCache[key] = toCache
                return toCache
            else:
                return ret.keep()
            return ret

    harms = []
    base = compress(sf.Silence(length))
    sigs = []
    for harm in harmonics:
        hfreq = harm * freq
        if hfreq > 18000.0:
            break
        harms.append(harm)
        vol = 1.0 / (pow(harm, rollOff))
        if flat:
            sfw = doWind(length, hfreq, vol)
            srv = doWind(length, hfreq, vol, reverse=True)
            sigs += [sfw, srv]
        else:
            sigs += [doWind(lenth, hfq, vol)]
        if len(sigs) >  3:
            sigs += [base]
            base = sf.Realise(sf.Mix(sigs))
            sigs = []

    base = decompress(base)
    ret = None
    if lowComp:
        base=sf.DirectRelength(base, 0.5)
        freq *= 0.5
        length *= 2.0
        ret = sf.Realise(sf.FixSize(sf.Clean(base)))
    else:
        ret = sf.Realise(sf.FixSize(polish(base,freq)))
    return post(ret, length, freq) if post else ret

def make_addtive_resonance(power = 1.1 , qCorrect = 1.25 , saturate = 0.0, rollOff = 4.0,
                           post = None, limit = False, seed = -60, flat = True, harmonics = xrange(1,100)):
    return functools.partial(additive_resonance, power, qCorrect, saturate, rollOff, post, limit, seed, flat, harmonics)

def oboe_filter(sig, length, freq):
    # Clip off extreme spectra leakage (which in can have big negative compenents).
    env = sf.NumericShape((0, 0), (5, 1), (length-5, 1), (length, 0))
    sig = sf.Multiply(env, sig)
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

def harpsichord_filter(power, resonance, sig, length, freq):
    with SFMemoryZone():
        ring = sf.SineWave(length, 50.0 + freq / 50.0)
        ring = sf.Multiply(sf.NumericShape((0,0.05), (length,0)), ring)
        ring = sf.DirectMix(1.0, ring)
        sig = sf.Multiply(sig, ring).keep()
        
    with SFMemoryZone():
        end = length - 10
        if end < 50:
            end = 50
        tot = 10000.0 # 10 Seconds
        
        env = sf.NumericShape(
            (0, 18000),
            (length-25, freq * 3.0),
            (length, freq * 6.0)
        )
        res = sf.NumericShape(
            (1, 0.2* resonance),
            (length-25, 0.5 * resonance),
            (length, 0.8 * resonance)
        )
        sig = sf.ShapedLadderLowPass(sig,env,res)
    
        env = sf.SimpleShape((0, 10), (5, -10), (10000, -80))
        env = sf.Cut(0, length, env)
        env = sf.Multiply(
            env,
            sf.NumericShape((0,1), (length - 25,1), (length, 0))
        )
        out = sf.FixSize(sf.Multiply(env, sig))
        click = sf.RBJLowPass(sf.SimpleShape((0,-10),(10,-90),(length, -100)),5000,1)
        out = sf.Mix(click, out)
        out = sf.Power(out, power)
        return sf.FixSize(out.flush())

# TODO remove length from signature.
def soft_harpsichord_filter(power, resonance, sig, length, freq, attack=2, triangle=True):

    length = sf.Length(sig)

    if attack > 20:
        raise ValueError('Attack too large; must be <= 20.')

    with SFMemoryZone():
        ring = sf.SineWave(length, 65 * random.random() * 10)
        if triangle:
            ring = sf.MakeTriangle(ring)
        quant = 0.1
        if freq < 256:
            quant = 0.3
        elif freq < 512:
            quant = 0.2
        ring = sf.Multiply(sf.NumericShape((0, quant), (length,0)), ring)
        ring = sf.DirectMix(1.0, ring)
        sig = sf.Multiply(sig, ring).keep()
    
    with SFMemoryZone():
        sig = sf.Reverse(sig)
        tot = 10000.0 # 10 Seconds
        
        max_len = 10000.0
        tLen = max_len if max_len > length else length
        
        q1 = freq * 7
        q2 = freq * 3
        if freq > 440:
            q2 *= 0.75
            q1 *= 0.75
        if freq > 660:
            q2 *= 0.75
            q1 *= 0.75
    
        env = None
        if length > 60:
            env = sf.SimpleShape(
                (0,       sf.ToDBs(18000)),
                (50,      sf.ToDBs(q1)),
                (max_len, sf.ToDBs(q2)),
                (tLen,    sf.ToDBs(q1))
            )         
        else:
            env = sf.SimpleShape(
                (0,       sf.ToDBs(18000)),
                (max_len, sf.ToDBs(q2)),
                (tLen,    sf.ToDBs(q2))
            )
        env = sf.Cut(0, length, env)
        env = sf.Multiply(
            sf.SimpleShape((0, 0),(length - 10, 0), (length, -20)),
            env
        )

        if length > 50:
            env = sf.Multiply(
                env,
                sf.NumericShape((0,1), (length - 35,1), (length, 0.1))
            )
        else:
            env = sf.Multiply(
                env,
                sf.NumericShape((0,1), (length, 0.1))
            )

        res = sf.NumericShape(
            (0, 0.2* resonance),
            (max_len, 0.5 * resonance),
            (tLen,  0.5 * resonance)
        )
        res = sf.Cut(0, length, res)
        env = sf.Multiply(
            sf.SimpleShape((0, 0),(length - 10, 0), (length, 10)),
            env
        )
        sig, env, res = sf.MatchLengths((sig, env, res))
        out = sf.ShapedLadderLowPass(sig, env, res)

        if power != 1.0:
            outP = sf.FixSize(sf.Power(out, power))
            outP = sf.Saturate(outP)
        
        env = None
        if length > 50:
            env = sf.SimpleShape((0, -40), (attack,0), (50, -10), (max_len, -80), (tLen, -80))
            env = sf.Cut(0, length, env)
            env = sf.Multiply(
                env,
                sf.NumericShape((0, 0), (10, 1), (length - 25,1), (length, 0))
            )
        else:
            env = sf.NumericShape((0, 0), (10, 1), (length, 0))
        out = sf.Multiply(env, out)

        return sf.FixSize(polish(out, freq)).flush()

def oboe_harpsichord_filter(sig, length, frequency, vibAbove=200):
    powr = 1.0
    if frequency< 250:
        powr -= (250.0 - frequency) / 750.0
        if powr < 0.5:
            powr = 0.5

    with SFMemoryZone():
        sig = soft_harpsichord_filter(power=powr, resonance=1.0, sig=sig,
                                   length=length, freq = frequency)
        if length > vibAbove:
            # TODO feels a bit crushed - more stages?
            vibStart  = length*0.5  if length>600 else vibAbove*0.75
            vibMiddle = length*0.75 if length>600 else vibAbove
            vibAmount = 0.5 if length > 1000 else 0.25
            trueLen = sf.Length(sig)
            l = trueLen
            env = sf.NumericShape((0, 0), (vibStart, 0), (vibMiddle, 1), (l, 0))
            env = sf.NumericVolume(env, vibAmount)
            trem = sf.SineWave(l,2.0 + random.random())
            trem = sf.MakeTriangle(trem)
            trem = sf.Multiply(env, trem)
            vib = trem
            trem = sf.DirectMix(1, sf.Pcnt50(trem))
            sig = sf.Multiply(trem, sig)
            vib = sf.DirectMix(1, sf.NumericVolume(vib, 0.01))
            sig = sf.Resample(vib, sig)
        
        return sig.keep()

goldbergSlope = sf.SimpleShape((0,0), (10,0), (1000,-30), (20000,-60))
def goldberg_filter(sig, length, frequency):
    return _goldberg_filter(sig, length, frequency, False)

def goldberg_filter_bright(sig, length, frequency):
    return _goldberg_filter(sig, length, frequency, True)

def _goldberg_filter(sig, length, frequency, bright):
    global goldbergSlope
    with SFMemoryZone():
        sig = oboe_harpsichord_filter(sig, length, frequency, 75)
        wetV = sf.ValueAt(goldbergSlope, frequency)
        dryV = 1.0 - wetV
        wet = sf.FixSize(sf.Saturate(sf.NumericVolume(sig, 2.0)))
        wet = sf.NumericVolume(wet, wetV)
        dry = sf.NumericVolume(sig, dryV)
        slope = (250.0 - frequency) / 250.0
        vol = 1.0 if slope < 0.0 else slope + 1.0
        sig = sf.Mix(wet, dry)
        if bright and length > 20.0:
            sig = sf.RBJPeaking(sig, frequency*4, 1.0, 1)
            reclip = sf.NumericShape((0,1), (length - 10, 1),  (length, 0))
            sig = sf.Multiply(reclip, sig)
        sig = sf.FixSize(sig)
        return sf.NumericVolume(sig, vol).keep()

def make_harpsichord_filter(soft=False, power=1.05, resonance=1.0):
    if soft:
        return functools.partial(soft_harpsichord_filter, power, resonance) 
    else:
        return functools.partial(harpsichord_filter, power, resonance)

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

def tremulus_diapason_filter(sig, length, freq):
    rate = 3.0
    # Clip off extreme spectra leakage (which in can have big negative compenents).
    env = sf.NumericShape((0, 0), (5, 1), (length-5, 1), (length, 0))
    sig = sf.Multiply(env, sig)
    # Filter gentally to keep the singal stable
    sig = sf.RBJLimitedPeaking(sig, freq*2, 0.5, 1.0)
    sig = sf.RBJLowPass(sig, freq*3, 1.0)
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

def tremulus_flute_filter(sig, length, freq):
    rate = 3.0
    # Clip off extreme spectra leakage (which in can have big negative compenents).
    env = sf.NumericShape((0, 0), (5, 1), (length-5, 1), (length, 0))
    sig = sf.Multiply(env, sig)
    shape = sf.SineWave(length, rate)
    shape = sf.NumericVolume(shape, 0.5)
    shape = sf.DirectMix(1.0, shape)
    filt = byquad_filter('high', +sig, freq * 2)
    filt = sf.Multiply(filt, +shape)
    sig = sf.Mix(sig, filt)
    mag = 0.01
    ev=sf.NumericVolume(shape, mag)
    ev=sf.DirectMix(1.0, ev)
    sigf=sf.FrequencyModulate(+sig, ev)
    return sf.FixSize(sf.Mix(sig, sigf))

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
    
    vibAbove = 250
    if length > vibAbove:
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
def _vox_filter(vox, freq, a, b, c):
    length=sf.Length(+vox)
    vox=sf.FixSize(polish(vox,freq)) 
    vox=do_formant(vox, a, b, c, freq)
    vox=polish(vox, freq)        
    vox=excite(vox, 0.2, 2.0)
    vox=polish(vox, freq)
    notch=(freq + a) / 2.0      
    vox=mix(
        sf.Pcnt75(sf.RBJNotch(+vox, notch, 0.5)),
        sf.Pcnt25(vox)
    )
    
    if length>1024:
        vibRate = 3.0
        depth = 0.05
        pDepth = 0.1
    else:
        vibRate = 2.5
        depth = 0.025
        pDepth = 0.05
    if length > 2048:
        at = length*0.75
    else:
        at = length*0.5
    vox=create_vibrato(vox, length, longer_than=512, rate=vibRate, depth=depth, pitch_depth=pDepth, at=at)
    
    vox=polish(vox, freq)
    vox=sf.RBJPeaking(vox, freq, 3, 4)
    vox=polish(vox, freq)
    return sf.FixSize(vox)

@sf_parallel
def femail_soprano_ah_filter(vox, length, freq):
    print length, freq
    vox = _vox_filter(vox, freq, 850, 1200, 2800)
    lower = sf.BesselLowPass(+vox,freq    ,2)
    higher = sf.Power(sf.BesselHighPass(vox, freq*4.0, 2), 1.25)
    higher = sf.Clean(higher)
    higher = sf.ButterworthHighPass(higher, freq*1.5 ,6)
    lower = sf.ButterworthHighPass(lower, freq*0.75 ,6)
    return sf.Realise(mix(sf.Pcnt95(lower), sf.Pcnt5(higher)))
    
@sf_parallel
def femail_soprano_a_filter(vox,length,freq):
    vox = _vox_filter(vox, freq, 860, 2050, 2850)
    a = sf.BesselLowPass(+vox,freq    ,2)
    b = sf.Power(sf.BesselHighPass(vox,freq*4.0,2),1.35)
    b = sf.Clean(b)
    b = sf.ButterworthHighPass(b,freq*1.5 ,6)
    a = sf.ButterworthHighPass(a,freq*0.75,6)
    return mix(sf.Pcnt75(a),sf.Pcnt25(b))

@sf_parallel
def femail_soprano_ma_filter(vox, length,freq):
    vox = vox_humana_femail_soprano_a(vox,length,freq)
    if length>128:
        qsh =sf.NumericShape((0,0.1),(120,2),  (length,0.1))
        msh =sf.NumericShape((0,1.0),(120,1.0),(length,0.0))
        mshr=sf.NumericShape((0,0.0),(120,0.0),(length,1.0))
        init=byquad_filter('low',+vox,freq,qsh)
        vox =sf.Multiply(vox ,mshr)
        init=sf.Multiply(init,msh)
        vox =mix(vox,init)
        vox=sf.FixSize(polish(vox,freq))
    return vox

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

def synthichord_filter(sig, length, freq):

    with SFMemoryZone():

        vibAbove = 250
        if length > vibAbove:
            # TODO feels a bit crushed - more stages?
            vibStart  = length*0.5  if length>600 else vibAbove*0.75
            vibMiddle = length*0.75 if length>600 else vibAbove
            vibAmount = 0.5 if length > 1000 else 0.25
            trueLen = sf.Length(+sig)
            l = trueLen
            env = sf.NumericShape((0, 0), (vibStart, 0), (vibMiddle, 1), (length, 0.75), (l, 0))
            env = sf.NumericVolume(env, vibAmount)
            trem = sf.SineWave(l,2.0 + random.random())
            trem = sf.MakeTriangle(trem)
            trem = sf.Multiply(env, trem)
            vib = +trem
            trem = sf.DirectMix(1, sf.Pcnt50(trem))
            sig = sf.Multiply(trem, sig)
            vib = sf.DirectMix(1, sf.NumericVolume(vib, 0.01))
            sig = sf.Resample(vib, sig)
        
        env = sf.SimpleShape(
            (0,       sf.ToDBs(18000)),
            (length,  sf.ToDBs(freq * 2.5))
        )
        env = sf.Cut(0, length, env)
        env = sf.Multiply(
            env,
            sf.NumericShape((0,1), (length - 35,1), (length, 0.1))
        )

        res = None
        if length > 50:
            res = sf.NumericShape(
                (0, 0.5),
                (length - 50, 0.5),
                (length, 0.9)
            )
        else:
            res = sf.NumericShape(
                (0, 0.5),
                (length, 0.8)
            )
        res = sf.Cut(0, length, res)
        sig, env, res = sf.MatchLengths((sig, env, res))
        out = sf.ShapedLadderLowPass(sig, env, res)

        attack = 5
        env = None
        if length > 50:
            env = sf.SimpleShape((0, -40), (attack,0), (length, -20))
            env = sf.Multiply(
                env,
                sf.NumericShape((0,1), (length - 20,1), (length, 0))
            )
        else:
            env = sf.NumericShape((0, 0.00), (attack,0), (length, 0))
        out = sf.Multiply(env, out)
        return sf.FixSize(polish(out, freq)).flush()