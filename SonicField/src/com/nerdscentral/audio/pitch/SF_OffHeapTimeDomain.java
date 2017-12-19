package com.nerdscentral.audio.pitch;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.OffHeapFFTbase;
import com.nerdscentral.data.OffHeapArray;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_OffHeapTimeDomain implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_PhaseSpace.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {

        SFSignal signal = Caster.makeSFSignal(input);
        // TODO these feel like they should be half the length - investigate
        int NFFT = signal.getLength();
        try (
            OffHeapArray out = OffHeapArray.doubleArray(NFFT << 1);
            OffHeapArray re = OffHeapArray.doubleArray(NFFT);
            OffHeapArray im = OffHeapArray.doubleArray(NFFT))
        {

            int j = 0;
            re.initialise();
            im.initialise();
            out.initialise();
            for (int i = 0; i < NFFT >> 1; ++i)
            {
                re.setDouble(i, signal.getSample(j++));
                im.setDouble(i, signal.getSample(j++));
            }
            OffHeapFFTbase.fft(re, im, out, false);
            SFSignal ret = SFData.build(NFFT, true);
            for (int i = 0; i >> 1 < NFFT; i += 2)
            {
                ret.setSample(i >> 1, out.getDouble(i));
            }
            return ret;
        }
    }
}
