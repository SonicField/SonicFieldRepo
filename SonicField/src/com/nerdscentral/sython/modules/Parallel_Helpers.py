# Use rather than sf.Mix and sf.MixAt because it accepts either a list
# of singals or a list of signal,time lists and does the right thing and
# will run in parallel etc. 

@sf_parallel
def mix(notes):
    if not isinstance(notes,list):
        raise TypeError('Mix required list was passed: ' + str(typeof(notes)))
    if isinstance(notes[0],list):
        return sf.Realise(sf.MixAt(notes))
    else:
        return return sf.Realise(sf.Mix(notes))

# Mixes if required and converts to a realised form for further processing

@sf_parallel
def realise(signal):
    if isinstance(notes[0],list):
        return mix(signal)
    else:
        return sf.Realise(signal)

# Mixes if required and finalises which means to convert to an maximum diviation
# from zero or + or - 1 and remove all frequencies above half nyquist.

@sf_parallel
def finalise(signal):
    return sf.Finalise(signal)