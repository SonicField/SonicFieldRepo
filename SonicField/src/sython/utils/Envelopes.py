################################################################################

################################################################################

def safe_env(sig,env):
    '''
    Take an envelope description (tuple of time,numeric-volume pairs) and ensure
    that the elements are in ascenting order and not so close that an audible
    'click' will be heard. If the resulting envolope has to be longer then the 
    original it logs a warning.
    '''
    length=sf.Length(+sig)
    ne=[]
    op=0
    for p,m in env:
        if p-op<16 and p>0:
            d_log("Warning: envelope to tight: ", env)
            p=op+16
        op=p
        ne.append([p,m])
    if p>length:
        d_log("***WARNING: envelop failure *** ",length," -> ",p," diff:", p-length)
    return sf.NumericShape(ne)