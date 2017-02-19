/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_MaxValue implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_MaxValue.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        SFSignal signal = Caster.makeSFSignal(input);
        int length = signal.getLength();
        double ret = Double.MIN_VALUE;
        for (int index = 0; index < length; ++index)
        {
            double x = signal.getSample(index);
            if (x > ret) ret = x;
        }
        return ret;
    }
}