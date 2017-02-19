/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume.internal;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.volume.Messages;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ComputeDC implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return "ComputeDC"; //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        double dc = 0;
        int len = data.getLength();
        for (int i = 0; i < len; ++i)
        {
            double d = data.getSample(i);
            if (Double.isInfinite(d)) throw new SFPL_RuntimeException(Messages.getString("SF_Normalise.0")); //$NON-NLS-1$
            if (Double.isNaN(d)) throw new SFPL_RuntimeException(Messages.getString("SF_Normalise.1")); //$NON-NLS-1$
            dc += d;
        }
        return dc;
    }
}
