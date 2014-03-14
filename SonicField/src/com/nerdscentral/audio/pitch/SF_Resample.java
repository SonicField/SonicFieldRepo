/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Resample implements SFPL_Operator
{

    /**
     * this takes a shape and for each sample step forward it add the value of the shape to the step and takes a linear
     * interpolation.
     * 
     * The natural step rate T0 is 1. A shape value SN where SN>0 will be added to the natural. Therefore SN of 1 will give
     * twice the frequency. If SN<0 we subtract from T0 from it and take the reciprocal. Thus a SN of -1 will give 1/2. Thus SN
     * - 1 will halve the sample rate.
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (SFSignal shape = Caster.makeSFSignal(l.get(0)); SFSignal sampleA = Caster.makeSFSignal(l.get(1)))
        {
            if (sampleA.getLength() != shape.getLength()) throw new SFPL_RuntimeException(Messages.getString("SF_Resample.1"));  //$NON-NLS-1$
            int len = sampleA.getLength();
            try (SFSignal ret = sampleA.replicateEmpty())
            {
                double pos = 0;
                for (int i = 0; i < len; ++i)
                {
                    ret.setSample(i, sampleA.getSampleCubic(pos));
                    double sn = shape.getSample(i);
                    pos += sn;
                }
                return Caster.prep4Ret(ret);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Resample.2");  //$NON-NLS-1$
    }

}