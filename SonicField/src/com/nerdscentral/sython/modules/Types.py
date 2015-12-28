#################################################################################
# For Copyright and License see LICENSE.txt and COPYING.txt in the root directory 
#################################################################################

# Methods for managing and recognising Sonic Field important types.

from com.nerdscentral.audio import SFSignal

# Returns true if the passed argument is a SFSignal 
# returns false otherwise.
def is_signal(x):
    return isinstance(x,SFSignal)

# Raises an exception if the passed argument is None
def not_none(x,arg="anonymous"):
    if x is None:
        raise Exception('None were a value required for ' + str(arg))
    return x

# If the passed argument is a SFSignal is it returned
# if it is a number then a signal of the passed length
# and of contant value of the passed number is returned
# Note that the returned signal, if a new signal, is a 
# generator so very low cost to create and use.
def ensure_signal(x,length):
    not_none(x)
    if is_singal(x):
        return x
    return sf.Constant(length,x)
    