/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.amps;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SFAmpAlgorithms
{
    public static SFSignal limit(SFSignal in)
    {
        int len = in.getLength();
        SFSignal out = SFData.build(len);
        int start = 0;
        int end = 0;
        for (;;)
        {
            start = end;
            int sign = getSign(in.getSample(end));
            double max = 0;
            while (end < len && getSign(in.getSample(end)) == sign)
            {
                double p = SFMaths.abs(in.getSample(end));
                if (p > max) max = p;
                ++end;
            }
            if (max > 1)
            {
                max = 1 / max;
            }
            else
            {
                max = 1;
            }

            for (int n = start; n < end; ++n)
            {
                out.setSample(n, in.getSample(n) * max);
            }
            if (!(end < len)) break;
        }
        return out;
    }

    public static SFSignal maximise(SFSignal in)
    {
        int len = in.getLength();
        SFSignal out = SFData.build(len);
        int start = 0;
        int end = 0;
        for (;;)
        {
            start = end;
            int sign = getSign(in.getSample(end));
            double max = 0;
            while (end < len && getSign(in.getSample(end)) == sign)
            {
                double p = SFMaths.abs(in.getSample(end));
                if (p > max) max = p;
                ++end;
            }
            max = 1 / max;
            for (int n = start; n < end; ++n)
            {
                out.setSample(n, in.getSample(n) * max);
            }
            if (!(end < len)) break;
        }
        return out;
    }

    public static SFSignal makeSquare(SFSignal in)
    {
        int len = in.getLength();
        SFSignal out = SFData.build(len);
        int start = 0;
        int end = 0;
        for (;;)
        {
            start = end;
            int sign = getSign(in.getSample(end));
            double sum = 0;
            while (end < len && getSign(in.getSample(end)) == sign)
            {
                sum += SFMaths.abs(in.getSample(end));
                ++end;
            }
            // start is the start of the half wave
            // end is the end of the half wave
            // sum is the area of the half wave
            double length = end - start;
            double hight = sum / length;
            for (int n = start; n < end; ++n)
            {
                out.setSample(n, sign * hight);
            }
            if (!(end < len)) break;
        }
        return out;
    }

    private static int getSign(double what)
    {
        return what < 0 ? -1 : 1;
    }

    public static SFSignal makeTriangle(SFSignal in)
    {
        int len = in.getLength();
        SFSignal out = SFData.build(len);
        int start = 0;
        int end = 0;
        for (;;)
        {
            start = end;
            int sign = getSign(in.getSample(end));
            double sum = 0;
            while (end < len && getSign(in.getSample(end)) == sign)
            {
                sum += SFMaths.abs(in.getSample(end));
                ++end;
            }
            // start is the start of the half wave
            // end is the end of the half wave
            // sum is the area of the half wave
            double length = end - start;
            double hight = sum * 2 / length;
            int middle = (int) (start + SFMaths.floor(length / 2));
            int middlePoint = middle - start;
            if (middlePoint == 0)
            {
                middlePoint = 1;
            }
            for (int n = start; n < middle; ++n)
            {
                out.setSample(n, (sign * hight) * (n - start) / middlePoint);
            }
            for (int n = middle; n < end; ++n)
            {
                out.setSample(n, (sign * hight) * (end - n - 1) / middlePoint);
            }
            if (!(end < len)) break;
        }
        return out;
    }

    public static SFSignal makeSawTooth(SFSignal in)
    {
        int len = in.getLength();
        SFSignal out = SFData.build(len);
        int start = 0;
        int end = 1;
        boolean ab = true;
        for (;;)
        {
            // Must use -1 or get an extra zero at the cross over
            start = end - 1;
            int sign = getSign(in.getSample(end));
            double sum = 0;
            while (end < len && getSign(in.getSample(end)) == sign)
            {
                sum += SFMaths.abs(in.getSample(end));
                ++end;
            }
            // start is the start of the half wave
            // end is the end of the half wave
            // sum is the area of the half wave
            double length = end - start;
            double hight = 2.0d * sum / length;
            if (ab)
            {
                for (int n = start; n < end; ++n)
                {
                    out.setSample(n, (sign * hight) * (end - n - 1) / (end - start));
                }
            }
            else
            {
                for (int n = start; n < end; ++n)
                {
                    out.setSample(n, (sign * hight) * (n - start) / (end - start));
                }
            }
            ab = !ab;
            if (!(end < len)) break;
        }
        return out;
    }

    public static SFSignal makeSubOctave(SFSignal in)
    {
        int len = in.getLength();
        SFSignal out = SFData.build(len);
        int start = 0;
        int end = 0;
        int polarity = 1;
        int cross = 0;
        for (;;)
        {
            start = end;
            double sum = 0;
            while (cross++ < 2)
            {
                int sign = getSign(in.getSample(end));
                while (end < len && getSign(in.getSample(end)) == sign)
                {
                    sum += SFMaths.abs(in.getSample(end));
                    ++end;
                }
                if (!(end < len)) return out;
            }
            cross = 0;
            // start is the start of the wave
            // end is the end of the wave
            // sum is the area of the wave
            double length = end - start;
            double hight = sum / (length * 0.65);
            final double PI2 = SFMaths.PI * 2.0d;
            double f = 0.5 / length;
            for (double i = 0; i < length; ++i)
            {
                out.setSample((int) (i + start), (hight * polarity * SFMaths.sin(i * PI2 * f)));
            }
            /*
             * int middle = (int) (start + SFMaths.floor(length / 2)); int middlePoint = middle - start; if (middlePoint == 0) {
             * middlePoint = 1; } for (int n = start; n < middle; ++n) { out.setSample(n, (polarity * hight) * (n - start) /
             * middlePoint); } for (int n = middle; n < end; ++n) { out.setSample(n, (polarity * hight) * (end - n - 1) /
             * middlePoint); }
             */
            if (!(end < len)) break;
            polarity = -polarity;
        }
        return out;
    }

    public static SFSignal gate(SFSignal in, double threshold)
    {
        SFSignal ret = in.replicateEmpty();
        int length = in.getLength();
        for (int index = 0; index < length; ++index)
        {
            if (in.getSample(index) < threshold)
            {
                ret.setSample(index, 0);
            }
            else
            {
                ret.setSample(index, 1);
            }
        }
        return ret;
    }

    public static SFSignal threshold(SFSignal in, double threshold)
    {
        SFSignal ret = in.replicateEmpty();
        int length = in.getLength();
        for (int index = 0; index < length; ++index)
        {
            if (in.getSample(index) < threshold)
            {
                ret.setSample(index, -1);
            }
            else
            {
                ret.setSample(index, 1);
            }
        }
        return ret;
    }

    public static SFSignal shapedThreshold(SFSignal in, SFSignal shape) throws SFPL_RuntimeException
    {
        SFSignal ret = in.replicateEmpty();
        int length = in.getLength();
        if (length != shape.getLength()) throw new SFPL_RuntimeException(Messages.getString("SF_AmpAlgorithms.0")); //$NON-NLS-1$
        for (int index = 0; index < length; ++index)
        {
            double threshold = shape.getSample(index);
            if (in.getSample(index) < threshold)
            {
                ret.setSample(index, -1);
            }
            else
            {
                ret.setSample(index, 1);
            }
        }
        return ret;
    }
}
