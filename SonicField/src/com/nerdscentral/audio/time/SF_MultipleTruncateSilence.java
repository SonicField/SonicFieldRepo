package com.nerdscentral.audio.time;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_MultipleTruncateSilence implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_TruncateSilence.1"); //$NON-NLS-1$
    }

    // @SuppressWarnings("resource")
    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        List<Object> din = Caster.makeBunch(lin.get(0));
        List<SFSignal> sdin = new ArrayList<>(din.size());
        List<SFSignal> outList = new ArrayList<>();

        for (int c = 0; c < din.size(); ++c)
        {
            sdin.add(Caster.makeSFSignal(din.get(c)));
        }
        SFSignal data = sdin.get(0);

        double threshold = SFConstants.fromDBs(Caster.makeDouble(lin.get(1)));
        int len = (int) (Caster.makeDouble(lin.get(2)) * SFConstants.SAMPLE_RATE_MS);
        int inLen = data.getLength();
        List<List<double[]>> segments = new ArrayList<>();
        for (int c = 0; c < din.size(); ++c)
        {
            segments.add(new LinkedList<double[]>());
        }
        int outLen = 0;
        for (int index = 0; index < inLen;)
        {
            int diff = inLen - index;
            diff = diff > len ? len : diff;
            double max = 0;
            for (int inner = 0; inner < diff;)
            {
                double x;
                x = data.getSample(index);
                x = SFMaths.abs(x);
                if (x > max) max = x;
                ++inner;
                ++index;
            }
            if (max > threshold)
            {
                int channel = 0;
                double[] segment = new double[diff];
                for (List<double[]> channelSegs : segments)
                {
                    index -= diff;
                    SFSignal otherData = sdin.get(channel);
                    for (int inner = 0; inner < diff;)
                    {
                        segment[inner] = otherData.getSample(index);
                        ++inner;
                        ++index;
                    }
                    channelSegs.add(segment);
                }
            }
            outLen += diff;
        }
        for (int c = 0; c < segments.size(); ++c)
        {
            SFSignal nData = SFData.build(outLen, true);
            outList.add(nData);
        }
        int channel = 0;
        for (List<double[]> channelSegs : segments)
        {
            int index = 0;
            for (double[] segment : channelSegs)
            {
                SFSignal outData = outList.get(channel);
                for (int inner = 0; inner < segment.length;)
                {
                    outData.setSample(index, segment[inner]);
                    ++index;
                    ++inner;
                }
            }
            ++channel;
        }

        return outList;
    }
}
