package com.nerdscentral.audio;

public abstract class SFSimpleGenerator extends SFGenerator
{
    private final int length;

    protected SFSimpleGenerator(int len)
    {
        length = len;
    }

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
