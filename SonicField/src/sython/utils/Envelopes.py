################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
################################################################################

def _safeEnv(sig, env):
    '''
    Take an envelope description (tuple of time,numeric-volume pairs) and ensure
    that the elements are in ascenting order and not so close that an audible
    'click' will be heard. If the resulting envolope has to be longer then the 
    original it logs a warning.
    '''
    length=sf.Length(sig)
    ne=[]
    op=0
    for p, m in env:
        if p - op < 16 and p>0:
            d_log("Warning: envelope to tight: ", env)
            p = op + 16
        op=p
        ne.append((p, m))
    if p > length:
        raise RuntimeError("Envelope failure - click removal caused overflow: ",length," -> ",p," diff:", p-length)
    return ne

def linearEnv(sig, env):
    if env[-1][1] != 0:
        raise RuntimeError('Envelope failure - last element none zero')
    if env[0][1] !=0:
        raise RuntimeError('Envelope failure - first element non zero')
    correctedEnv = _safeEnv(sig, env)
    return sf.LinearShape(correctedEnv)

def dbsEnv(sig, env):
    if abs(env[-1][1]) > 1.0e-6:
        raise RuntimeError('Envelope failure - last element none trivial: %f' % env[-1][1])
    if abs(env[0][1]) > 1.0e-6:
        raise RuntimeError('Envelope failure - first element non trivial: %f' % env[0][1])
    correctedEnv = _safeEnv(sig, env)
    return sf.ExponentialShape(correctedEnv)
