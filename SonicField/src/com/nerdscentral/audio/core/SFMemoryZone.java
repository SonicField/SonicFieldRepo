package com.nerdscentral.audio.core;

import java.util.ArrayList;

public class SFMemoryZone
{

    public final ArrayList<SFData> localData = new ArrayList<>();

    public void __enter__()
    {
        SFData.pushZone(this);
    }

    public void __exit__(Object t, Object v, Object tr)
    {
        // PASS
        try
        {
            if (this != SFData.popZone())
            {
                throw new RuntimeException("Memory zone missmatch");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }
}
