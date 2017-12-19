package com.nerdscentral.audio.core;

public abstract class SFSingleTranslator extends SFGenerator
{

    private final SFSignal signal;
    private final int      length;

    protected double getInputSample(int index)
    {
        return signal.getSample(index);
    }

    protected SFSingleTranslator(SFSignal input)
    {
        signal = input;
        length = input.getLength();
    }

    @Override
    public abstract double getSample(int index);

    @Override
    public int getLength()
    {
        return length;
    }

    @Override
    public void release()
    {
        // nop
    }

}
