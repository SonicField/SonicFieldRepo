#################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
#################################################################################

# Synthon accessors to help use Sonic Field filters
#
# Note that this processor is already parallelised
#
# Filter Types
#        'high'       : byquad filter high
#        'low'        : byquad filter low
#        'high shelf' : byquad filter high shelf
#        'low shelf'  : byquad filter low shelf
#        'notch'      : byquad filter notch
#        'peak'       : byquad filter peak
#        'all'        : byquad filter all
#        'band'       : byquad filter band
#        'ladder'     : ladder filter
#
# If signals are passed into any of frequency, q and/or db_gain then
# shaped filters will be used if availible.

from Types import is_signal

def byquad_filter(f_type,input,frequency,q=1.0,db_gain=6.0):

    def byquad_filter_high():
        if shaped:
            return sf.ShapedRBJHighPass(
                input,
                ensure_signal(frequency),
                ensure_signal(q)
            )
        else:
            return sf.RBJHighPass(
                input,
                frequency,
                q
            )

    def byquad_filter_low():
        if shaped:
            return sf.ShapedRBJLowPass(
                input,
                ensure_signal(frequency),
                ensure_signal(q)
            )
        else:
            return sf.RBJLowPass(
                input,
                frequency,
                q
            )

    def byquad_filter_high_shelf():
        if shaped:
            raise Exception(f_type,'Not currently available as shaped')
        
        return sf.RBJHighShelf(
            input,
            frequency,
            q,
            db_gain
        )
        
    def byquad_filter_low_shelf():
        if shaped:
            raise Exception(f_type,'Not currently available as shaped')

        return sf.RBJLowShelf(
            input,
            frequency,
            q,
            db_gain
        )

    def byquad_filter_notch():
        if shaped:
            return sf.ShapedRBJNotch(
                input,
                ensure_signal(frequency),
                ensure_signal(q)
            )
        else:
            return sf.RBJNotch(
                input,
                frequency,
                q
            )

    def byquad_filter_peak():
        if shaped:
            return sf.ShapedRBJPeaking(
                input,
                ensure_signal(frequency),
                ensure_signal(q),
                ensure_signal(db_gain)
            )
        else:
            return sf.RBJPeaking(
                input,
                frequency,
                q,
                db_gain
            )

    def byquad_filter_all():
        if shaped:
            raise Exception(f_type,'Not currently available as shaped')
         
        return sf.RBJAllPass(
            input,
            frequency,
            q
        )
    
    def byquad_filter_band():
        if shaped:
            return sf.ShapedRBJBandPass(
                input,
                ensure_signal(frequency),
                ensure_signal(q)
            )
        else:            
            return sf.RBJBandPass(
                input,
                frequency,
                q
            )

    def ladder_filter():
        # if non of the inputs of signals no matter
        # this will make a static filter from a shaped filter
        return sf.ShapedLadderLowPass(
            input,
            ensure_signal(frequency),
            ensure_signal(q)
        )

    def not_known():
        c_log("Here - about to raise")
        raise Exception('No such filter',f_type)

    shaped=is_signal(frequency) or is_signal(q)

    run={
        'high'       : byquad_filter_high,
        'low'        : byquad_filter_low,
        'high_shelf' : byquad_filter_high_shelf,
        'low_shelf'  : byquad_filter_low_shelf,
        'notch'      : byquad_filter_notch,
        'peak'       : byquad_filter_peak,
        'all'        : byquad_filter_all,
        'band'       : byquad_filter_band,
        'ladder'     : ladder_filter
    }.get(f_type,not_known)
    c_log(run)
    return sf_do(run)
 
 