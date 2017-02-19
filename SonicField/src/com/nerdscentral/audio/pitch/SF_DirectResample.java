/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_DirectResample implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        SFSignal sampleA = Caster.makeSFSignal(l.get(0));
        double rate = Caster.makeDouble(l.get(1));
        int len = sampleA.getLength();
        SFSignal ret = sampleA.replicateEmpty();
        double pos = 0;
        for (int i = 0; i < len; ++i)
        {
            ret.setSample(i, sampleA.getSampleCubic(pos));
            pos = pos + rate;
        }
        return ret;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_DirectResample.0"); //$NON-NLS-1$
    }

}