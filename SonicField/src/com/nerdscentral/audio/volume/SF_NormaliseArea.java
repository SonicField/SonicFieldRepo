/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1 Sf.Normalise !s1_normal... forwards an SFData normalised ...
 * 
 * @author AlexTu
 * 
 */

public class SF_NormaliseArea implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input); SFSignal data = Caster.makeSFSignal(input).replicateEmpty();)
        {

            // remove DC
            double dc = 0;
            int len = data.getLength();
            for (int i = 0; i < len; ++i)
            {
                dc += in.getSample(i);
            }
            dc = dc / len;
            for (int i = 0; i < len; ++i)
            {
                data.setSample(i, in.getSample(i) - dc);
            }
            double totalExcersion = 0;
            for (int i = 0; i < len; ++i)
            {
                totalExcersion += SFMaths.abs(data.getSample(i));
            }
            for (int i = 0; i < len; ++i)
            {
                data.setSample(i, data.getSample(i) / totalExcersion);
            }
            return Caster.prep4Ret(data);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_NormaliseArea.0"); //$NON-NLS-1$
    }

}