/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_FirstCross implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        /*
         * Zeros  the start of a signal to the point where the first crossing of zero is.
         * This helps get rid of unwanted clicks at the start of a signal and is a bit like
         * starting all the oscillators in phase at the start.
         */
        SFSignal dataIn = Caster.makeSFSignal(input);
        int start = 0;
        double first = dataIn.getSample(0);
        if (SFMaths.abs(first) == 0.0) return input;

        int len = dataIn.getLength();
        boolean phase = first > 0.0;
        for (; start < len; ++start)
        {
            boolean newPhase = dataIn.getSample(start) > 0.0;
            if (newPhase != phase) break;
        }
        SFSignal out = SFData.build(len - start);
        for (int i = 0; i < start; ++i)
        {
            out.setSample(i, 0);

        }
        for (int i = start; i < len; ++i)
        {
            out.setSample(i, dataIn.getSample(i));
        }
        return out;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_FirstCross.0"); //$NON-NLS-1$
    }

}