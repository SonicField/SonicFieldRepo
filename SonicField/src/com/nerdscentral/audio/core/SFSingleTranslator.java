package com.nerdscentral.audio.core;

import com.nerdscentral.sython.Caster;

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
        signal = Caster.incrReference(input);
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
