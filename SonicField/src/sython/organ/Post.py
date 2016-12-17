######################
# Channel By Channel 
# Mix mix/processing  
######################

from organ.Algorithms import echo_division,tremolate
from Parallel_Helpers import finalise,mix

@sf_parallel
def post_process(notes):
    count=0
    tnsl=[]
    tnsr=[]
    all_left=[]
    all_right=[]
    for note in notes:
        nlr,atl,atr=note
        c_log("Mix phase 1 done: ",count,atl,atr)
        notel,noter=nlr
        tnsl.append([notel,atl])
        tnsr.append([noter,atr])
        count+=1
    tnl=mix(tnsl)
    tnr=mix(tnsr)
    c_log("Mix phase 2 done")
    return tnl,tnr

@sf_parallel
def post_process_tremolate(notes,rate=3.5,magnitude=0.25):
    count=0
    tnsl=[]
    tnsr=[]
    for note in notes:
        nlr,atl,atr=note
        c_log("Trem phase 1 done: ",count)
        notel,noter=nlr
        tnsl.append([notel,atl])
        tnsr.append([noter,atr])
        count+=1
    tnl=mix(tnsl)
    tnr=mix(tnsr)
    tnl=tremolate(tnl,rate,magnitude)
    tnr=tremolate(tnr,rate,magnitude)
    c_log("Trem phase 3 done")
    return tnl,tnr

@sf_parallel
def post_process_echo(notes):
    count=0
    tnsl=[]
    tnsr=[]
    for note in notes:
        nlr,atl,atr=note
        c_log("Echo phase 1 done: ",count)
        notel,noter=nlr
        tnsl.append([notel,atl])
        tnsr.append([noter,atr])
        count+=1
    tnl=mix(tnsl)
    tnr=mix(tnsr)
    c_log("Echo phase 2 done")
    tnl=echo_division(tnl)
    tnr=echo_division(tnr)
    c_log("Echo phase 3 done")
    return tnl,tnr

@sf_parallel
def do_final_mix(all_left,all_right):
    left =finalise(all_left)
    right=finalise(all_right)
    left  = sf.Cut(1,sf.Length(+left ),left)
    right = sf.Cut(1,sf.Length(+right),right)
    return left,right
