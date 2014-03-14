import math
all=sf.Slide((0,1000),(60000,2000))
bunch=sf.Granulate(all,1000,10)
for (sig,at) in bunch:
    print (sf.Length(sig),at,sig.getClass())
    sig=sf.Normalise(sig)
    sf.WriteFile16((sig,sig),"temp/"+at.__str__()+".wav")
    
all=sf.Normalise(sf.MixAt(bunch))
sf.WriteFile32((all,all),"temp/temp.wav")