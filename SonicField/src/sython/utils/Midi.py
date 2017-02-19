from com.nerdscentral.audio.midi import MidiFunctions
import math
import random
import copy

modern_base  = 8.1757989156
baroque_base = modern_base * 415.0/440.0

def key_from_cents(base,key,cents,offset=False):
    '''
    Get the frequency of a key given a list of cents offsets within each octave
    note this only works for midi keys so does not support true enharmonisity
    '''
    key=float(key)
    cent=2.0**(1.0/1200.0)
    octave=math.floor(key/12.0)
    pitch=base*2.0**octave
    note=int(key-octave*12)
    if offset:
        pitch*=cent**((cents[note]+note*100.0)-cents[0])
    else:
        pitch*=cent**cents[note]
    return pitch

################################################################################
#
# Note for temperaments the base parameter is the pitch of key=0.
#
################################################################################

def werckmeisterIII(key,base=baroque_base):
    '''
    Werckmeister III well termperament. This is good for baroque music; works 
    really well for Bach orgasn music. Also see bach_lehman
    '''
    #Pitch:  C   C#     D       Eb      E       F       F#      G       G#      A       A#      B      
    cents=[  0,  90.225,192.18, 294.135,390.225,498.045,588.27, 696.09, 792.18, 888.27, 996.09, 1092.18]
    return key_from_cents(base,key,cents)

def bach_lehman(key,base=baroque_base):
    '''
    Some people believe this to be the temperament used for the composition of
    the Well Tempered Clavier.
    '''
    #Pitch:  C   C#     D       Eb      E       F       F#      G       G#      A       A#      B
    cents=[5.9  ,3.9   ,2      ,3.9   ,-2      ,7.8    ,2      ,3.9    ,3.9    ,0      ,3.9    ,0]
    return key_from_cents(base,key,cents,True)

def just_intonation(key,base=modern_base):
    '''
    One transposition for just intonation onto the non enharmonic chromatic scale.
    This is only likely to work in scales close to C. It will go horribly out of 
    tune unless great care is taken.
    '''
    key=float(key)
    ratios = (
        (1,1),    #C
        (16,15),  #C+
        (9,8),    #D
        (6,5),    #D+
        (5,4),    #E
        (4,3),    #F
        (10,7),   #F+
        (3,2),    #G
        (32,21),  #G+
        (5,3),    #A
        (9,5),    #A+
        (15,8)    #B
    )
    octave=math.floor(key/12.0)
    pitch=base*2.0**octave
    note=int(key-octave*12)
    ratio=ratios[note]
    ratio=float(ratio[0])/float(ratio[1])
    pitch*=ratio
    return pitch

def equal_temperament(key,base=modern_base):
    '''True equal temperament based on the twelth root of 2.'''
    key=float(key)
    return(sf.Semitone(0)**key) * base

def unpack(tup,beat,base=modern_base):
            tickOn,tickOff,note,key,velocity = tup
            at  = tickOn*beat
            llen = (tickOff-tickOn)*beat
            if key==0:
                pitch=base
            else:
                pitch= (sf.Semitone(0)**float(key)) * base
            return tickOn,tickOff,note,key,velocity,at,llen,pitch

def _repare_overlap(midi,blip=5):
    d_log("Interpretation Pass")
    mute=True
    while mute:
        mute=False
        index=0
        midiOut=[]
        this=[]
        next=[]
        midi_full=midi
        midi=[]
        other=[]
        for event in midi_full:
            if event.isNote():
                midi.append(event.clone())
            else:
                other.append(event.clone())
        midi=sorted(midi, key=lambda event: event.tick)
        midi=sorted(midi, key=lambda event: (event.isNote() and event.key) or 0)
        endAt=len(midi)-1
        d_log("Demerge pass:",endAt)
        while index<endAt:
            this=midi[index]
            next=midi[index+1]
            ttickOn,ttickOff,tnote,tkey,tvelocity=this.getNote()
            ntickOn,ntickOff,nnote,nkey,nvelocity=next.getNote()
    
            # Merge interpretation
            finished=False
            dif=(ttickOff-ttickOn)
            if dif<blip and tkey==nkey and ttickOff>=ntickOn and ttickOff<=ntickOff:
                d_log("Separating: ",this,next," Diff: ",(ttickOff-ntickOn))
                midiOut.append(this)
                midiOut.append(next)
                index+=1
                mute=True     
            elif  dif<blip:
                d_log("Removing blip: ",(ttickOff-ttickOn))
                index+=1
                mute=True     
                continue
            else:
                midiOut.append(this)
            # iterate the loop
            index+=1
        if index==endAt:
            midiOut.append(next)
        midiOut+=other
        midiOut=sorted(midiOut, key=lambda event: event.tick)
        if not mute:
            return midiOut
        midi=midiOut

