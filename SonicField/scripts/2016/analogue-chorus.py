from Filters import byquad_filter
import random
sig = sf.SineWave(10000,440)
env = sf.LinearShape((0,0),(10,1),(1024,0.1),(10000,0))
sig = +sf.Multiply(env,sig)
dhz = 32
dly = 1000.0/float(dhz)
frq = 32
mod = +sig
sig = sf.AnalogueChorus(sig,dly,mod,0.75,1.25)
out = [ sf.FixSize(s) for s in sig ]
sf.WriteFile32(out,'temp/ac.wav')

