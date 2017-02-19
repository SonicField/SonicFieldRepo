/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_CrossFadeSplit implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;
    private final double      RAMP_SIZE        = 100;

    @Override
    public String Word()
    {
        return Messages.getString("SFPL_CrossFadeSplit.0"); //$NON-NLS-1$
    }

    @SuppressWarnings("unused")
    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        SFSignal in = Caster.makeSFSignal(lin.get(0));
        double chunkLen = Caster.makeDouble(lin.get(1));
        if (chunkLen < 2 * RAMP_SIZE) throw new SFPL_RuntimeException(Messages.getString("SF_CrossFadeSplit.0") + RAMP_SIZE); //$NON-NLS-1$
        List<Object> ret = new ArrayList<>();
        int chunkSamples = (int) (chunkLen * SFConstants.SAMPLE_RATE_MS);
        int index = 0;
        int rampSamples = (int) (RAMP_SIZE * SFConstants.SAMPLE_RATE_MS);
        SFSignal a = SFData.build(chunkSamples);
        while (index < chunkSamples - rampSamples)
        {
            a.setSample(index, in.getSample(index));
        }
        int rampEnd = index + rampSamples;
        int rampStart = index;
        while (index < rampEnd)
        {
            // TODO - all the below
        }
        // Work out the first bit before the first ramp
        // ..
        // Work do ramps
        // ..
        // Tidy up the last bit
        // ..
        return null;
    }

}
