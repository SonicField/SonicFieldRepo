import traceback
import inspect
 
from com.nerdscentral.audio.core import SFSignal
from com.nerdscentral.audio.core import SFConstants

# Set up the module load path and then clean up afterwards
from java.lang import System
import sys
sys.path.append(System.getProperty('sython.modules'))
del sys.modules['sys']
del sys.modules['java.lang']

# This is the code which actually calls Sonic Field operators
class SonicField:
    def __init__(self,procs):
        self.processors=procs

    def run(self, word, input,args):
        if len(args) != 0:
            args = list(args)
            args.insert(0, input)
            input = args
        if SFConstants.TRACE:
            trace = ''.join(traceback.format_stack(inspect.currentframe().f_back, 20))
            SFSignal.setPythonStack(trace)
        ret=self.processors.get(word).Interpret(input)
        return ret