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


@sf_parallel
def vox_humana_inner(length,freq,a,b,c,z1=1.0,z2=1.25):
    vox=mix(
        sf.Pcnt75(sing_base(length,freq,z1)),
        sf.Pcnt25(sing_base(length,freq*2.0,z1))
    )
    length=sf.Length(+vox)
    vox=sf.FixSize(polish(vox,freq)) 
    if length>1024:
        rt=3.0
        dp=0.2
        pdp=0.1
    else:
        rt=4.5
        dp=0.1
        pdp=0.05
    if length>2048:
        a=length*0.75
    else:
        a=length*0.5
    vox=create_vibrato(vox,length,longer_than=512,rate=rt,depth=dp,pitch_depth=pdp,at=a)
    vox=do_formant(vox,a,b,c,freq)
    vox=polish(vox,freq)        
    vox=excite(vox,0.2,2.0)
    vox=polish(vox,freq)
    notch=(freq+a)/2.0      
    vox=mix(
        sf.Pcnt75(sf.RBJNotch(+vox,notch,0.5)),
        sf.Pcnt25(vox)
    )

    vox=mix(
        sf.Multiply(
            sf.FixSize(sf.Power(clean_noise(length,freq*0.5),1.5)),
            sf.SimpleShape((0,-60),(64,-35),(128,-40),(length,-60))
        ),
        vox
    )
    
    vox=polish(vox,freq)
    vox=sf.RBJPeaking(vox,freq,3,4)
    vox=polish(vox,freq)
    return sf.FixSize(vox)

@sf_parallel
def vox_humana_femail_soprano_ah(length,freq):
    vox = vox_humana_inner(length,freq,850,1200,2800,2.0,3.0)
    a = sf.BesselLowPass(+vox,freq    ,2)
    b = sf.Power(sf.BesselHighPass(vox,freq*4.0,2),1.25)
    b = sf.Clean(b)
    b = sf.ButterworthHighPass(b,freq*1.5 ,6)
    a = sf.ButterworthHighPass(a,freq*0.75,6)
    return sf.Realise(mix(sf.Pcnt75(a),sf.Pcnt25(b)))
    
@sf_parallel
def vox_humana_femail_soprano_a(length,freq):
    vox = vox_humana_inner(length,freq,860,2050,2850,1.8,2.5)
    a = sf.BesselLowPass(+vox,freq    ,2)
    b = sf.Power(sf.BesselHighPass(vox,freq*4.0,2),1.35)
    b = sf.Clean(b)
    b = sf.ButterworthHighPass(b,freq*1.5 ,6)
    a = sf.ButterworthHighPass(a,freq*0.75,6)
    return mix(sf.Pcnt75(a),sf.Pcnt25(b))

@sf_parallel
def vox_humana_femail_soprano_ma(length,freq):
    vox = vox_humana_femail_soprano_a(length,freq)
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

def vox_human_mail_soprano(length,freq):
    return vox_humana_inner(length,freq,850,1200,2800)

@sf_parallel
def celest_flute(length,freq):
    sig=mix(
        sf.Pcnt50(sweet_flute_base(length,freq)),
        sf.Pcnt50(sweet_flute_base(length,freq+1.0)),
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-28),(128,-40),(length,-40))
        )
    )
    return pitch_move(sig)

@sf_parallel
def sweet_flute(length,freq):
    sig=mix(
        sweet_flute_base(length,freq),
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-30),(128,-40),(length,-40))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def trost_bright_flute(length,freq):
    sig  = bright_flute_base(length,freq)
    wind = sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-16),(128,-20),(length,-20))
    )
    sig  = sf.Multiply(
        sf.NumericShape((0,0),(32,1),(length,1)),
        sig
    )
    start=sf.Multiply(
            mix(
                sf.FixSize(sf.Power(sf.MakeSawTooth(sf.SineWave(length,freq*0.500)),2.0)),
                sf.FixSize(sf.Power(sf.MakeSawTooth(sf.SineWave(length,freq*0.123)),2.0))
            ),
            sf.NumericShape((0,3.0),(32,0),(length,0))            
    )
    start=polish(start,freq*0.5)
    sig=mix(sig,start,wind)
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def trost_sweet_flute(length,freq):
    sig  = sweet_flute_base(length,freq)
    wind = sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-16),(128,-20),(length,-20))
    )
    sig  = sf.Multiply(
        sf.NumericShape((0,0),(32,1),(length,1)),
        sig
    )
    start=sf.Multiply(
            mix(
                sf.FixSize(sf.Power(sf.MakeSawTooth(sf.SineWave(length,freq*0.500)),2.0)),
                sf.FixSize(sf.Power(sf.MakeSawTooth(sf.SineWave(length,freq*0.123)),2.0))
            ),
            sf.NumericShape((0,3.0),(32,0),(length,0))            
    )
    start=polish(start,freq*0.5)
    sig=mix(sig,start,wind)
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def bright_flute(length,freq):
    sig=mix(
        bright_flute_base(length,freq),
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-28),(128,-40),(length,-40))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def string(length,freq):
    def ms():
        sig=mix(
            sf.Pcnt45(sf.MakeTriangle(sf.PhasedSineWave(length,freq,random.random()))),
            sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq*2.0,random.random()))),
            sf.Pcnt15(sf.MakeTriangle(sf.PhasedSineWave(length,freq*4.0,random.random()))),
            sf.Pcnt15(sf.PhasedSineWave(length,freq*6.0,random.random())),
            sf.Multiply(
                clean_noise(length,freq),
                sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
            )
        )
        sig=polish(sig,freq)
        sig=sf.FixSize(polish(sf.Saturate(sig),freq))
        return pitch_move(sig)
        
    return sf.FixSize(mix(ms(),ms()))

