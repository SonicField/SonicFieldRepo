/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSimpleGenerator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Sin implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    public static class Generator extends SFSimpleGenerator
    {

        final static double PI2 = SFMaths.PI * 2.0d;
        final double        mult;

        protected Generator(int len, double frequ)
        {
            super(len);
            mult = PI2 * frequ / SFConstants.SAMPLE_RATE;
        }

        @Override
        public double getSample(int index)
        {
            return SFMaths.sin(index * mult);
        }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Sin.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        final List<Object> l = Caster.makeBunch(input);
        final double frequency = Caster.makeDouble(l.get(1));
        final double duration = (Caster.makeDouble(l.get(0))) / 1000.0d;
        final int size = (int) (duration * SFConstants.SAMPLE_RATE);
        return new Generator(size, frequency);
    }

}
