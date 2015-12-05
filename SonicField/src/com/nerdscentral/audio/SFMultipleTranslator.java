package com.nerdscentral.audio;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.sython.Caster;

public abstract class SFMultipleTranslator extends SFGenerator
{
    @Override
    public void release()
    {
        // no-op
    }

    @Override
    public void close() throws RuntimeException
    {
        for (SFSignal x : signals)
        {
            x.close();
        }
    }

    @Override
    public void incrReferenceCount() throws RuntimeException
    {
        for (SFSignal x : signals)
        {
            x.incrReferenceCount();
        }
    }

    @Override
    public SFSignal __pos__()
    {
        for (SFSignal x : signals)
        {
            x.__pos__();
        }
        return this;
    }

    @Override
    public SFSignal __neg__()
    {
        for (SFSignal x : signals)
        {
            x.__neg__();
        }
        return this;
    }

    @Override
    public void decrReferenceCount()
    {
        this.__neg__();
    }

    private final List<SFSignal> signals;
    private final int            length;

    protected int getNMembers()
    {
        return signals.size();
    }

    protected double getInputSample(int memberIndex, int index)
    {
        return getInputSample(memberIndex, index, 0);
    }

    @SuppressWarnings("resource")
    protected double getInputSample(int memberIndex, int index, double otherwise)
    {
        // Resource managed at class level
        SFSignal ret = signals.get(memberIndex);
        if (index < 0) return otherwise;
        if (index >= ret.getLength()) return otherwise;
        return ret.getSample(index);
    }

    protected SFMultipleTranslator(List<SFSignal> input)
    {
        int len = 0;
        signals = new ArrayList<>();
        for (SFSignal signal : input)
        {
            signals.add(Caster.incrReference(signal));
            int l1 = signal.getLength();
            if (l1 > len) len = l1;
        }
        length = len;
    }

    @Override
    public abstract double getSample(int index);

    @Override
    public int getLength()
    {
        return length;
    }

}