@sf_parallel
def viola(length,freq):
    sig=mix(
        viola_base(length,freq),
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def cello(length,freq):
    sig=string(length,freq)
    sig=create_vibrato(
        sig,length,
        longer_than=512,
        rate=2.5,
        at=450,
        depth=0.25,
        pitch_depth=0.02
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def plucked_glass(length,freq):
    sig=mix(        
            sf.Pcnt50(stretched_bass(length,freq,z=3.5,s=1.005,d=1.00,at=3000)),
            sf.Pcnt50(stretched_bass(length,freq,z=3.5,s=1.005, d=1.00,at=1500))
        )
    sig=sf.Multiply(
        sig,
        sf.NumericShape((0,0.5),(16,1),(length,1))
    )
    sig=sf.BesselLowPass(sig,freq*5,1)
    sig=sf.Multiply(
        sig,
        sf.DirectMix(1,sf.Pcnt5(sf.SineWave(length,16+random.random()*8.0)))
    )
    sig=sf.RBJPeaking(sig,freq*5,0.5,5)
    
    start=sf.Multiply(
            mix(
                sf.FixSize(sf.MakeSawTooth(sf.SineWave(length,120))),
                sf.FixSize(sf.MakeSawTooth(sf.SineWave(length,freq*0.75))),
                sf.FixSize(sf.MakeSawTooth(sf.SineWave(length,freq**0.5)))
            ),
            sf.SimpleShape((0,1.0),(16,-30),(32,-60),(length,-99))            
    )
    start=sf.Multiply(
        start,
        sf.NumericShape((0,0),(16,1),(length,0),(length*0.5,0))
    )            
    start=sf.Clean(start)
    conv =clean_noise(64,32)
    conv =sf.Multiply(
        sf.SimpleShape((0,-12),(60,-60),(66,-60)),
        conv
    )
    conv=sf.Multiply(
        conv,
        sf.NumericShape((0,0),(60,1),(66,0))
    ) 
    conv=reverberate(start,conv)
    conv=sf.Multiply(
        conv,
        sf.SimpleShape((0,1.0),(16,-30),(32,-60),(length,-99))
    )
    sig=mix(sf.FixSize(sig),sf.Pcnt50(sf.FixSize(conv)))
    sig=sf.Cut(0,length,sig)
    return sf.FixSize(polish(sig,64))
  
@sf_parallel
def bright_plucked_glass(length,freq):
    sig=mix(
        stretched_bass(length,freq,z=1.5,s=1.02,d=1.0,at=4000),
        sf.Multiply(
            clean_noise(length,freq*2.0),
            sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
        )
    )
    sig=sf.RBJPeaking(sig,freq*5,0.5,5)
    start=sf.Multiply(
            mix(
                sf.FixSize(sf.MakeSawTooth(sf.SineWave(length,freq*0.75))),
                sf.FixSize(sf.MakeSawTooth(sf.SineWave(length,freq**0.5)))
            ),
            sf.SimpleShape((0,1.0),(16,-30),(32,-60),(length,-99))            
    )
    start=sf.Clean(start)
    sig=mix(sf.FixSize(sig),sf.FixSize(start))
    return sf.FixSize(sf.Clean(sig))

@sf_parallel
def trost_lead_diapason(length,freq):
    sig=mix(
        sf.Multiply(
            mix(
                sf.Pcnt65(sf.MakeTriangle(sf.PhasedSineWave(length,freq,random.random()))),
                sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq*2.0,random.random()))),
                sf.Pcnt15(sf.PhasedSineWave(length,freq*4.0,random.random()))
            ),
            sf.NumericShape((0,0),(48,1),(length,1))
        ),       
        sf.Multiply(
            sf.MakeSquare(sf.SineWave(length,freq*0.5)),
            sf.NumericShape((0,0.5),(48,0),(length,0))            
        ),
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def lead_diapason(length,freq):
    sig=mix(
        sf.Pcnt65(sf.MakeTriangle(sf.PhasedSineWave(length,freq,random.random()))),
        sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq*2.0,random.random()))),
        sf.Pcnt15(sf.PhasedSineWave(length,freq*4.0,random.random())),
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def second_diapason(length,freq):
    sig=mix(
        sf.MakeTriangle(sf.PhasedSineWave(length,freq,random.random())),
        sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq*2.0,random.random()))),
        sf.Pcnt15(sf.PhasedSineWave(length,freq*4.0,random.random())),
        sf.Pcnt5(sf.PhasedSineWave(length,freq*8.0,random.random())),
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-20),(128,-26),(length,-20))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def grand_bass(length,freq):
    sig=mix(
        mix(
            [sf.Pcnt25(nice_saw(length,freq)),0],
            [sf.Pcnt50(sf.MakeTriangle(sf.PhasedSineWave(length,freq*0.501,random.random()))),32],
            [sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq*0.252,random.random()))),64],
            [sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq/8.01,random.random()))),64]
        ),          
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-12),(128,-24),(length,-20))
        )
    )
    sig=sf.BesselLowPass(sig,freq*3.0,1)
    sig=sf.FixSize(polish(sig,freq))
    sig=pitch_move(sig)
    sig=sf.ButterworthLowPass(sig,freq*9.0,1)
    return sf.FixSize(sf.Clean(sig))

