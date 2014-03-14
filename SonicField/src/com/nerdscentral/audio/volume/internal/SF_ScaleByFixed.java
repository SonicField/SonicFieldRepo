/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume.internal;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ScaleByFixed implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final double      scale;

    public SF_ScaleByFixed(double scaleIn)
    {
        scale = 1.0d / scaleIn;
    }

    @Override
    public String Word()
    {
        return "scaleByFixed"; //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        int len = data.getLength();
        for (int i = 0; i < len; ++i)
        {
            data.setSample(i, data.getSample(i) * scale);
        }
        return data;
    }

}
