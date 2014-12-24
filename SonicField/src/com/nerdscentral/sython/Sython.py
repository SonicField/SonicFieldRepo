import traceback
from com.nerdscentral.audio import SFSignal

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
        trace=''.join(traceback.format_stack())
        SFSignal.setPythonStack(trace)
        ret=self.processors.get(word).Interpret(input,self.context)
        return ret