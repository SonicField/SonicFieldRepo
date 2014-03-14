class SonicField:
    def __init__(self,procs):
        from com.nerdscentral.sython import SFPL_Context
        self.processors=procs
        self.context=SFPL_Context()

    # Process all incoming args scanning for sf data
    # if there is one ensure that its count is reduced by one
    # before it is released or raise an error apart from if it implements pass through
    def run(self,word,input,args):
        if len(args)!=0:
            args=list(args)
            args.insert(0,input)
            input=args
        ret=self.processors.get( word).Interpret(input,self.context)
        return ret