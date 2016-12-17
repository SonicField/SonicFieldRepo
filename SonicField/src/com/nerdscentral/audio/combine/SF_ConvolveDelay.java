/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ConvolveDelay implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ConvolveDelay.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        try (
            SFSignal sample = Caster.makeSFSignal(inList.get(0));
            SFSignal shape = Caster.makeSFSignal(inList.get(1));
            SFSignal ret = sample.replicateEmpty();)
        {
            int ll = sample.getLength();
            int ls = shape.getLength();
            for (int x = 0; x < ll; ++x)
            {
                double r = 0;
                int pos = x;
                for (int y = 0; y < ls; ++y)
                {
                    r = (r + sample.getSample(pos) * shape.getSample(ls - y));
                    if (--pos < 0) break;
                }
                ret.setSample(x, r);
            }
            return Caster.prep4Ret(ret);
        }
    }
}
