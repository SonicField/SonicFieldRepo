import traceback
from com.nerdscentral.audio import SFSignal

# Set up the module load path and then clean up afterwards
from java.lang import System
import sys
sys.path.append(System.getProperty('sython.modules'))
del sys.modules['sys']
del sys.modules['java.lang']

# This si the code which actually calls Sonic Field operators
class SonicField:
    def __init__(self,procs):
        from com.nerdscentral.sython import SFPL_Context
        self.processors=procs
        self.context=SFPL_Context()

    def run(self,word,input,args):
        if len(args)!=0:
            args=list(args)
            args.insert(0,input)
            input=args
        if TRACE:
            trace=''.join(traceback.format_stack())
            SFSignal.setPythonStack(trace)
            c_log(word+'('+str(input)+')')
        ret=self.processors.get(word).Interpret(input,self.context)
        return ret