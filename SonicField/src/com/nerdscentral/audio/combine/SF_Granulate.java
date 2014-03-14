/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Granulate implements SFPL_Operator
{
    class BaseTranslator extends SFSingleTranslator
    {

        @Override
        protected double getInputSample(int index)
        {
            if (index >= super.getLength()) return 0;
            return super.getInputSample(index);
        }

        final int start;
        final int rolloff;

        @Override
        public int getLength()
        {
            return rolloff * 2;
        }

        protected BaseTranslator(SFSignal input, int startIn, int rolloffIn)
        {
            super(input);
            start = startIn;
            rolloff = rolloffIn;
        }

        @Override
        public double getSample(int index)
        {
            if (index < rolloff)
            {
                return getInputSample(index);
            }
            double offset = rolloff * 2 - index;
            double attenue = offset / rolloff;
            return getInputSample(index) * attenue;
        }

    }

    class MiddleTranslator extends BaseTranslator
    {

        protected MiddleTranslator(SFSignal input, int startIn, int rolloffIn)
        {
            super(input, startIn, rolloffIn);
        }

        @Override
        public double getSample(int index)
        {
            int relIndex = index + start;
            if (index < rolloff)
            {
                double offset = index;
                double attenue = offset / rolloff;
                return getInputSample(relIndex) * attenue;
            }
            double offset = rolloff * 2 - index;
            double attenue = offset / rolloff;
            return getInputSample(relIndex) * attenue;
        }
    }

    class EndTranslator extends BaseTranslator
    {

        protected EndTranslator(SFSignal input, int startIn, int rolloffIn)
        {
            super(input, startIn, rolloffIn);
        }

        @Override
        public double getSample(int index)
        {
            int relIndex = index + start;
            if (index < rolloff)
            {
                double offset = index;
                double attenue = offset / rolloff;
                return getInputSample(relIndex) * attenue;
            }
            return getInputSample(relIndex);
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        try (SFSignal data = Caster.makeSFSignal(lin.get(0));)
        {
            int rollOffSamples = (int) (Caster.makeDouble(lin.get(1)) * SFConstants.SAMPLE_RATE_MS);
            int randSamples = lin.size() > 2 ? (int) (Caster.makeDouble(lin.get(2)) * SFConstants.SAMPLE_RATE_MS) : 0;
            rollOffSamples /= 2;
            List<Object> retOuter = new ArrayList<>();
            int len = data.getLength();
            int start = 0;
            while (true)
            {
                int rollOffSamplesOld = rollOffSamples;
                rollOffSamples += SFMaths.random() * randSamples;
                int k = rollOffSamples * 2;
                int end = start + k;
                BaseTranslator trans = null;
                if (start == 0)
                {
                    try (BaseTranslator x = new BaseTranslator(data, start, rollOffSamples);)
                    {
                        Caster.prep4Ret(x);
                        trans = x;
                    }
                }
                else if (end >= len)
                {
                    try (BaseTranslator x = new EndTranslator(data, start, rollOffSamples);)
                    {
                        Caster.prep4Ret(x);
                        trans = x;
                    }
                }
                else
                {
                    try (BaseTranslator x = new MiddleTranslator(data, start, rollOffSamples);)
                    {
                        Caster.prep4Ret(x);
                        trans = x;
                    }
                }
                // add to retout,
                List<Object> thisRet = new ArrayList<>(2);
                thisRet.add(trans);
                thisRet.add(start / SFConstants.SAMPLE_RATE_MS);
                retOuter.add(thisRet);
                // All done
                if (end >= len) break;
                // move on by rolloutsamples
                start += rollOffSamples;
                rollOffSamples = rollOffSamplesOld;
            }
            return retOuter;
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Granulate.0"); //$NON-NLS-1$
    }

}