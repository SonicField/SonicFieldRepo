#################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
#################################################################################

# Methods for managing and recognising Sonic Field important types.

from com.nerdscentral.audio import SFSignal

# Returns true if the passed argument is a SFSignal 
# returns false otherwise.
def is_signal(x):
    return isinstance(x,SFSignal)

# If the passed argument is a SFSignal is it returned
# if it is a number then a signal of the passed length
# and of contant value of the passed number is returned
# Note that the returned signal, if a new signal, is a 
# generator so very low cost to create and use.
def ensure_signal(x,length):
    if is_singal(x):
        return x
    return sf.Constant(length,x)
    