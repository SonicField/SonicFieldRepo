/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import com.nerdscentral.audio.core.SFConstants;
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
public class SF_Silence implements SFPL_Operator
{

    public static class Generator extends SFSimpleGenerator
    {

        protected Generator(int len)
        {
            super(len);

        }

        @Override
        public double getSample(int index)
        {
            return 0;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        double length = Caster.makeDouble(input);
        return new Generator((int) (length * SFConstants.SAMPLE_RATE_MS));
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Silence.1");  //$NON-NLS-1$
    }

}