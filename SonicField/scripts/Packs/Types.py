#################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
#################################################################################

# Methods for managing and recognising Sonic Field important types.

from com.nerdscentral.audio import SFSignal

def is_signal(x):
    return isinstance(x,SFSignal)
    
