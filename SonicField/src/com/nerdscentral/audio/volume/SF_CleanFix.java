/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_CleanFix implements SFPL_Operator
{
    static class Translator extends SFSingleTranslator
    {
        final double scale;

        protected Translator(SFSignal input, double scaleIn)
        {
            super(input);
            scale = scaleIn;
        }

        @Override
        public double getSample(int index)
        {
            double r = getInputSample(index) * scale;
            return r;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal sig = Caster.makeSFSignal(input))
        {
            try (SFData dataIn = SFData.realise(sig); SFData dataOut = dataIn.replicateEmpty();)
            {
                if (dataIn == sig) dataIn.incrReferenceCount();

                double max = SF_Clean.decimateFilter(dataIn, dataOut);
                max = 1 / max;
                Translator ret = new Translator(dataOut, max);
                return ret;
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_CleanFix.0"); //$NON-NLS-1$
    }

}