/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Swap implements SFPL_Operator, SFPL_RefPassThrough
{

    public SF_Swap()
    {
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Swap.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal in = Caster.makeSFSignal(input);)
        {
            try (SFData out = SFData.realiseSwap(in))
            {
                if (out == in)
                {
                    Caster.incrReference(out);
                }
                Caster.incrReference(out);
                return out;
            }
        }
    }
}