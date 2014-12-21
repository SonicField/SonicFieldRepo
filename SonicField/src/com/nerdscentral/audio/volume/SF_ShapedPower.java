/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ShapedPower implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (SFSignal shape = Caster.makeSFSignal(l.get(1)); SFSignal in = (Caster.makeSFSignal(l.get(0))))
        {
            int length = in.getLength();
            if (length != shape.getLength())
            {
                throw new SFPL_RuntimeException(Messages.getString("SFP_ShapedPower.0")); //$NON-NLS-1$
            }
            try (SFSignal out = in.replicateEmpty())
            {
                for (int i = 0; i < length; ++i)
                {
                    double pw = shape.getSample(i);
                    double q = in.getSample(i);
                    if (q < 0)
                    {
                        out.setSample(i, -SFMaths.fastPow(-q, pw));
                    }
                    else
                    {
                        out.setSample(i, SFMaths.fastPow(q, pw));
                    }
                }
                return Caster.prep4Ret(out);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SFP_ShapedPower.1"); //$NON-NLS-1$
    }

}