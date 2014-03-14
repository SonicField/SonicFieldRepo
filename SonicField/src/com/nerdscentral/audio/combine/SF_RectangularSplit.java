/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_RectangularSplit implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_RectangularSplit.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFSignal sample = Caster.makeSFSignal(input);)
        {
            int len = sample.getLength();
            try (SFSignal real = SFData.build(len); SFSignal imaginary = SFData.build(len);)
            {
                int j = 0;
                for (int i = 0; i < len; i += 2)
                {
                    real.setSample(j, sample.getSample(i));
                    imaginary.setSample(j, sample.getSample(i + 1));
                    ++j;
                }
                List<Object> ret = new ArrayList<>();
                ret.add(real);
                ret.add(imaginary);
                return Caster.prep4Ret(ret);
            }
        }
    }
}
