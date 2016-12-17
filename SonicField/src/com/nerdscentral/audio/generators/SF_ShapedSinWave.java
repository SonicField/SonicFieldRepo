/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ShapedSinWave implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ShapedSinWave.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        final SFSignal frequency = Caster.makeSFSignal(input);
        int length = frequency.getLength();
        // float[] f = new float[length];
        try (SFData data = SFData.build(length);)
        {
            final double PI2 = SFMaths.PI * 2.0d;
            final double scal = 0.1 * PI2 / SFConstants.SAMPLE_RATE;
            double pos = 0;
            // 10:1 over sample
            int llen = length * 10;
            for (int i = 0; i < llen; ++i)
            {
                int index = i / 10;
                if (i % 10 == 0) data.setSample(index, Math.sin(pos));
                pos += frequency.getSampleCubic(index) * scal;
            }
            return Caster.prep4Ret(data);
        }
    }
}