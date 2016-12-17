package com.nerdscentral.audio.core;

public abstract class SFSingleUnidirectionsTranslator extends SFSingleTranslator
{
    int    previous;
    SFData reified;

    protected SFSingleUnidirectionsTranslator(SFSignal input)
    {
        super(input);
    }

    @Override
    public double getSample(int index)
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
