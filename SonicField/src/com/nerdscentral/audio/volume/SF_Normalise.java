/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1 Sf.Normalise !s1_normal... forwards an SFData normalised ...
 * 
 * @author AlexTu
 * 
 */

public class SF_Normalise implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static class Translator extends SFSingleTranslator
    {
        final double dc;
        final double scale;

        protected Translator(SFSignal input, double dcIn, double scaleIn)
        {
            super(input);
            dc = dcIn;
            scale = scaleIn;
        }

        @Override
        public double getSample(int index)
        {
            return (getInputSample(index) + dc) * scale;
        }

    }

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        return doNormalisation(data);
    }

    public static SFSignal doNormalisation(SFSignal signalIn)
    {
        double dc = 0;

        SFSignal data = SFData.realise(signalIn);
        int len = data.getLength();
        for (int i = 0; i < len; ++i)
        {
            dc = dc + data.getSample(i);
        }
        dc /= data.getLength();
        dc = -dc;

        double max = 0;
        for (int i = 0; i < len; ++i)
        {
            double d = SFMaths.abs(data.getSample(i) + dc);
            if (d > max)
            {
                max = d;
            }
        }
        max = 1 / max;
        Translator ret = new Translator(data, dc, max);
        return ret;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Normalise.4");  //$NON-NLS-1$
    }

}