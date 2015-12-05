package com.nerdscentral.audio;

import com.nerdscentral.sython.Caster;

public abstract class SFSingleTranslator extends SFGenerator
{

    @Override
    public SFSignal __pos__()
    {
        signal.__pos__();
        return this;
    }

    @Override
    public SFSignal __neg__()
    {
        signal.__neg__();
        return this;
    }

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
    public void close()
    {
        signal.close();
    }

    @Override
    public void decrReferenceCount()
    {
        signal.decrReferenceCount();
    }

    @Override
    public void incrReferenceCount()
    {
        signal.incrReferenceCount();
    }

    @Override
    public void release()
    {
        // nop
    }

    @Override
    public int getReferenceCount()
    {
        return signal.getReferenceCount();
    }

}
