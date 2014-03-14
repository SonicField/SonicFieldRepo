/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_MaxDeviation implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (SFSignal sampleA = Caster.makeSFSignal(l.get(0)))
        {
            double max = Caster.makeDouble(l.get(1));
            int len = sampleA.getLength();
            try (SFSignal ret = sampleA.replicateEmpty())
            {
                double value = 0;
                for (int i = 0; i < len; ++i)
                {
                    double next = sampleA.getSample(i);
                    double diff = next - value;
                    if (SFMaths.abs(diff) > max)
                    {
                        diff = diff > 0 ? max : -max;
                    }
                    value += diff;
                    ret.setSample(i, value);
                }
                return Caster.prep4Ret(ret);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_MaxDeviation.0"); //$NON-NLS-1$
    }

}