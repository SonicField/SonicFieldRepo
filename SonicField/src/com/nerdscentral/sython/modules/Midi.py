from com.nerdscentral.audio.midi import MidiFunctions
import math
import random

modern_base  = 8.1757989156
baroque_base = modern_base * 415.0/440.0

################################################################################
#
# Get the frequency of a key given a list of cents offsets within each octave
# note this only works for midi keys so does not support true enharmonisity
#
################################################################################
def key_from_cents(base,key,cents,offset=False):
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

################################################################################
#
# Werckmeister III well termperament. This is good for baroque music; works 
# really well for Bach orgasn music. Also see bach_lehman
#
################################################################################
def werckmeisterIII(key,base=baroque_base):
    #Pitch:  C   C#     D       Eb      E       F       F#      G       G#      A       A#      B      
    cents=[  0,  90.225,192.18, 294.135,390.225,498.045,588.27, 696.09, 792.18, 888.27, 996.09, 1092.18]
    return key_from_cents(base,key,cents)

################################################################################
#
# Some people believe this to be the temperament used for the composition of
# the Well Tempered Clavier.
#
################################################################################
def bach_lehman(keybase=baroque_base):
    #Pitch:  C   C#     D       Eb      E       F       F#      G       G#      A       A#      B
    cents=[5.9  ,3.9   ,2      ,3.9   ,-2      ,7.8    ,2      ,3.9    ,3.9    ,0      ,3.9    ,0]
    return key_from_cents(key,cents,True)

################################################################################
#
# One transposition for just intonation onto the non enharmonic chromatic scale.
# This is only likely to work in scales close to C. It will go horribly out of 
# tune unless great care is taken.
#
################################################################################
def just_intonation(key,base=modern_base):
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

################################################################################
#
# True equal temperament based on the twelth root of 2.
#
################################################################################
def equal_temperament(key,base=modern_base):
    key=float(key)
    return(sf.Semitone(0)**key) * base

def unpack_midi(tup,beat,base=modern_base):
            tickOn,tickOff,note,key,velocity = tup
            at  = tickOn*beat
            llen = (tickOff-tickOn)*beat
            if key==0:
                pitch=base
            else:
                pitch= (sf.Semitone(0)**float(key)) * base
            return tickOn,tickOff,note,key,velocity,at,llen,pitch
    
def interpret_midi_bombast(midi,beat):
    change=True
    while change:
        midi=sorted(midi, key=lambda tup: tup[0])
        midi=sorted(midi, key=lambda tup: tup[3])
        change=False
        d_log("Interpretation Pass")
        endAt=len(midi)-1
        index=0
        midiOut=[]
        while index<endAt:
            this=midi[index]
            next=midi[index+1]
            ttickOn,ttickOff,tnote,tkey,tvelocity,tAt,tLen,tPitch = unpack_midi(this,beat)
            ntickOn,ntickOff,nnote,nkey,nvelocity,nAt,nLen,nPitch = unpack_midi(next,beat)
    
            # Merge interpretation
            finished=False
            if tLen>512:
                if tPitch==nPitch and tvelocity==nvelocity and (ntickOn-ttickOff)<128 and (ntickOn-ttickOff)>-16:
                    # go for a low pitch merge
                    # below middle C
                    if nLen>256 and tPitch<256:
                        finished=True
                        midiOut.append([ttickOn,ntickOff,tnote,tkey,tvelocity])
                        d_log("Merging: ",this," & ",next)
                        index+=1
                        change=True
    
            if not finished:
                midiOut.append(this)
                if index==endAt-1:
                    midiOut.append(next)
                
            # iterate the loop
            index+=1
        midi=midiOut
    return midi

def repare_overlap_midi(midi,blip=5):
    d_log("Interpretation Pass")
    mute=True
    while mute:
        endAt=len(midi)-1
        mute=False
        index=0
        midiOut=[]
        this=[]
        next=[]
        d_log("Demerge pass:",endAt)
        midi=sorted(midi, key=lambda tup: tup[0])
        midi=sorted(midi, key=lambda tup: tup[3])
        while index<endAt:
            this=midi[index]
            next=midi[index+1]
            ttickOn,ttickOff,tnote,tkey,tvelocity=this
            ntickOn,ntickOff,nnote,nkey,nvelocity=next
    
            # Merge interpretation
            finished=False
            dif=(ttickOff-ttickOn)
            if dif<blip and tkey==nkey and ttickOff>=ntickOn and ttickOff<=ntickOff:
                d_log("Separating: ",this,next," Diff: ",(ttickOff-ntickOn))
                midiOut.append([ttickOn ,ntickOn ,tnote,tkey,tvelocity])
                midiOut.append([ttickOff,ntickOff,nnote,nkey,nvelocity])
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
        midiOut=sorted(midiOut, key=lambda tup: tup[0])
        if not mute:
            return midiOut
        midi=midiOut

def repare_overlap_midis(midis,blip=5):
    midisOut=[]
    for midi in midis:
        midisOut.append(repare_overlap_midi(midi,blip))
    return midisOut

