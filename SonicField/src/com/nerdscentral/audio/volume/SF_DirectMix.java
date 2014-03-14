/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_DirectMix implements SFPL_Operator
{

    public static class Translator extends SFSingleTranslator
    {
        private final double toAdd;

        Translator(SFSignal input, double add)
        {
            super(input);
            toAdd = add;
        }

        @Override
        public double getSample(int index)
        {
            return getInputSample(index) + toAdd;
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        double constant = Caster.makeDouble(l.get(0));
        try (SFSignal in = (Caster.makeSFSignal(l.get(1)));)
        {
            try (SFSignal f = new Translator(in, constant))
            {
                return Caster.prep4Ret(f);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_DirectMix.0"); //$NON-NLS-1$
    }

}