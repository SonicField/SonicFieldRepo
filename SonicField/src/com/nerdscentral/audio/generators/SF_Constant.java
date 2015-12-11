/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.SFSimpleGenerator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Constant implements SFPL_Operator
{
    public static class Generator extends SFSimpleGenerator
    {

        final double value;

        protected Generator(int len, double v)
        {
            super(len);
            value = v;
        }

        @Override
        public double getSample(int index)
        {
            return value;
        }

    }

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_NumericShape.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        return new Generator(Caster.makeInt(l.get(0)), Caster.makeDouble(l.get(1)));
    }
}
