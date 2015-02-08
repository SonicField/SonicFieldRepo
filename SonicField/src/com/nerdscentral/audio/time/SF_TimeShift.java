/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_TimeShift implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        try (SFSignal in = Caster.makeSFSignal(lin.get(0)); SFSignal shift = Caster.makeSFSignal(lin.get(1)))
        {
            try (SFSignal y = in.replicateEmpty())
            {

                int length = y.getLength();
                if (shift.getLength() < length) length = shift.getLength();
                for (int index = 0; index < length; ++index)
                {
                    double pos = index + SFConstants.SAMPLE_RATE_MS * shift.getSample(index);
                    y.setSample(index, in.getSampleCubic(pos));
                }
                length = y.getLength();
                for (int index = shift.getLength(); index < length; ++length)
                {
                    y.setSample(index, in.getSample(index));
                }
                return Caster.prep4Ret(y);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_TimeShift.0"); //$NON-NLS-1$
    }

}