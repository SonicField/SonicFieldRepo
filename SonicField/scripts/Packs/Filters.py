#################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
#################################################################################

# Synthon accessors to help use Sonic Field filters

from Types import is_signal

def byquad_filter(f_type,input,frequency,q,db_gain=None):

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

    def byquad_filter_low_shelf():
        if shaped:
            raise Exception(f_type,'Not currently available as shaped')

    def byquad_filter_notch():
        pass

    def byquad_filter_peak():
        pass

    def byquad_filter_all():
        if shaped:
            raise Exception(f_type,'Not currently available as shaped')
    
    def byquad_filter_band():
        pass
    
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
        'band'       : byquad_filter_band
    }.get(f_type,not_known)
    c_log(run)
    return sf_do(run)
 
 