def interpret_mid_istaccato(midi,beat):
    change=True
    while change:
        midi=sorted(midi, key=lambda tup: tup[0])
        midi=sorted(midi, key=lambda tup: tup[3])
        change=False
        d_log("Interpretation Pass")
        endAt=len(midi)-1
        index=0
        midiOut=[]
        while index<endAt:
            this=midi[index]
            next=midi[index+1]
            ttickOn,ttickOff,tnote,tkey,tvelocity,tAt,tLen,tPitch = unpack_midi(this,beat)
            ntickOn,ntickOff,nnote,nkey,nvelocity,nAt,nLen,nPitch = unpack_midi(next,beat)
    
            # Merge interpretation
            finished=False
            if tLen>512:
                if tPitch==nPitch and tvelocity==nvelocity and (ntickOn-ttickOff)<128 and (ntickOn-ttickOff)>-16:
                    # go for a low pitch merge
                    # below middle C
                    if nLen>256 and tPitch<256:
                        finished=True
                        midiOut.append([ttickOn,ntickOff,tnote,tkey,tvelocity])
                        d_log("Merging: ",this," & ",next)
                        index+=1
                        change=True
    
            if not finished:
                midiOut.append(this)
                if index==endAt-1:
                    midiOut.append(next)
                
            # iterate the loop
            index+=1
        midi=midiOut
    return midi

def delay_midi(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        midiOut.append((ttickOn+millis,ttickOff+millis,tnote,tkey,tvelocity))
        # iterate the loop
        index+=1
    return midiOut

def fix_velocity_midi(midi,v=100):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        midiOut.append((ttickOn,ttickOff,tnote,tkey,v))
        index+=1
    return midiOut

def damp_velocity(midi,key=80,amount=0.75):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        if tkey<key:
            v=tvelocity
        else:
            print "Damping: ",midi[index]
            v=tvelocity*amount
        midiOut.append((ttickOn,ttickOff,tnote,tkey,v))
        index+=1
    return midiOut

def min_length_midi(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        l=float(ttickOff-ttickOn)
        if l<millis:
            midiOut.append([ttickOn,ttickOn+millis,tnote,tkey,tvelocity])
        else:
            midiOut.append([ttickOn,ttickOn+millis,tnote,tkey,tvelocity])
        # iterate the loop
        index+=1
    return midiOut

def legato_midi(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        l=float(ttickOff-ttickOn)
        if l<millis:
            midiOut.append([ttickOn,ttickOff+l*0.5,tnote,tkey,tvelocity])
        else:
            midiOut.append([ttickOn,ttickOff+millis,tnote,tkey,tvelocity])
        # iterate the loop
        index+=1
    return midiOut

def staccato_midi(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    millis=float(millis)
    millis/=float(beat)
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        l=flost(ttickOff-ttickOn)
        if l*2.0<millis:
            midiOut.append([ttickOn,ttickOn+l*0.5,tnote,tkey,tvelocity])
        else:
            midiOut.append([ttickOn,ttickOff-millis,tnote,tkey,tvelocity])
        # iterate the loop
        index+=1
    return midiOut

def long_as_midi(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    millis=float(millis)
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        l=ttickOff-ttickOn
        l*=beat
        if l>=millis:
            midiOut.append([ttickOn,ttickOff,tnote,tkey,tvelocity])
        else:
            print "Skipping ",l
        index+=1
    return midiOut

def shorter_than_midi(midi,beat,millis):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    millis=float(millis)
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        l=(ttickOff-ttickOn)*beat
        if l<millis:
            midiOut.append([ttickOn,ttickOff,tnote,tkey,tvelocity])
        else:
            print "Skipping ",l
        index+=1
    return midiOut

def scatter_midi(midi,beat,millis_in):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    while index<endAt:
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[index]
        millis=float(millis_in)
        millis/=float(beat)
        millis=millis*random.random()
        midiOut.append([ttickOn+millis,ttickOff+millis,tnote,tkey,tvelocity])
        index+=1
    return midiOut

def find_length_midi(midi,beat):
    d_log("Interpretation Pass")
    endAt=len(midi)
    index=0
    midiOut=[]
    ttickOn,ttickOff,tnote,tkey,tvelocity=midi[len(midi)-1]
    print "End of last note is: ", (ttickOff*beat)/60000.0
    sys.exit(0)

def set_length_midi(midis,length,beat=4.0):
    length=float(length)
    beat=float(beat)
    max=0.0
    for midi in midis:
        if not len(midi):
            continue
        endAt=len(midi)
        index=0
        midiOut=[]
        ttickOn,ttickOff,tnote,tkey,tvelocity=midi[len(midi)-1]
        lNow=float((ttickOff*beat)/60000.0)
        if lNow>max:
            max=lNow
    return beat*length/max
    
def read_midi_file(name):
    midis=MidiFunctions.readMidiFile(name)
    midis=MidiFunctions.processSequence(midis)
    note_midis=[]
    for midi in midis:
        notes=[]
        note_midis.append(notes)
        for e in midi:
            if e['command']=='note':
                notes.append((e['tick'],e['tick-off'],'unknown',e['key'],e['velocity']))
    return note_midis
    