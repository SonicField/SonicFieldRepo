/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ToGrain implements SFPL_Operator
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        int rollOffSamples = data.getLength() / 2;
        int len = data.getLength() - rollOffSamples;
        SFSignal out = data.replicateEmpty();
        for (int i = 0; i < rollOffSamples + 1; ++i)
        {
            out.setSample(i, data.getSample(i) * i / rollOffSamples);
        }

        for (int i = 0; i < rollOffSamples; ++i)
        {
            out.setSample(i + len, data.getSample(i + len) * (rollOffSamples - i) / rollOffSamples);
        }
        return out;

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_ToGrain.0"); //$NON-NLS-1$
    }

}