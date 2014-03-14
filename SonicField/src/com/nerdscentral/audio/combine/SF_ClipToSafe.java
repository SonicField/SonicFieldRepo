/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
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
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
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
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_ClipToSafe.0"); //$NON-NLS-1$
    }

}