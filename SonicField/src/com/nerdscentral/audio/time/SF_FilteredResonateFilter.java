/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.SFInLineIIRFilter;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_FilteredResonateFilter implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("FilteredResonateFilter.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        SFSignal in = Caster.makeSFSignal(lin.get(0));
        double vResonant = Caster.makeDouble(lin.get(1));
        double vOriginal = Caster.makeDouble(lin.get(2));
        double delay = Caster.makeDouble(lin.get(3));
        SFInLineIIRFilter filterR = Caster.makeFilter(lin.get(4)).duplicate();
        SFInLineIIRFilter filterQ = filterR.duplicate();
        SFSignal out = in.replicate();
        double r = in.getLength();
        int delaySamples = (int) (delay * SFConstants.SAMPLE_RATE_MS);
        for (int index = 0; index < delaySamples; ++index)
        {
            out.setSample(index, filterR.filterSample(out.getSample(index)) * vOriginal);
        }
        for (int n = 0; n < r; ++n)
        {
            double q = out.getSample(n);
            int index = n + delaySamples;
            if (index < r)
            {
                q = filterQ.filterSample(q);
                out.setSample(index, filterR.filterSample(out.getSample(index)) * vOriginal + q * vResonant);
            }
        }
        return out;
    }
}