def repare_overlap(midis,blip=5):
    midisOut=[]
    for midi in midis:
        midisOut.append(_repare_overlap(midi,blip))
    return midisOut

def delay(midi,beat,millis):
    d_log("Interpretation Pass")
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    for event in midi:
        event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        event=event=event.clone()
        event.tick+=millis
        event.tick_off+=millies
        midiOut.append(event)
    return midiOut

def fix_velocity(midi,v=100):
    d_log("Interpretation Pass")
    midiOut=[]
    for event in midi:
        event=event.clone()
        if not is_midi_note(event):
            midiOut.append(event)
            continue
        event.velocity=v
        midiOut.append(event)
    return midiOut

def damp_velocity(midi,key=80,amount=0.75):
    d_log("Interpretation Pass")
    midiOut=[]
    for event in midi:
        event=event.clone()
        if not event.isNode():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        if tkey<key:
            v=tvelocity
        else:
            print "Damping: ",midi[index]
            v=tvelocity*amount
        evevnt.velocity=v
    return midiOut

def min_length(midi,beat,millis):
    d_log("Interpretation Pass")
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    for event in midi:
        event=event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        l=float(ttickOff-ttickOn)
        if l<millis:
            event.tick_off=event.tick+millis
        midiOut.append(event)
        # iterate the loop
    return midiOut

def legato(midi,beat,millis):
    d_log("Interpretation Pass")
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    for event in midi:
        event=event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        l=float(ttickOff-ttickOn)
        if l<millis:
            event.tick_off=event.tick_off+l*0.5
        else:
            event.tick_off=event.tick_off+millis
        midiOut.append(event)
    return midiOut

def staccato(midi,beat,millis):
    d_log("Interpretation Pass")
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    for event in midi:
        event=event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        l=flost(ttickOff-ttickOn)
        if l*2.0<millis:
            event.tick_off=event.tick+l*0.5
        else:
            event.tick_off=event.tick_off-millis
        midiOut.append(event)
    return midiOut

