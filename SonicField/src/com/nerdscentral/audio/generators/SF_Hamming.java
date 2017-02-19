/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Hamming implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Hamming.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        double length = Caster.makeDouble(input);
        length *= SFConstants.SAMPLE_RATE_MS;
        length = SFMaths.ceil(length);
        SFSignal out = SFData.build((int) length);
        int Nminus1 = (int) (length - 1);
        for (int n = 0; n < length; ++n)
        {
            out.setSample(n, 0.54 - 0.46 * SFMaths.cos(2 * SFMaths.PI * n / Nminus1));
        }

        return out;
    }

}
