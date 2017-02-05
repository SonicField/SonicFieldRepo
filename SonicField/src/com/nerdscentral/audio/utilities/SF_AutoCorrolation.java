/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_AutoCorrolation implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_AutoCorrolation.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        SFSignal orig = Caster.makeSFSignal(input);
        int len = orig.getLength();
        int window = len / 2;
        SFSignal r = SFData.build(window);
        double sum;
        for (int i = 0; i < window; i++)
        {
            sum = 0;
            for (int j = 0; j < window - i; j++)
            {
                sum += orig.getSample(j) * orig.getSample(j + i);
            }
            r.setSample(i, sum);
        }
        return r;
    }
}