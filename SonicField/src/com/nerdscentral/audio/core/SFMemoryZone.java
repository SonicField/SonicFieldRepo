package com.nerdscentral.audio.core;

import java.util.ArrayList;

public class SFMemoryZone
{

    public ArrayList<SFData> localData = new ArrayList<>();

    public int size()
    {
        return localData.size();
    }

    public void __enter__()
    {
        // PASS
        SFData.pushZone(this);
    }

    public void __exit__(Object t, Object v, Object tr)
    {
        // PASS
        if (this != SFData.popZone())
        {
            throw new RuntimeException("Memory zone missmatch");
        }
    }
}
