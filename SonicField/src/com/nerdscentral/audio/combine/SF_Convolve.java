/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Convolve implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Convolve.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        try (
            SFSignal sample = Caster.makeSFSignal(inList.get(0));
            SFSignal shape = Caster.makeSFSignal(inList.get(1));
            SFSignal ret = sample.replicateEmpty())
        {
            // double[] sampleData = sample.getData();
            // double[] shapeData = shape.getData();
            // double[] retData = ret.getData();
            int ll = sample.getLength();
            int ls = shape.getLength();
            int center = shape.getLength() / 2;
            for (int x = 0; x < ll; ++x)
            {
                double r = 0;
                Thread.yield();
                for (int y = 0; y < ls; ++y)
                {
                    double d = shape.getSample(ls - y);
                    if (d == 0) continue;
                    int pos = x - center + y;
                    double q = 0;
                    if (pos < 0)
                    {
                        q = sample.getSample(pos + ls);
                    }
                    else if (pos >= ll)
                    {
                        q = sample.getSample(pos - ls);
                    }
                    else
                    {
                        q = sample.getSample(pos);
                    }
                    r = (r + q * d);
                }
                ret.setSample(x, r);
            }
            return Caster.prep4Ret(ret);
        }
    }

}
