/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSimpleGenerator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_PhasedSin implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    public static class Generator extends SFSimpleGenerator
    {

        final double        frequency;
        final double        phase;
        final static double PI2 = SFMaths.PI * 2.0d;

        protected Generator(int len, double frequ, double ph)
        {
            super(len);
            frequency = frequ;
            phase = ph;
        }

        @Override
        public double getSample(int index)
        {
            return SFMaths.sin(index * PI2 * frequency / SFConstants.SAMPLE_RATE + phase);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_PhasedSin.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        final List<Object> l = Caster.makeBunch(input);
        final double frequency = Caster.makeDouble(l.get(1));
        final double duration = (Caster.makeDouble(l.get(0))) / 1000.0d;
        double phase = (Caster.makeDouble(l.get(2)));
        final int size = (int) (duration * SFConstants.SAMPLE_RATE);
        final double PI2 = SFMaths.PI * 2.0d;
        // SFData data = SFData.build(size);
        phase = phase * PI2;
        // for (int i = 0; i < size; ++i)
        // {
        // data.setSample(i, SFMaths.sin(i * PI2 * frequency / SFConstants.SAMPLE_RATE + phase));
        // }
        // return data;
        return new Generator(size, frequency, phase);
    }
}