@sf_parallel
def double_bombard(length,freq):
    sig=mix(
        mix(
            [sf.Pcnt50(bombard_pulse(length,freq)),0],
            [sf.Pcnt50(bombard_pulse(length,freq*1.5)),10]
        ),          
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-10),(128,-24),(length,-20))
        )
    )
    return pitch_move(sig)

@sf_parallel
def trost_posaune(length,freq):
    b=posaunePulse(length,freq)
    b=mix(
        [b,12],
        [
        sf.NumericShape(
            (0, -2.0),
            (4,  2.0),
            (12,-1.00),
            (20, 1.00),
            (28,-1.00),
            (length,0)
        ),0]
    )
    b=sf.RBJPeaking(b,freq*2,2,2)
    b=polish(b,freq)
    sig=mix(
        b
        ,
        sf.Pcnt20(sf.Multiply(+b,sf.WhiteNoise(length))),          
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-14),(128,-28),(length,-24))
        )
    )
    return pitch_move(sig)

@sf_parallel
def bombard(length,freq):
    b=mix(
            [sf.Pcnt33(bombard_pulse(length,freq)),0],
            [sf.Pcnt33(bombard_pulse(length,freq)),10],
            [sf.Pcnt33(bombard_pulse(length,freq)),20]
    )
    b=polish(b,freq)
    sig=mix(
        b
        ,
        sf.Pcnt10(sf.Multiply(+b,sf.WhiteNoise(length))),          
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-14),(128,-28),(length,-24))
        )
    )
    return pitch_move(sig)

@sf_parallel
def single_bombard(length,freq):
    sig=mix(
        sf.Pcnt33(bombard_pulse(length,freq))
        ,          
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-16),(128,-24),(length,-20))
        )
    )
    return pitch_move(sig)

@sf_parallel
def ophicleide(length,freq):
    b=mix(
            [sf.Pcnt10(ophicleide_pulse(length,freq*0.25-1)),0],
            [sf.Pcnt20(ophicleide_pulse(length,freq)),15],
            [sf.Pcnt20(ophicleide_pulse(length,freq)),10],
            [sf.Pcnt20(ophicleide_pulse(length,freq*2.0)),5],
            [sf.Pcnt20(ophicleide_pulse(length,freq*3.0)),0]
    )
    b=mix(
        sf.Power(+b,2.0),
        sf.Pcnt10(sf.Raise(+b,2.0)),
        sf.Pcnt50(b)
    )
    b=polish(b,freq)
    sig=mix(
        sf.Pcnt90(b)
        ,
        sf.Pcnt10(sf.FixSize(sf.Multiply(+b,clean_noise(length,freq*2.0)))),          
        sf.Multiply(
            clean_noise(length,freq*0.5),
            sf.SimpleShape((0,-60),(64,-16),(128,-20),(length,-22))
        )
    )
    return sig

@sf_parallel
def upper_accent(length,freq):
    return sf.FixSize(
        polish(
            sf.Power(
                polish(
                    sf.MakeTriangle(
                        sf.SineWave(length,freq)
                    ),
                    freq
                )
               ,2.0
            ),
            freq
        )
    )

@sf_parallel
def trost_upper_accent(length,freq):
    sig=mix(
        sf.Multiply(
            upper_accent(length,freq),
            sf.NumericShape((0,0),(48,1),(length,1))
        ),       
        sf.Multiply(
            sf.MakeSquare(sf.SineWave(length,freq*0.5)),
            sf.NumericShape((0,0.5),(48,0),(length,0))            
        ),
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
        )
    )
    sig=sf.FixSize(polish(sig,freq))
    return pitch_move(sig)

