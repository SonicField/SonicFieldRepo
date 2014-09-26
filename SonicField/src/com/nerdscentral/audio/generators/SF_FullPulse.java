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

public class SF_FullPulse implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    public static class Generator extends SFSimpleGenerator
    {

        final double         frequency;
        final static double  PI2 = SFMaths.PI * 2.0d;
        final private double w;

        protected Generator(int len, double frequ)
        {
            super(len);
            frequency = frequ;
            w = SFConstants.SAMPLE_RATE / frequency;
        }

        @Override
        public double getSample(int index)
        {
            int z = (int) (index % w);
            if (z == 0) return -1;
            if (z == 1) return 1;
            return 0;
        }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_FullPulse.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        final List<Object> l = Caster.makeBunch(input);
        final double frequency = Caster.makeDouble(l.get(1));
        final double duration = (Caster.makeDouble(l.get(0))) / 1000.0d;
        final int size = (int) (duration * SFConstants.SAMPLE_RATE);
        return new Generator(size, frequency);
    }

}