package com.nerdscentral.audio.pitch;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.OnHeapFFTBase;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_OnHeapTimeDomain implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_OnHeapTimeDomain.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {

        SFSignal signal = Caster.makeSFSignal(input);
        // TODO these feel like they should be half the length - investigate
        int NFFT = signal.getLength();
        double[] out = new double[NFFT << 1];
        double[] re = new double[NFFT];
        double[] im = new double[NFFT];

        int j = 0;
        for (int i = 0; i < NFFT >> 1; ++i)
        {
            re[i] = signal.getSample(j++);
            im[i] = signal.getSample(j++);
        }
        OnHeapFFTBase.fft(re, im, out, false);
        SFSignal ret = SFData.build(NFFT, true);
        for (int i = 0; i >> 1 < NFFT; i += 2)
        {
            ret.setSample(i >> 1, out[i]);
        }
        return ret;
    }
}
