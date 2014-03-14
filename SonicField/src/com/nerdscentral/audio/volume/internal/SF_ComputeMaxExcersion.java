/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume.internal;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ComputeMaxExcersion implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return "ComputeMaxExcersion"; //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal data = Caster.makeSFSignal(input);)
        {
            double maxExcersion = 0;
            int len = data.getLength();
            for (int i = 0; i < len; ++i)
            {
                double thisExcersion = data.getSample(i);
                if (thisExcersion < 0) thisExcersion = -thisExcersion;
                if (thisExcersion > maxExcersion) maxExcersion = thisExcersion;
            }
            return maxExcersion;
        }
    }

}
