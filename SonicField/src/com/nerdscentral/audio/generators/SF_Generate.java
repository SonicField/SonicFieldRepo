/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFSimpleGenerator;
import com.nerdscentral.audio.pitch.SFNPoleFilterOperator;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator.NPoleFilterDef;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Generate implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    private static class Filter extends SFNPoleFilterOperator
    {

        static SFData filter(SFData x, double frequency)
        {
            NPoleFilterDef fd = SFFilterGenerator.computeBesselNLP(frequency, 6);
            SFData y = x.replicateEmpty();
            filterLoop(x, y, fd, fd.getGaindc());
            return y;
        }
    }

    public static class Generator extends SFSimpleGenerator
    {

        final static double  PI2 = SFMaths.PI * 2.0d;
        final SFData         waveTable;
        private final double upscale;
        private final int    size;

        @Override
        public void release()
        {
            super.release();
            waveTable.release();
        }

        // TO DO reduce the amount of wave table stored, there is
        // no need to store 3 times , just 1 and a little bit at each side
        @SuppressWarnings("resource")
        // resource managed by container
        protected Generator(int len, double up, SFSignal wt)
        {
            super(len);
            this.size = len;
            this.upscale = up;
            SFData ttable = SFData.build(size * 3);
            for (int i = 0; i < size; ++i)
            {
                double data = wt.getSample(i);
                ttable.setSample(i, data);
                ttable.setSample(i + size, data);
                ttable.setSample(i + size * 2, data);
            }
            // Remove all frequencies which will be over the Nyquist limit
            // Before sub-sampling
            this.waveTable = Filter.filter(ttable, 20000 / upscale);
        }

        @Override
        public double getSample(int index)
        {
            double pos = index * upscale;
            pos = pos % size;
            return waveTable.getSampleCubic(index + size);
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