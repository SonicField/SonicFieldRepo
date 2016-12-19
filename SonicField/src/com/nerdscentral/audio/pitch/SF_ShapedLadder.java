/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ShapedLadder implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private class InnerFilter
    {
        double cutoff;
        double res;
        double fs;
        double y1, y2, y3, y4;
        double oldx;
        double oldy1, oldy2, oldy3;
        double x;
        double r;
        double p;
        double k;

        InnerFilter()
        {
            fs = SFConstants.SAMPLE_RATE;
            init();
        }

        void init()
        {
            // initialize values
            y1 = y2 = y3 = y4 = oldx = oldy1 = oldy2 = oldy3 = 0;
            calc();
        }

        void calc()
        {
            double f = (cutoff + cutoff) / fs; // [0 - 1]
            p = f * (1.8f - 0.8f * f);
            k = p + p - 1.f;

            double t = (1.f - p) * 1.386249f;
            double t2 = 12.f + t * t;
            r = res * (t2 + 6.f * t) / (t2 - 6.f * t);
        }

        double process(double input)
        {
            // process input
            x = input - r * y4;

            // Four cascaded onepole filters (bilinear transform)
            y1 = x * p + oldx * p - k * y1;
            y2 = y1 * p + oldy1 * p - k * y2;
            y3 = y2 * p + oldy2 * p - k * y3;
            y4 = y3 * p + oldy3 * p - k * y4;

            // Clipper band limited sigmoid
            y4 -= (y4 * y4 * y4) / 6.f;

            oldx = x;
            oldy1 = y1;
            oldy2 = y2;
            oldy3 = y3;
            return y4;
        }

        @SuppressWarnings("unused")
        double getCutoff()
        {
            return cutoff;
        }

        void setCutoff(double c)
        {
            // Only recalculate when the change is bigger than
            // one cent
            double cc = c / cutoff;
            if (cc < 1.0) cc = 1.0 / cc;
            if (cc > 1.0005777895)
            {
                // System.out.println("Recomputing c " + c);
                cutoff = c;
                calc();
            }
        }

        @SuppressWarnings("unused")
        double getRes()
        {
            return res;
        }

        void setRes(double r1)
        {
            // Only recalculate when the change is bigger than
            // one cent
            double rr = r1 / cutoff;
            if (rr < 1.0) rr = 1.0 / r1;
            if (rr > 1.0005777895)
            {
                res = r1;
                calc();
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Ladder.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> in = Caster.makeBunch(input);
        SFSignal dataIn = Caster.makeSFSignal(in.get(0));
        SFSignal shape = Caster.makeSFSignal(in.get(1));
        SFSignal resonance = Caster.makeSFSignal(in.get(2));
        SFSignal ret = dataIn.replicateEmpty();
        int length = dataIn.getLength();
        if (shape.getLength() != length || resonance.getLength() != length)
        {
            throw new SFPL_RuntimeException(Messages.getString("SF_Ladder.1")); //$NON-NLS-1$
        }
        InnerFilter filter = new InnerFilter();
        filter.init();
        for (int index = 0; index < length; ++index)
        {
            filter.setRes(resonance.getSample(index));
            filter.setCutoff(shape.getSample(index));
            ret.setSample(index, filter.process(dataIn.getSample(index)));
        }
        return ret;
    }
}