def long_as(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    midiOut=[]
    millis=float(millis)
    for event in midi:
        event=event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        l=ttickOff-ttickOn
        l*=beat
        if l>=millis:
            midiOut.append(event)
        else:
            print "Skipping ",l
    return midiOut

def shorter_than(midi,beat,millis):
    d_log("Interpretation Pass")
    midiOut=[]
    millis=float(millis)
    for event in midi:
        event=event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        ttickOn,ttickOff,tnote,tkey,tvelocity=event.getNote()
        l=(ttickOff-ttickOn)*beat
        if l<millis:
            midiOut.append(event)
        else:
            print "Skipping ",l
    return midiOut

def scatter(midi, beat, millis_in):
    d_log("Interpretation Pass")
    midiOut=[]
    for event in midi:
        event=event.clone()
        if not event.isNote():
            midiOut.append(event)
            continue
        millis=float(millis_in)
        millis/=float(beat)
        millis=millis*random.random()
        event.tick+=millis
        event.tick_off+=millis
        midiOut.append(event)
    return midiOut

# To Do - Fix For Velocity Changes
def find_length(midi,beat):
    d_log("Interpretation Pass")
    endAt=len(midi)
    ttickOn,ttickOff,tnote,tkey,tvelocity=midi[-1]
    print "End of last note is: ", (ttickOff*beat)/60000.0
    sys.exit(0)

def set_length(midis,length,beat=4.0):
    length=float(length)
    beat=float(beat)
    max=0.0
    for midi in midis:
        endAt=len(midi)
        if endAt==0:
            continue
        index=0
        midiOut=[]
        lNow=float((midi[-1].tick_off*beat)/60000.0)
        if lNow>max:
            max=lNow
    return beat*length/max

def clone(midis):
    rets=[]
    for midi in midis:
        ret=[]
        for event in midi:
            ret+=[event.clone()]
        midis+=[ret]
    return rets

def tempo(midis):
    tempos=[]
    for midi in midis:
        for event in midi:
            if event.isMeta() and event.type=='tempo':
                event=event.clone()
                tempos.append(event)
    tempos=sorted(tempos, key=lambda event: event.tick)
    if not tempos:
        return clone(midis)
    if not tempos[0].tick==0.0:
        raise NotImplementedError('Initial temp marker must be at zero in this implementation')

    outs=[]
    initial=tempos[0].data
    tmplu={}
    for t in tempos:
        tmplu[t.tick]=t.data
    for midi in midis:
        ticks={}
        last=0.0
        out=[]
        for event in midi:
            event.clone()
            on=int(event.tick)
            if not on in ticks:
                ticks[on]=[event]
            else:
                ticks[on].append(event)
            off=int(event.tick_off)
            if not off in ticks:
                ticks[off]=[event]
            else:
                lst=ticks[off]
                if not event in lst:
                    ticks[off].append(event)
            if off>last:
                last=off
            out.append(event)
        outs.append(out)

    # As we store all events to be changed in a dictionary
    # we can process them all at once for all midis and so
    # only run the midi player emulator once
    rate=1.0
    at=0.0
    # Bruit force mimic a midi reading state machine
    # TODO make this more efficient by jumping over the
    # iterations which we do not require
    for tick in range(0,last+1):
        if tick in tmplu:
            rate=tmplu[tick]/initial
        if tick in ticks:
            for event in ticks[tick]:
                if event.tick==tick:
                    event.tick=at
                if event.tick_off==tick:
                    event.tick_off=at
        at+=rate
    return outs
        
class MidiEvent(object):
    def __init__(self,event):
        for k,v in event.iteritems():
            setattr(self,k,v)

    def isNote(self):
        return self.command=='note'

    def isMeta(self):
        return self.command=='meta'

    def isSysex(self):
        return self.command=='sysex'

    def isControlChange(self):
        return self.command=='command' and self.type=='control_change'
    
    def getNote(self):
        if not self.isNote():
            raise ValueError('Event is not a note:'+self.command)
        return (
            self.tick,
            self.tick_off,
            None,            # Historical 
            self.key,
            self.velocity
        )

    def clone(self):
        '''
        Return a depy copy of self.
        This is useful for creating adjusted versions of midid events 
        whilst leaving the original events unchanged.
        '''
        return copy.deepcopy(self)

    def pprint(self):
        return str(self.__dict__)
    
        atoms=[]
        for lhs in dir(self):
            vl=getattr(self,lhs)
            rhs=None
            if not hasattr(vl,'__call__'):
                if hasattr(vl,'__abs__'):
                    # Number
                    rhs=str(vl)
                else:
                    rhs="'{0}'".format(vl)
                atoms+=["'{0}:{1}".format(lhs,rhs)]
        return '{'+','.join(atoms)+'}'

def pprint(midi):
    for event in midi:
        print event.pprint()
                       
def read_midi_file(name):
    midis=MidiFunctions.readMidiFile(name)
    midis=MidiFunctions.processSequence(midis)
    note_midis=[]
    for midi in midis:
        notes=[]
        for e in midi:
            command=e['command']
            if command=='meta':
                dd=0
                # Convert the data into an integer - this is probably OK for now in all
                # cases we are likely to use (mainly tempo)
                for d in e['data']:
                    if d < 0:
                        d+=256
                    dd*=256
                    dd+=d
                    try:
                        e['data']=float(dd)
                    except OverflowError as er:
                        print 'Over flow error: %s -> %s' % (str(er), str(dd))
                    e['data'] = 0

            if not command=='note':
                e['tick_off']=e['tick']

            notes.append(MidiEvent(e))
        note_midis+=[notes]
    return note_midis

def controllerEnvelope(midi,controller,beat,signal):
    midiOut=[]
    env=[[0,0]]
    for event in midi:
        if event.isControlChange():
            node=[event.tick*beat,float(event.amount)/127.0]
            env+=[node]
    # unique and sort
    env=sorted(env,key=lambda x: x[0])
    envb=[[0.0,0.0]]
    old=[0.0,0.0]
    for e in env:
        if old[0]!=e[0] and old[1]!=e[1]:
            if e[0]-10>old[0]:
                envb.append([e[0]-10,old[1]])
            envb.append(e)
        old=e
    l=sf.Length(signal)
    if envb[-1][0]<l:
        envb+=[ [ l,env[-1][1] ] ]
    return sf.NumericShape(envb)  
