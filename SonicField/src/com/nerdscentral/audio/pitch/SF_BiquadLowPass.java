/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.SFRBJFilter;
import com.nerdscentral.audio.pitch.algorithm.SFRBJFilter.FilterType;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_BiquadLowPass implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_BiquadLowPass.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        SFRBJFilter filter = new SFRBJFilter();
        try (SFSignal x = Caster.makeSFSignal(l.get(0)))
        {
            double frequency = Caster.makeDouble(l.get(1));
            double q = Caster.makeDouble(l.get(2));
            FilterType type = FilterType.LOWPASS;
            filter.calc_filter_coeffs(type, frequency, q, 0);
            try (SFSignal y = x.replicateEmpty())
            {
                int length = y.getLength();
                for (int index = 0; index < length; ++index)
                {
                    y.setSample(index, filter.filter(x.getSample(index)));
                }
                return Caster.prep4Ret(y);
            }
        }
    }
}