@sf_parallel
def clarion(length,freq):
    s1=sf.MakeTriangle(sf.PhasedSineWave(length,freq,random.random()))
    s2=sf.MakeTriangle(sf.PhasedSineWave(length,freq*2.001,random.random()))
    s3=nice_saw(length,freq*4.002)
    s1=polish(s1,freq)
    s2=polish(s2,freq)
    s3=polish(s3,freq)
    
    sig=mix(
        sf.Pcnt70(s1),
        sf.Pcnt20(s2),
        sf.Pcnt10(s3),
        sf.Multiply(
            clean_noise(length,freq*2.0),
            sf.SimpleShape((0,-60),(64,-20),(128,-36),(length,-36))
        )
    )

    sig=sf.FixSize(sf.Power(sig,10.0))
    sig=polish(sig,freq)
    sig=sf.BesselLowPass(sig,freq*6.0,2)
    return sf.FixSize(sf.Clean(sig))
    
@sf_parallel
def warm_bass(length,freq):
    sig=mix(
        sf.FixSize(
            sf.Power(
                sf.Clean(
                    mix(
                        [sf.Pcnt25(sf.MakeSquare  (sf.PhasedSineWave(length,freq      ,random.random()))),0],
                        [sf.Pcnt50(sf.MakeTriangle(sf.PhasedSineWave(length,freq*0.501,random.random()))),32],
                        [sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq*0.252,random.random()))),64],
                        [sf.Pcnt25(sf.MakeTriangle(sf.PhasedSineWave(length,freq/8.01,random.random()))),64]
                    )
                )
                ,1.25
            )
        ),      
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,-12),(128,-24),(length,-24))
        )
    )
    sh=sf.WhiteNoise(length)
    sh=sf.Multiply(sh,+sig)
    sig=mix(sig,sf.Pcnt5(sh))

    if freq<128:
        q=freq*6.0
    elif freq<256:
        q=freq*5
    else:
        q=freq*4.0
    sig=sf.BesselLowPass(sig,q,1)
    return pitch_move(sig)

@sf_parallel
def simple_oboe(length,freq):
    sig=sf.FixSize(
        sf.Power(
            sf.Clean(
                mix(
                    nice_saw(length,freq),
                    sf.PhasedSineWave(length,freq,random.random())
                )
            )
            ,
            1.5
        )
    )
    sig=polish(sig,freq)
    sig=sf.FixSize(sf.Power(sig,1.5))
    sig=polish(sig,freq)
    sig=sf.FixSize(sf.Power(sig,1.5))
    sig=polish(sig,freq)
    sig=sf.FixSize(sig)
      
    sig=sf.RBJPeaking(sig,freq*5,0.5,5)
    sig=sf.RBJPeaking(sig,freq*7,1,5)
    sig=sf.RBJNotch  (sig,freq*2,0.5,1)
    sig=sf.Clean(sig)
    
    sig=mix(
        sf.FixSize(sig),
        sf.Multiply(
            clean_noise(length,freq*9.0),
            sf.SimpleShape((0,-60),(64,-20),(128,-24),(length,-24))
        )
    )

    sig=sf.ButterworthLowPass (sig,freq*9,4)
    sig=polish(sig,freq)
    return sf.FixSize(sig)

@sf_parallel
def mute_oboe(length,freq):
    sig=sf.FixSize(
        sf.Power(
            sf.Clean(
                mix(
                    nice_saw(length,freq),
                    sf.PhasedSineWave(length,freq,random.random())
                )
            )
            ,
            1.5
        )
    )
    sig=polish(sig,freq)
    sig=sf.FixSize(sf.Power(sig,1.5))
    sig=polish(sig,freq)
    sig=sf.FixSize(sf.Power(sig,1.5))
    sig=polish(sig,freq)
    sig=sf.FixSize(sig)      

    sig=sf.RBJPeaking(sig,freq*5,0.5,5)
    sig=sf.RBJPeaking(sig,freq*7,1,5)
    sig=sf.RBJNotch  (sig,freq*2,0.5,1)
    sig=sf.Clean(sig)
    
    sig=mix(
        sf.FixSize(sig),
        sf.Multiply(
            clean_noise(length,freq*9.0),
            sf.SimpleShape((0,-60),(64,-16),(128,-20),(length,-20))
        )
    )

    sig=polish(sig,freq)
    sig=sf.BesselLowPass (sig,freq*4,1)
    osig=+sig
    osig=sf.BesselHighPass(sig,freq*4,2)
    sig=sf.BesselLowPass (sig,freq*6,1)
    sig=sf.BesselLowPass (sig,freq*8,4)
    sig=mix(sig,osig)
    return sf.FixSize(sf.Clean(sig))
    
