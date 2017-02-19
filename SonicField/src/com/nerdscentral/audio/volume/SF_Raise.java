/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Raise implements SFPL_Operator
{
    public static class Translate extends SFSingleTranslator
    {
        final double pw;

        protected Translate(SFSignal input, double expIn)
        {
            super(input);
            pw = expIn;
        }

        @Override
        public double getSample(int index)
        {
            double q = getInputSample(index);
            return SFMaths.pow(q, pw);
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        double pw = Caster.makeDouble(l.get(1));
        SFSignal in = Caster.makeSFSignal(l.get(0));
        return new Translate(in, pw);

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Raise.0"); //$NON-NLS-1$
    }

}