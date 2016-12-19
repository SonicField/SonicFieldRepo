/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.List;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ConvolveForward implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ConvolveForward.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        SFSignal sample = Caster.makeSFSignal(inList.get(0));
        SFSignal shape = Caster.makeSFSignal(inList.get(1));
        int ll = sample.getLength();
        int ls = shape.getLength();
        SFData ret = SFData.build(ll + ls);
        for (int x = 0; x < ll; ++x)
        {
            double r = 0;
            for (int y = 0; y < ls; ++y)
            {
                int p = x + y;
                if (p >= ll) break;
                r = (r + sample.getSample(p) * shape.getSample(ls - y));
            }
            ret.setSample(x, r);
        }
        return ret;
    }

}
