package com.nerdscentral.audio.time;

import java.util.LinkedList;
import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_TruncateSilence implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_TruncateSilence.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        try (SFSignal data = Caster.makeSFSignal(lin.get(0));)
        {
            double threshold = SFConstants.fromDBs(Caster.makeDouble(lin.get(1)));
            int len = (int) (Caster.makeDouble(lin.get(2)) * SFConstants.SAMPLE_RATE_MS);
            int newlen = 0;
            if (lin.size() > 3)
            {
                newlen = (int) Caster.makeDouble(lin.get(3));
            }
            int inLen = data.getLength();
            List<double[]> segments = new LinkedList<>();
            int outLen = 0;
            boolean inSilence = false;
            for (int index = 0; index < inLen;)
            {
                int diff = inLen - index;
                diff = diff > len ? len : diff;
                double[] segment = new double[diff];
                double max = 0;
                for (int inner = 0; inner < diff;)
                {
                    double x;
                    segment[inner] = x = data.getSample(index);
                    x = SFMaths.abs(x);
                    if (x > max) max = x;
                    ++inner;
                    ++index;
                }
                if (max > threshold)
                {
                    segments.add(segment);
                    inSilence = false;
                }
                else
                {
                    if (newlen > 0 && !inSilence)
                    {
                        segments.add(new double[(int) (newlen * SFConstants.SAMPLE_RATE_MS)]);
                        inSilence = true;
                    }
                }
                outLen += segment.length;
            }
            try (SFData outData = SFData.build(outLen);)
            {
                int index = 0;
                for (double[] segment : segments)
                {
                    for (int inner = 0; inner < segment.length;)
                    {
                        outData.setSample(index, segment[inner]);
                        ++index;
                        ++inner;
                    }
                }

                return Caster.prep4Ret(outData);
            }
        }

    }
}
