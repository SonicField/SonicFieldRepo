/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.amps;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Saturate implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Saturate.0"); //$NON-NLS-1$
    }

    public static class Translator extends SFSingleTranslator
    {

        protected Translator(SFSignal input)
        {
            super(input);
        }

        @Override
        public double getSample(int index)
        {
            double x = getInputSample(index);
            double y = x >= 0 ? x / (x + 1) : x / (1 - x);
            return y;
        }
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input))
        {
            return new Translator(in);
        }
    }

}
