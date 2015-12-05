/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1 Sf.Normalise !s1_normal... forwards an SFData normalised ...
 * 
 * @author AlexTu
 * 
 */

public class SF_FixSize implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

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
            return (getInputSample(index)) * scale;
        }

    }

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal data = Caster.makeSFSignal(input))
        {
            return setup(data);
        }
    }

    private static SFSignal setup(SFSignal signalIn)
    {

        try (SFData data = SFData.realise(signalIn);)
        {
            // If we keep the input then we increase its reference
            // so when what ever calls reduces it all
            // is OK. IE this behaves as though the return is always new
            if (data == signalIn) data.incrReferenceCount();
            int len = data.getLength();

            double max = 0;
            for (int i = 0; i < len; ++i)
            {
                double d = SFMaths.abs(data.getSample(i));
                if (d > max)
                {
                    max = d;
                }
            }
            max = 1 / max;
            Translator ret = new Translator(data, max);
            return ret;
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_FixSize.0"); //$NON-NLS-1$
    }

}