@sf_parallel
def orchestral_oboe(length,freq):
    vox=make_simple_base(length,freq,0.25)
    vox=sf.Multiply(
        sf.NumericShape((0,0),(sf.Period(freq)/2.0,1),(length,1)),
        vox
    )
    vox=polish(vox,freq)
    vox=sf.Finalise(sf.Power(vox,1.5))
    vox=polish(vox,freq)
    vox=sf.Finalise(sf.Power(vox,1.5))
    vox=polish(vox,freq)
    vox=sf.RBJPeaking(vox,freq*5,0.5,5)
    vox=sf.RBJPeaking(vox,freq*7,1,5)
    vox=sf.RBJNotch  (vox,freq*2,0.5,1)
    vox=sf.FixSize(vox) 
    res=512*math.ceil(float(freq)/256.0)
    vox=mix(
        sf.RBJPeaking(+vox,res    ,1,8),
        sf.RBJPeaking( vox,res*4.0,1,8),
    )
    vox=sf.ButterworthLowPass(vox,freq*4,4) 
    vox=sf.FixSize(vox)
    nos=sf.Multiply(
        +vox,
        sf.Multiply(
            clean_noise(length,freq),
            sf.SimpleShape((0,-60),(64,0),(128,-9),(length,-9))
        )
    )
    vox=mix(
        vox,
        sf.Pcnt10(sf.FixSize(nos))
    )
    vox=polish(vox,freq)
    return sf.FixSize(vox)

@sf_parallel
def trost_orchestral_oboe(length,freq):
    start=sf.Multiply(
            mix(
                sf.FixSize(sf.Power(sf.MakeSawTooth(sf.SineWave(length,freq*0.500)),2.0)),
                sf.FixSize(sf.Power(sf.MakeSawTooth(sf.SineWave(length,freq*0.123)),2.0))
            ),
            sf.NumericShape((0,3.0),(32,0),(length,0))            
    )
    start=polish(start,freq*0.5)
    sig=mix(orchestral_oboe(length,freq),sf.FixSize(start))
    return sf.FixSize(polish(sig,freq))

@sf_parallel
def rich_reed(length,freq):
    s1=bombard_pulse(length,freq*1.000)
    s2=nice_pulse(length,freq+1)
    s3=nice_pulse(length,freq-1.5)
    s1=polish(s1,freq)
    s2=polish(s2,freq)
    s3=polish(s3,freq)
    
    sig=mix(
        sf.Pcnt70(s1),
        sf.Pcnt20(s2),
        sf.Pcnt10(s3),
        sf.Multiply(
            clean_noise(length,freq*2.0),
            sf.SimpleShape((0,-60),(64,-20),(128,-24),(length,-24))
        )
    )

    sig=sf.FixSize(sig)
    sig=polish(sig,freq)
    return sf.FixSize(sig)

@sf_parallel
def reed(length,freq):
    s1=nice_pulse(length,freq*1.000)
    s1=polish(s1,freq)
    
    sig=mix(
        s1,
        sf.Multiply(
            clean_noise(length,freq*2.0),
            sf.SimpleShape((0,-60),(64,-16),(128,-20),(length,-20))
        )
    )

    sig=sf.FixSize(sig)
    sig=polish(sig,freq)
    return sf.FixSize(sig)

@sf_parallel
def stopped_reed(length,freq):
    s1=stopped_pulse(length,freq*1.000)
    s1=polish(s1,freq)
    
    sig=mix(
        s1,
        sf.Multiply(
            clean_noise(length,freq*2.0),
            sf.SimpleShape((0,-60),(64,-16),(128,-20),(length,-20))
        )
    )

    sig=sf.FixSize(sig)
    sig=mix(
        sf.Pcnt10(sf.Clean(sf.Saturate(+sig))),
        sig
    )
    sig=polish(sig,freq)
    return sf.FixSize(sig)

@sf_parallel
def trumpet(length,freq):
    if length>256:
        sig1=trumpe_base(length,freq,-0.25)
        sig2=trumpet_base(length,freq, 0.25)
        env1=sf.NumericShape((0,0),(256,1),(length,1))
        env2=sf.NumericShape((0,1),(256,0),(length,0))
        sig1=sf.Multiply(sig1,env1)
        sig2=sf.Multiply(sig2,env2)
        sig=mix(sig1,sig2)
    else:
        sig=trumpet_base(length,freq,-0.25)
    
    sig=sf.FixSize(sig)
    sig=polish(sig,freq)

    sig=mix(
        sig,
        sf.Multiply(
            clean_noise(length,freq*1.0),
            sf.SimpleShape((0,-60),(32,-22),(64,-60),(length,-90))
        )
    )
        
    sig=sf.FixSize(sig)
    sig=polish(sig,freq)
    return sf.FixSize(sig)

@sf_parallel
def shawm(length,freq):
    s1=simple_sawtooth(length,freq)
    if freq>1000:
        s_freq=1000
    else:
        s_freq=freq*2.0
    
    sig=mix(
        s1,
        sf.Multiply(
            clean_noise(length,s_freq),
            sf.SimpleShape((0,-60),(64,-20),(128,-30),(length,-40))
        )
    )

    sig=sf.FixSize(sig)
    sig=byquad_filter('peak',sig,freq*3.0,1,6)
    sig=byquad_filter('low', sig,freq*6.0,1)
    sig=polish(sig,freq)
    return sf.FixSize(sig)

