left  = sf.ReadSignal("temp/c_left")
right = sf.ReadSignal("temp/c_right")
t = 0
l = max(sf.Length(+left),sf.Length(+right))
c = 0
while t<l: 
    e = t + 1800000.0
    e = min(l, e)
    # TODO is left and right are different lengths
    # will this fail?
    leftX  = sf.Cut(t, e, +left)
    rightX = sf.Cut(t, e, +right)
    sf.WriteFile32((leftX,rightX), "temp/out_{0}.wav".format(c))
    c += 1
    t = e

-left
-right
