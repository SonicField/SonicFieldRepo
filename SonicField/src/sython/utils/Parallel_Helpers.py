# Use rather than sf.Mix and sf.MixAt because it accepts either a list
# of signals or a list of signal,time lists and does the right thing and
# will run in parallel etc. 

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

def realise(*args):
    if len(args)>1:
        return mix(args)
    else:
        return sf.Realise(args[0])

# Mixes if required and finalises which means to convert to an maximum diviation
# from zero or + or - 1 and remove all frequencies above half nyquist.

def finalise(*args):
    if len(args)>1:
        return sf.Finalise(mix(args))
    elif isinstance(args[0],(list,tuple)):
        return sf.Finalise(mix(args[0]))
    else:
        return sf.Finalise(args[0])