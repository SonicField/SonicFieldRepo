/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ClipToSafe implements SFPL_Operator
{
    private final static int  rollOffSamples   = (int) (SFConstants.SAMPLE_RATE_MS * 20);
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        if (data.getLength() < rollOffSamples)
        {
            try (SFData x = SFData.build(data.getLength()))
            {
                return Caster.prep4Ret(x);
            }
        }
        int len = data.getLength() - rollOffSamples;
        try (SFSignal out = data.replicateEmpty();)
        {
            double dc = data.getSample(0);
            for (int i = 0; i < len; ++i)
            {
                out.setSample(i, data.getSample(i) - (dc * (len - i) / len));
            }

            for (int i = 0; i < rollOffSamples; ++i)
            {
                out.setSample(i + len, data.getSample(i + len) * (rollOffSamples - i) / rollOffSamples);
            }
            return Caster.prep4Ret(out);
        }
        finally
        {
            data.__neg__();
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_ClipToSafe.0"); //$NON-NLS-1$
    }

}