###############################################################################
# END BAROQUE VOICES
###############################################################################

###############################################################################
# START FOLK VOICES
###############################################################################

@sf_parallel
def folk_basson(length,freq):
    sig=sf.FixSize(
        sf.Power(
            phasing_sawtooth(length,freq)
            ,
            1.5
        )
    )
    sig=polish(sig,freq)
    sig=sf.FixSize(sf.Power(sig,1.5))
    sig=polish(sig,freq)
    sig=sf.FixSize(sf.Power(sig,1.5))
    sig=polish(sig,freq)
    sig=sf.FixSize(sig)
      
    sig=sf.RBJPeaking(sig,freq*5,0.5,5)
    sig=sf.RBJPeaking(sig,freq*7,1,5)
    sig=sf.RBJNotch  (sig,freq*2,0.5,1)
    sig=sf.Clean(sig)
    
    sig=mix(
        sf.FixSize(sig),
        sf.ButterworthLowPass (
            sf.Multiply(
                sf.MakeSquare(sf.SineWave(length,freq)),
                sf.SimpleShape((0,-60),(64,-32),(96,-60),(length,-60))
            ),
            freq*9,
            4
        )
    )

    sig=sf.ButterworthLowPass (sig,freq*9,2)
    sig=polish(sig,freq)
    return sf.FixSize(sig)

@sf_parallel
def folk_flute(length,freq):
    sig=mix(
        byquad_filter(
            'low',
            sf.Mix(
                phasing_triangle(length,freq),
                sf.Pcnt1(sf.MakeSquare(sf.SineWave(length,freq*0.9)))
            ),
            freq*2.0,
            2
        ),
        sf.Multiply(
            byquad_filter(
                'peak',
                clean_noise(length,freq*0.5),
                freq,
                0.5,
                16
            ),
            sf.SimpleShape((0,-60),(64,-28),(128,-40),(length,-40))
        )
    )
    sig=create_vibrato(
        sig,length,
        longer_than=512,
        rate=2.5,
        at=450,
        depth=0.5,
        pitch_depth=0.02
    )
    return sf.FixSize(polish(sig,freq))

