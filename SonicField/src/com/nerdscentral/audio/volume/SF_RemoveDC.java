/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1 Sf.RemoveDC !s1_normal... forwards an SFData with no DC componenet ...
 * 
 * @author AlexTu
 * 
 */

public class SF_RemoveDC implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input); SFSignal data = in.replicateEmpty())
        {
            // remove DC
            double dc = 0;
            for (int i = 0; i < data.getLength(); ++i)
            {
                dc += data.getSample(i);
            }
            dc = dc / data.getLength();
            for (int i = 0; i < data.getLength(); ++i)
            {
                data.setSample(i, in.getSample(i) - dc);
            }
            return Caster.prep4Ret(data);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_RemoveDC.0");  //$NON-NLS-1$
    }

}