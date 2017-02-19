/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSimpleGenerator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * 10 Sf.Silence ... forwards an SFData of 10 milliseconds of silence ...
 * 
 * @author AlexTu
 * 
 */
public class SF_WhiteNoise implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static class Generator extends SFSimpleGenerator
    {
        long   x  = System.nanoTime();
        long   xi = x;
        double g  = Integer.MAX_VALUE;
        SFSignal data;

        Generator(int len)
        {
            super(len);
        }

        @Override
        public double getSample(int index)
        {
            x ^= (x << 21);
            x ^= (x >>> 35);
            x ^= (x << 4);
            int y = (int) x;
            return y / g;
        }
    }

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        double length = Caster.makeDouble(input);
        return new Generator((int) (length * SFConstants.SAMPLE_RATE_MS));
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_WhiteNoise.1");  //$NON-NLS-1$
    }

}