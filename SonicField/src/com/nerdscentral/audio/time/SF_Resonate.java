/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Resonate implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    private static class ResonantDescriptor
    {
        private final int      delaySamples;
        private final SFSignal shape;

        public ResonantDescriptor(int delaySamplesIn, SFSignal shapeIn)
        {
            delaySamples = delaySamplesIn;
            shape = shapeIn;
        }

        /**
         * @return the delaySamples
         */
        public int getDelaySamples()
        {
            return delaySamples;
        }

        /**
         * @return the shape
         */
        public SFSignal getShape()
        {
            return shape;
        }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Resonate.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        SFSignal in = Caster.makeSFSignal(lin.get(0));
        lin = Caster.makeBunch(lin.get(1));
        List<ResonantDescriptor> descriptors = new ArrayList<>(lin.size());
        for (int i = 0; i < lin.size(); ++i)
        {
            List<Object> llin = Caster.makeBunch(lin.get(i));
            @SuppressWarnings("resource")
            SFSignal shape = Caster.makeSFSignal(llin.get(0));
            int delay = (int) (Caster.makeDouble(llin.get(1)) * SFConstants.SAMPLE_RATE_MS);
            descriptors.add(new ResonantDescriptor(delay, shape));
        }
        try (SFSignal out = in.replicate())
        {
            double r = in.getLength();
            endAll: for (int n = 0; n < r; ++n)
            {
                double q = out.getSample(n);
                boolean overflowAll = true;
                endOne: for (ResonantDescriptor descriptor : descriptors)
                {
                    @SuppressWarnings("resource")
                    SFSignal shape = descriptor.getShape();
                    int delay = descriptor.getDelaySamples();
                    int t = shape.getLength();
                    for (int m = 0; m < t; ++m)
                    {
                        int index = n + delay + m;
                        if (index >= r) break endOne;
                        out.setSample(index, out.getSample(index) + q * shape.getSample(m));
                    }
                    overflowAll = false;

                }
                if (overflowAll) break endAll;
            }
            for (ResonantDescriptor descriptor : descriptors)
            {
                @SuppressWarnings("resource")
                SFSignal shape = descriptor.getShape();
                shape.close();
            }
            in.close();
            return Caster.prep4Ret(out);
        }
    }
}
