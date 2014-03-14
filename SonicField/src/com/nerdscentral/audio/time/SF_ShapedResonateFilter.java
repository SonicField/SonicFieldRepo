/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ShapedResonateFilter implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("_SF_ResonateFilter.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        try (SFSignal in = Caster.makeSFSignal(lin.get(0));)
        {
            double vResonant = Caster.makeDouble(lin.get(1));
            double vOriginal = Caster.makeDouble(lin.get(2));
            double delay = Caster.makeDouble(lin.get(3));
            try (SFSignal delayShape = Caster.makeSFSignal(lin.get(4));)
            {
                if (delayShape.getLength() != in.getLength()) throw new SFPL_RuntimeException(
                                Messages.getString("_SF_ResonateFilter.1")); //$NON-NLS-1$
                try (SFSignal out = in.replicate();)
                {
                    double r = in.getLength();
                    int delaySamples = (int) (delay * SFConstants.SAMPLE_RATE_MS);
                    for (int n = 0; n < r; ++n)
                    {
                        double q = out.getSample(n);
                        int index = (int) SFMaths.floor((n + delaySamples / delayShape.getSample(n)));
                        if (index < r)
                        {
                            out.setSample(index, out.getSample(index) * vOriginal + q * vResonant);
                        }
                    }
                    return Caster.prep4Ret(out);
                }
            }
        }

    }
}
