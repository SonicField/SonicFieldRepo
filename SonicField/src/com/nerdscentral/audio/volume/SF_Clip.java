/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Clip implements SFPL_Operator
{

    public static class Translator extends SFSingleTranslator
    {

        protected Translator(SFSignal input)
        {
            super(input);
        }

        @Override
        public double getSample(int index)
        {
            double q = getInputSample(index);
            if (q < -1) q = -1;
            else if (q > 1) q = 1;
            return q;
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input); SFData out = in.replicateEmpty();)
        {
            for (int i = 0; i < in.getLength(); ++i)
            {
                double q = in.getSample(i);
                if (q < -1) q = -1;
                else if (q > 1) q = 1;
                out.setSample(i, q);
            }
            return Caster.prep4Ret(out);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Clip.1"); //$NON-NLS-1$
    }

}