# Use rather than sf.Mix and sf.MixAt because it accepts either a list
# of singals or a list of signal,time lists and does the right thing and
# will run in parallel etc. 

@sf_parallel
def mix(*args):
    if len(args)==1:
        notes=args[0]
    else:
        notes=args
    if not isinstance(notes,(list,tuple)):
        raise TypeError('Mix required list was passed: ' + str(type(notes)))
    if isinstance(notes[0],(list,tuple)):
        return sf.Realise(sf.MixAt(notes))
    else:
        return sf.Realise(sf.Mix(notes))

# Mixes if required and converts to a realised form for further processing

@sf_parallel
def realise(signal):
    if isinstance(signal,list):
        return mix(signal)
    else:
        return sf.Realise(signal)

# Mixes if required and finalises which means to convert to an maximum diviation
# from zero or + or - 1 and remove all frequencies above half nyquist.

@sf_parallel
def finalise(signal):
    return sf.Finalise(signal)