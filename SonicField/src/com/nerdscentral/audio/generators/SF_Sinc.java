/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Sinc implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Sinc.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        double length = Caster.makeDouble(lin.get(0));
        double quality = Caster.makeDouble(lin.get(1));
        // Length is actually 2*4sigma
        double frequency = 1000d / length;
        length *= SFConstants.SAMPLE_RATE_MS;
        length *= quality;
        // float d[] = new float[(int) SFMaths.ceil(length)];
        SFSignal data = SFData.build((int) SFMaths.ceil(length));
        int center = (int) (length / 2);
        final double PI2 = SFMaths.PI * 2.0d;
        final double window = 0.125;

        for (int i = 0; i < length; ++i)
        {
            double x = i - center;
            if (x == 0)
            {
                data.setSample(i, data.getSample(i - 1));
            }
            else
            {
                data.setSample(i, Math.sin(x * PI2 * frequency / SFConstants.SAMPLE_RATE) * SFConstants.SAMPLE_RATE / x);
            }
            if (i < length * window)
            {
                data.setSample(i, data.getSample(i) * i / (length * window));
            }
            if (i > length * (1 - window))
            {
                data.setSample(i, data.getSample(i) * (length - i) / (length * window));
            }
        }

        return data;
    }
}
