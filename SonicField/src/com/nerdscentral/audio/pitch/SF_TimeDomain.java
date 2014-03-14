package com.nerdscentral.audio.pitch;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.FFTbase;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_TimeDomain implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_PhaseSpace.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        double[] re, im;
        int NFFT;

        try (SFSignal signal = Caster.makeSFSignal(input))
        {

            NFFT = signal.getLength();

            re = new double[NFFT];
            im = new double[NFFT];
            int j = 0;
            for (int i = 0; i < NFFT / 2; ++i)
            {
                re[i] = signal.getSample(j++);
                im[i] = signal.getSample(j++);
            }
        }
        double[] d = FFTbase.fft(re, im, false);

        try (SFData ret = SFData.build(NFFT))
        {
            for (int i = 0; i / 2 < NFFT; i += 2)
            {
                ret.setSample(i / 2, d[i]);
            }
            return Caster.prep4Ret(ret);
        }
    }
}
