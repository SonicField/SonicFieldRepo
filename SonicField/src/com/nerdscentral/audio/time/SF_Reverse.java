/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.audio.combine.Messages;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Reverse implements SFPL_Operator
{
    static class Translator extends SFSingleTranslator
    {

        protected Translator(SFSignal input)
        {
            super(input);
        }

        @Override
        public double getSample(int index)
        {
            return getInputSample(getLength() - 1 - index);
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input))
        {
            return new Translator(in);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Reverse.0");  //$NON-NLS-1$
    }

}