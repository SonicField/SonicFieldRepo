Sig = sf.MakeTriangle(sf.SineWave(10000,440))
env = sf.NumericShape((0,0),(10,1),(1024,0.1),(10000,0))
sig = +sf.Multiply(env,sig)
dhz = 8
dly = 1000.0/float(dhz);
mod = sf.SineWave(10000,1.0)
sig = sf.AnalogueChorus(sig,dly,mod,0.75,1.25)
out = [ sf.FixSize(s) for s in sig ]
sf.WriteFile32(out,'temp/ac.wav')

