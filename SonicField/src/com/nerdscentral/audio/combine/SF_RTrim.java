/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_RTrim implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        /*
         * Trims silence (actually noise < NOISE_FLOOR) off the right (trailing end) of a signal.
         */
        SFSignal dataIn = Caster.makeSFSignal(input);
        int start = 0;
        int len = dataIn.getLength();
        int end = len - 1;
        for (; end > start; --end)
        {
            if (SFMaths.abs(dataIn.getSample(end)) > SFConstants.NOISE_FLOOR) break;
        }
        ++end;
        SFSignal out = SFData.build(end - start);
        for (int i = start; i < end; ++i)
        {
            out.setSample(i - start, dataIn.getSample(i));
        }
        return out;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_RTrim.0"); //$NON-NLS-1$
    }

}