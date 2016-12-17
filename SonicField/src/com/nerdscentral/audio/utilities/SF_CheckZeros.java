/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_CheckZeros implements SFPL_Operator, SFPL_RefPassThrough
{

    public SF_CheckZeros()
    {
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_CheckZeros.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input);)
        {
            int len = in.getLength();

            for (int i = 0; i < len; ++i)
            {
                if (in.getSample(i) == 0)
                {
                    System.out.println(Messages.getString("SF_CheckZeros.1") + i + Messages.getString("SF_CheckZeros.2") + (len - 1) + Messages.getString("SF_CheckZeros.3")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
            }

            return Caster.incrReference(in);
        }
    }
}