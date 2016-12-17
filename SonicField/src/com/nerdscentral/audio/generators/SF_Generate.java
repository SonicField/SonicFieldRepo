/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSimpleGenerator;
import com.nerdscentral.audio.pitch.SF_FrequencyDomain;
import com.nerdscentral.sython.Caster;
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

        // No sensible way to automatically remove frequencies
        // which will exceed nyquist on upscaling. Therefore
        // this does not bother, it relies on the uncomming table
        // having the correct mix of frequencies to correctly
        // upscale
        protected Generator(int len, double up, SFSignal wt)
        {
            super(len);
            this.size = len;
            // If the wave table is longer then sample rate we need
            // to upscale more and if it is shorter then less
            this.upscale = up * wt.getLength() / SFConstants.SAMPLE_RATE;
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
    public Object Interpret(Object input) throws SFPL_RuntimeException
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