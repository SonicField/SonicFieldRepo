package com.nerdscentral.audio.pitch;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.FFTbase;
import com.nerdscentral.data.OffHeapArray;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_OffHeapFrequencyDomain implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_PhaseSpace.1"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        SFSignal signal = Caster.makeSFSignal(input);
        int Nx = signal.getLength();
        int NFFT = (int) Math.pow(2.0, Math.ceil(Math.log(Nx) / Math.log(2.0)));
        try (
            OffHeapArray out = OffHeapArray.doubleArray(NFFT << 1);
            OffHeapArray re = OffHeapArray.doubleArray(NFFT);
            OffHeapArray im = OffHeapArray.doubleArray(NFFT))
        {
            out.initialise();
            re.initialise();
            im.initialise();
            for (int i = 0; i < Nx; ++i)
            {
                re.setDouble(i, signal.getSample(i));
            }
            FFTbase.fft(re, im, out, true);
            SFSignal ret = SFData.build(out, NFFT);
            return ret;
        }
    }
}
