/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Invert implements SFPL_Operator
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
            return -getInputSample(index);
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input); SFSignal ret = new Translator(in);)
        {
            return Caster.prep4Ret(ret);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Invert.1");  //$NON-NLS-1$
    }

}