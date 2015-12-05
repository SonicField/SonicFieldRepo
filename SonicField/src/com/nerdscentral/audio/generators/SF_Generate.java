/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSimpleGenerator;
import com.nerdscentral.audio.pitch.SF_FrequencyDomain;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Generate implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    public static class Generator extends SFSimpleGenerator
    {

        final SFData                            waveTable;
        private final double                    upscale;
        private final int                       size;
        private final static SF_FrequencyDomain toFreq = new SF_FrequencyDomain();

        @Override
        public void release()
        {
            super.release();
            waveTable.release();
        }

        @SuppressWarnings("unused")
        protected Generator(int len, double up, SFSignal wt) throws SFPL_RuntimeException
        {
            super(len);
            this.size = len;
            // If the wave table is longer then sample rate we need
            // to upscale more and if it is shorter then less
            this.upscale = up * wt.getLength() / SFConstants.SAMPLE_RATE;
            // TODO use FFT to remove the frequencies higher than nyquist
            // after up scale

            // SFData fDomain = (SFData) toFreq.Interpret(wt, null);
            // Zero to filter
            // Convert back to time domain
            waveTable = SFData.realise(wt);
            wt.decrReferenceCount();
        }

        @Override
        public double getSample(int index)
        {
            double pos = index * upscale;
            pos = pos % size;
            // use periodic boundary condition for the interpolation
            return waveTable.getSampleCubicPeriodic(pos);
        }

    }

    @Override
    public String Word()
    {
        return com.nerdscentral.audio.generators.Messages.getString("SF_Generate.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        final List<Object> l = Caster.makeBunch(input);
        final SFSignal waveTable = Caster.makeSFSignal(l.get(2));
        final double upscale = Caster.makeDouble(l.get(1));
        final double duration = (Caster.makeDouble(l.get(0))) / 1000.0d;
        final int size = (int) (duration * SFConstants.SAMPLE_RATE);
        try (Generator g = new Generator(size, upscale, waveTable))
        {
            return Caster.prep4Ret(g);
        }
    }

}