def _distant_wind(length, freq, qCorrect = 1.25):
    length += 250.0
    base = sf.Mix(
        clean_noise(length,freq*4.0),
        sf.Volume(sf.SineWave(length,freq), -60)
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
                'peak',
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

def additive_resonance(power, qCorrect, saturate, rollOff, post, length, freq):

    @sf_parallel
    def doWind(lenth, hfq):
        out = _distant_wind(length, hfq, qCorrect)
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

def make_addtive_resonance(power = 1.1 , qCorrect = 1.25 , saturate = 0.0, rollOff = 4.0, post = None):
    return functools.partial(additive_resonance, power, qCorrect, saturate, rollOff, post)

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

@sf_parallel
def folk_clarinet(length,freq):
    s1=stopped_pulse(length,freq*1.000)
    sig=mix(
        s1,
        sf.Multiply(
            nice_saw(length,freq*0.5),
            sf.SimpleShape((0,-32),(64,-16),(128,-99),(length,-99))
        )
    )
    sig=polish(sig,freq)
    sig=polish(sf.Saturate(sf.FixSize(sig)),freq)
    return sf.FixSize(sig)   

def _nordic_string(
        freq=None,
        length=None,
        e=32,
        a=64,
        whiteAmount=0.1,
        vibStart=None,
        vibMiddle=None,
        vibAmount=0,
        vibRate=2.0,
        bright=True,
        body=None
    ):

    if freq is None:
        raise ValueError('Must provide freq')

    if length is None:
        raise ValueError('Must provide length')

    if vibAmount != 0:
        if vibMiddle is None:
            raise ValueError('Must provide vibMiddle if vibAmount')
        if vibStart is None:
            raise ValueError('Must provide vibStart if vibAmount')
    
    def raw_string(length,freq):
        def raw_stringA(l,p):
            return phasing_sawtooth(l,p)
        freq=float(freq)
        s1=raw_stringA(length,freq)
        s2=raw_stringA(length,freq*2.0)
        s3=raw_stringA(length,freq*4.0)
        s4=sf.WhiteNoise(length)
        signal=sf.FixSize(
            sf.Mix(
                sf.Pcnt20(s4),
                sf.Pcnt50(s1),
                sf.Pcnt20(s2),
                sf.Pcnt10(s3)
            )
        )
        signal=sf.Clean(sf.ResonantFilter(signal,0.95,0.15,sf.Period(freq)))
        multi=sf.Finalise(
            sf.DirectRelength(
                sf.ButterworthLowPass(sf.WhiteNoise(length/500.0),2500,6),
                0.001
            )
        )
        multi=sf.Cut(0,sf.Length(+signal),multi)
        signal=sf.Resample(
            sf.DirectMix(1,sf.NumericVolume(multi,0.001)),
            signal
        )
        return sf.Realise(sf.FixSize(sf.Clean(signal)))
    
    
    def play_string_clean(a,length,freq,whiteAmount,body):
        def rsd():
            return raw_string(length,freq)
        
        signal=0
        if(freq>500):
            signal=sf.FixSize(sf.Mix(rsd(),rsd(),rsd()))
        else:
            signal=sf.FixSize(sf.Mix(rsd(),rsd()))

        if not bright:
            if(freq>440):    
                signal=sf.ButterworthHighPass(signal,freq*0.5,6)
                signal=sf.ButterworthHighPass(signal,2000,1)
                signal=sf.ButterworthLowPass(signal,8000,1)
            if(freq<128):
                signal=sf.ButterworthHighPass(signal,freq*0.5,1)
                signal=sf.ButterworthHighPass(signal,500,1)
                signal=sf.ButterworthLowPass(signal,2000,1)
            else:
                signal=sf.ButterworthHighPass(signal,freq*0.5,3)
                signal=sf.ButterworthHighPass(signal,1500,1)
                signal=sf.ButterworthLowPass(signal,4000,1)
        
            signal=sf.ButterworthLowPass(signal,freq*10.0,1)
            
        signal=sf.Mix(
            sf.Pcnt25(+signal),
            sf.Pcnt75(sf.RBJNotch(signal,freq,0.5))
        )    

        white=sf.WhiteNoise(length)
        white=sf.ButterworthHighPass(white,freq*2.0,2)
        white=sf.ButterworthLowPass(white,freq*6.0,1)
        white=sf.FixSize(white)
        white=sf.Multiply(white,+signal)
        white=sf.NumericVolume(white,whiteAmount)
        signal=sf.NumericVolume(signal,1.0-whiteAmount)
        signal=sf.FixSize(sf.Mix(signal,white))
    
        sq=sf.Mix(
            sf.PhasedSineWave(length,freq*0.95,random.random()),
            sf.PhasedSineWave(length,freq*1.05,random.random())
        )
        envb=sf.NumericShape((0,0.25),(a,0),(length,0))
        sq=sf.Multiply(envb,sf.FixSize(sq))
    
        enva=sf.NumericShape((0,0.75),(a,1),(length,1))
        signal=sf.Multiply(enva,signal)
    
        signal=sf.Mix(sq,sf.FixSize(signal))
        
        sigs=[]
        bodies=[]
        if body is None:
            if(freq<128):
                bodies=bassIRs
            elif(freq<440):
                bodies=celloIRs
            else:
                bodies=violinIRs
        else:
            bodies={
                'bass'  : bassIRs,
                'cello' : celloIRs,
                'violin': violinIRs
            }[body]

        if bright:
            bs=[]
            for b in bodies:
                bs.append(sf.Power(b,1.25))
            bodies=bs
            if freq<256:
                signal=sf.Power(signal,1.5)
            elif freq<512:
                signal=sf.Power(signal,1.25)
            elif freq<1024:
                signal=sf.Power(signal,1.15)
            else:
                signal=sf.Power(signal,1.05)
        
        env=sf.NumericShape((0,0),(16,1),(length-16,1),(length,0))
        signal=sf.Multiply(env,signal)

        for body in bodies:
            sigs.append(convolve(+signal,+body))
            
        signal=sf.Mix(
             sf.FixSize(sf.Mix(sigs)),
             sf.NumericVolume(sf.FixSize(signal),0.05)
        )
        
        return sf.FixSize(sf.Clean(signal))

    sigs=[]
    for x in range(0,5):
        sigs.append(play_string_clean(a,length,freq,whiteAmount,body))
    signal=sf.FixSize(sf.Mix(sigs))

    trueLen=sf.Length(+signal)
    envA=sf.SimpleShape(
        (0,-60),
        (e,0),
        (trueLen,0)
    )
    envB=sf.NumericShape(
        (0,0),
        (a,1),
        (trueLen,1)
    )
    env=sf.Multiply(envA,envB)
    
    signal=sf.Multiply(signal,env)
    if(vibAmount>0):
        l=trueLen
        env=sf.NumericShape((0,0),(vibStart,0),(vibMiddle,1),(length,0.75),(l,0))
        env=sf.NumericVolume(env,vibAmount)
        trem=sf.SineWave(l,2.0+random.random())
        trem=sf.Multiply(env,trem)
        vib=+trem
        trem=sf.DirectMix(1,sf.Pcnt50(trem))
        signal=sf.Multiply(trem,signal)
        vib=sf.DirectMix(1,sf.NumericVolume(vib,0.01))
        signal=sf.Resample(vib,signal)
    
    if(freq>128):
        signal=sf.ButterworthHighPass(signal,freq*0.75,6)
        if not bright:
            signal=sf.BesselLowPass(signal,freq,1)
    else:
        signal=sf.ButterworthHighPass(signal,freq*0.75,3)
        
    return sf.Realise(sf.FixSize(sf.Clean(signal)))

@sf_parallel
def nordic_cello(length,freq,vibAbove=250):
    params={
        'freq'      :freq,
        'length'     :length,
        'e'          :32,
        'a'          :64,
        'whiteAmount':0.1 if freq < 720 else 0.05,
        'vibStart'   :None,
        'vibMiddle'  :None,
        'vibAmount'  :0,
        'vibRate'    :2.0+random.random()*0.5,
        'bright'     :True,
        'body'       :'cello'
    }

    # Vibrato on longer nodes
    if length > vibAbove:
        # TODO feels a bit crushed - more stages?
        params['vibStart'] = length*0.5  if length>600 else vibAbove*0.75
        params['vibMiddle']= length*0.75 if length>600 else vibAbove
        params['vibAmount']= 0.5 if length > 1000 else 0.25

    return _nordic_string(**params)

def pure_sine(length,freq):
    return sf.Realise(sf.PhasedTableSineWave(length,freq,random.random()))

def pure_triangle(length,freq):
    return sf.Realise(limited_triangle(length,freq,12))

@sf_parallel
def simple_bell(length,pitch):
    length = length * 0.75
    sig1 = sf.SineWave(length,pitch*1.2)
    sig2 = sf.SineWave(length,pitch*1.2 + 1)
    env  = sf.SimpleShape((0,-60),(125,0),(length,-30))
    
    sig1 = sf.Multiply(+env,sig1)
    sig1 = sf.Pcnt90(sf.DirectMix(1,sig1))
    sig3 = sf.PhaseModulatedSineWave(pitch,sig1)
    sig3 = sf.Multiply(+env,sig3)

    sig2 = sf.Multiply(+env,sig2)
    sig2 = sf.Pcnt90(sf.DirectMix(1,sig2))
    sig4 = sf.PhaseModulatedSineWave(pitch,sig2)
    sig4 = sf.Multiply(env,sig4)
    
    sig5 = sf.Volume(sf.Mix(sig3,sig4),6)
    sig=sf.Saturate(sig5)
    #sig=sf.ResonantFilter(sig,0.99,0.05,1000.0/pitch)
    return sf.Realise(sf.Finalise(sig))

@sf_parallel
def strange_bell(length,pitch):
    print "Ring: " + str(pitch) + "/" + str(length)
    sig1 = sf.SineWave(length,pitch*1.2)
    sig2 = sf.SineWave(length,pitch*1.2 + 1)
    env  = sf.SimpleShape((0,0),(256,-10),(length,-99))
    
    sig1 = sf.Multiply(+env,sig1)
    sig1 = sf.Pcnt90(sf.DirectMix(1,sig1))
    sig3 = sf.PhaseModulatedSineWave(pitch,sig1)
    sig3 = sf.Multiply(+env,sig3)

    sig2 = sf.Multiply(+env,sig2)
    sig2 = sf.Pcnt90(sf.DirectMix(1,sig2))
    sig4 = sf.PhaseModulatedSineWave(pitch,sig2)
    sig4 = sf.Multiply(env,sig4)
    
    sig5 = sf.Volume(sf.Mix(sig3,sig4),6)
    sig=sf.Saturate(sig5)
    sig=sf.Concatenate(+sig,sf.Silence(sf.Length(sig)))
    sig=sf.ResonantFilter(sig,0.99,0.01,1000.0/pitch)
    sig=sf.Cut(128,sf.Length(+sig),sig)
    return sf.Realise(sf.Finalise(sig))

@sf_parallel
def clear_bell(length,pitch,dullness=0):
    length+=128
    print "Ring: " ,length,pitch,dullness
    signal1=sf.SineWave(length,pitch-2)
    signal2=sf.SineWave(length,pitch+2)
    fm1=sf.DB48(sf.SineWave(length,pitch*1.2))
    fm2=sf.DB48(sf.SineWave(length,pitch*1.3))
    
    signal=sf.Mix(
        sf.FrequencyModulate(signal1,fm1),
        sf.FrequencyModulate(signal2,fm2)
    )
    signal=sf.Normalise(signal)
    
    wn=sf.ButterworthLowPass(sf.WhiteNoise(100),pitch,2)
    wn=sf.Multiply(wn,sf.NumericShape((0,0),(20,1),(100,0)))
    
    signal=sf.Mix(signal,wn)
    signal=sf.Normalise(signal)
    
    env=sf.SimpleShape(
        (0,-99),
        (50,0),
        (length-128,-60),
        (length,-99)
    )
    
    signal=sf.Multiply(
        signal,
        env
    )
    
    signal=sf.ButterworthHighPass(signal,pitch*2,2)
    if dullness>0:
        return sf.Finalise(sf.BesselLowPass(signal,pitch,dullness))
    else:
        return sf.Finalise(signal)
    

