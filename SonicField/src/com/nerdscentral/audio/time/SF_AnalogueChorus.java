/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_AnalogueChorus implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString(Messages.getString("SF_AnalogueChorus.0")); //$NON-NLS-1$
    }

    private static double sat(double x)
    {
        double y = x >= 0 ? x / (x + 1) : x / (1 - x);
        return y;
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        try (SFSignal in = Caster.makeSFSignal(lin.get(0)); SFSignal delay = Caster.makeSFSignal(lin.get(2)))
        {
            double feedBack = Caster.makeDouble(lin.get(2));
            double satAmount = Caster.makeDouble(lin.get(3));
            try (SFSignal out = in.replicate();)
            {
                double r = in.getLength();
                for (int n = 0; n < r; ++n)
                {
                    int delaySamples = (int) (delay.getSample(n) * SFConstants.SAMPLE_RATE_MS);
                    double q = out.getSample(n);
                    q = (1.0 - satAmount) * q + satAmount * sat(q);
                    int index = n + delaySamples;
                    if (index < r)
                    {
                        out.setSample(index, q);
                    }
                }
                for (int n = 0; n < r; ++n)
                {
                    int delaySamples = (int) (delay.getSample(n) * SFConstants.SAMPLE_RATE_MS);
                    double q = out.getSample(n);
                    q = (1.0 - satAmount) * q + satAmount * sat(q);
                    int index = n + delaySamples;
                    if (index < r)
                    {
                        out.setSample(index, out.getSample(index) * feedBack + q * (1.0 - feedBack));
                    }
                }
                return Caster.prep4Ret(out);
            }
        }
    }
}
