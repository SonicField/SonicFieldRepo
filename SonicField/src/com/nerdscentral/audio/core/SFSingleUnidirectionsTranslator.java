package com.nerdscentral.audio.core;

public abstract class SFSingleUnidirectionsTranslator extends SFSingleTranslator
{
    int    previous;
    SFSignal reified;

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
