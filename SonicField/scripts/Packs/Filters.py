#################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
#################################################################################

# Synthon accessors to help use Sonic Field filters

from Types import is_signal

def byquad_filter(f_type=None,input=None,frequency=None,q=None,db_gain=None):

    def byquad_filter_high():
        pass

    def byquad_filter_low():
        pass

    def byquad_filter_high_shelf():
        pass

    def byquad_filter_low_shelf():
        pass

    def byquad_filter_notch():
        pass

    def byquad_filter_peak():
        pass

    def byquad_filter_all():
        pass
    
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
        