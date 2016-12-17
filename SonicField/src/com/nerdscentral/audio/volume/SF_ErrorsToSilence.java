/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ErrorsToSilence implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        SFSignal in = Caster.makeSFSignal(input);
        boolean errors = false;
        for (int i = 0; i < in.getLength(); ++i)
        {
            double d = in.getSample(i);
            if (Double.isInfinite(d) || Double.isNaN(d))
            {
                System.out.println(Messages.getString("SF_ErrorsToSilence.0")); //$NON-NLS-1$
                errors = true;
                break;
            }
        }
        if (errors)
        {
            in.decrReferenceCount();
            return SFData.build(in.getLength());
        }
        return in;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_ErrorsToSilence.1"); //$NON-NLS-1$
    }

}