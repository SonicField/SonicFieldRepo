/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume.internal;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_RemoveFixedDCAndSubNormals implements SFPL_Operator
{
    public SF_RemoveFixedDCAndSubNormals(double dcIn)
    {
        this.dc = dcIn;
    }

    private static final long serialVersionUID = 1L;
    private final double      dc;

    @Override
    public String Word()
    {
        return "RemoveFixedDC"; //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        int len = data.getLength();
        double ldc = dc;
        if (SFMaths.abs(dc) < Double.MIN_NORMAL) ldc = 0;
        for (int i = 0; i < len; ++i)
        {
            double d = data.getSample(i) - ldc;
            if (SFMaths.abs(d) < Double.MIN_NORMAL)
            {
                data.setSample(i, 0);
            }
            else
            {
                data.setSample(i, data.getSample(i) - dc);
            }
        }
        return data;
    }

}
