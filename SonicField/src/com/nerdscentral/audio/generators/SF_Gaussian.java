/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Gaussian implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Gaussian.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        double length = Caster.makeDouble(input);
        // Length is actually 2*4sigma
        length *= SFConstants.SAMPLE_RATE_MS;
        double sigma = length;
        length *= 8;
        SFSignal out = SFData.build((int) SFMaths.ceil(length));
        double center = length / 2;
        for (int i = 0; i < length; ++i)
        {
            double q = SFMaths.pow(SFMaths.E, -1d * SFMaths.pow(i - center, 2) / (2d * SFMaths.pow(sigma, 2)));
            out.setSample(i, q);
        }
        return out;
    }

}
