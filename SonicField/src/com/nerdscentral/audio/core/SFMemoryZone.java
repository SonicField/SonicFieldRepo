package com.nerdscentral.audio.core;

import java.util.ArrayList;

public class SFMemoryZone
{

    public final ArrayList<SFData> localData = new ArrayList<>();

    public void __enter__()
    {
        SFData.pushZone(this);
    }

    /**
     * @param t
     * @param v
     * @param tr
     */
    public void __exit__(Object t, Object v, Object tr)
    {
        // PASS
        try
        {
            if (this != SFData.popZone())
            {
                throw new RuntimeException(Messages.getString("SFMemoryZone.0")); //$NON-NLS-1$
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }
}
