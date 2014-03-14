/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Check implements SFPL_Operator, SFPL_RefPassThrough
{

    public SF_Check()
    {
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SFPLSonicFieldLib.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input);)
        {
            int len = in.getLength();

            for (int i = 0; i < len; ++i)
            {
                double q = in.getSample(i);
                if (Double.isInfinite(q)) throw new SFPL_RuntimeException(Messages.getString("SFPLSonicFieldLib.1") + i); //$NON-NLS-1$
                if (Double.isNaN(q)) throw new SFPL_RuntimeException(Messages.getString("SFPLSonicFieldLib.2") + i); //$NON-NLS-1$
            }

            return Caster.incrReference(in);
        }
    }
}