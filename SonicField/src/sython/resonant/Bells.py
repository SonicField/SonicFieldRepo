from sython.concurrent import sf_parallel
from com.nerdscentral.audio.core import SFMemoryZone
from sython.utils.Generative import PrimeRange, tweakRandom
from sython.voices.ResonantVoices import make_addtive_resonance

dullRange = PrimeRange(biggerThan = 9, lessThan = 30,  divisor = 10.0)
brightRange = PrimeRange(biggerThan = 9, lessThan = 120, divisor = 10.0)

@sf_parallel
def primeBell(frequency = 440 , brightness = 1.0, length = 10000, hit = 1.0, isBowl = True):
    
    with SFMemoryZone():
        saturate = 0.0
        qCorrect = 3.0
        if frequency > 2000:
            # Ouch.
            frequency *= 0.5
            saturate = 0.1
            qCorrect = 1.0
        
        qc = 1.0 if brightness < 2.0 else 3.0
        harmonics = dullRange if brightness < 2.0 else brightRange
        harmonics = tweakRandom(harmonics(), 0.05)
        
        if isBowl:
            length += 4000 + length
        
        with SFMemoryZone():
            gen = make_addtive_resonance(
                qCorrect = qCorrect, 
                post = None, 
                rollOff = 3.0, 
                power = brightness, 
                harmonics = harmonics,
                seed = -40,
                saturate = saturate
            )
            sig = gen(length, frequency).keep()

        sig = sf.Mix(
            sig,
            sf.RBJLowPass(
                sf.Multiply(
                    sf.WhiteNoise(30),
                    sf.NumericShape((0, hit/4.0), (30,0))
                ),
                frequency * 4.0,
                2.0
            )
        )
        
        peak = 2000 if isBowl else 1
        env = sf.NumericShape(
            (0, frequency if isBowl else 18000),
            (peak, 18000 if isBowl else frequency * 4.0),
            (length, frequency)
        )
        res = sf.NumericShape((1, 1.0),(length,3.0 if isBowl else 1.5))
        sig = sf.ShapedRBJLowPass(sig, env, res)
    
        # A mixture of linear and exponential enveloping.
        env = sf.Multiply(
            sf.NumericShape(
                (0, 0),
                (peak, 1),
                (length, 0)),
            sf.SimpleShape((0, 0), (peak, 0), (length, -30))
        )
        out = sf.FixSize(sf.Multiply(env, sig))
        return out.flush()