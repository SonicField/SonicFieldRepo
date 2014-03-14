/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_DirectRelength implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (SFSignal sampleA = Caster.makeSFSignal(l.get(0)))
        {
            double rate = Caster.makeDouble(l.get(1));
            int lengthIn = sampleA.getLength();
            int len = (int) (lengthIn / rate);
            try (SFData ret = SFData.build(len))
            {
                double pos = 0;
                for (int i = 0; i < len && pos < lengthIn; ++i)
                {
                    ret.setSample(i, sampleA.getSampleCubic(pos));
                    pos = pos + rate;
                }
                return Caster.prep4Ret(ret);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_DirectRelength.0"); //$NON-NLS-1$
    }

}