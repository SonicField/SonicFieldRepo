/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_MultipleResonantFilter implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    private static class ResonantDescriptor
    {
        private final int    delaySamples;
        private final double volume;

        public ResonantDescriptor(int delaySamplesIn, double volumeIn)
        {
            delaySamples = delaySamplesIn;
            volume = volumeIn;
        }

        /**
         * @return the delaySamples
         */
        public int getDelaySamples()
        {
            return delaySamples;
        }

        /**
         * @return the volume
         */
        public double getVolume()
        {
            return volume;
        }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_DirectResonate.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        SFSignal in = Caster.makeSFSignal(lin.get(0));
        List<Object> lin2 = Caster.makeBunch(lin.get(1));
        double forward = 1;
        if (lin.size() > 2)
        {
            forward = Caster.makeDouble(lin.get(2));
            forward = SFConstants.fromDBs(forward);
        }
        List<ResonantDescriptor> descriptors = new ArrayList<>(lin.size());
        for (int i = 0; i < lin2.size(); ++i)
        {
            List<Object> llin = Caster.makeBunch(lin2.get(i));
            double volume = Caster.makeDouble(llin.get(0));
            int delay = (int) ((Caster.makeDouble(llin.get(1)) * SFConstants.SAMPLE_RATE_MS));
            descriptors.add(new ResonantDescriptor(delay, SFConstants.fromDBs(volume)));
        }
        SFSignal out = in.replicate();
        double r = in.getLength();
        // Scale forward so it effect is the same as resonators are added or taken away
        forward = Math.pow(forward, 1.0 / descriptors.size());
        for (int n = 0; n < r; ++n)
        {
            double q = out.getSample(n);
            for (ResonantDescriptor descriptor : descriptors)
            {
                double volume = descriptor.getVolume();
                int delay = descriptor.getDelaySamples();
                int index = n + delay;
                if (n < delay)
                {
                    q = q * volume;
                    out.setSample(n, q);
                }
                if (index < r)
                {
                    out.setSample(index, out.getSample(index) * forward + q * volume);
                }
            }
        }
        return out;
    }
}
