/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator;

public class SFNPoleFilterOperator
{
    protected static void filterLoop(SFSignal x, SFSignal y, List<SFFilterGenerator.NPoleFilterDefListNode> fds)
    {

        int poles = fds.get(0).getDefinition().getPoles();
        double[] xv = new double[poles + 1];
        double[] yv = new double[poles + 1];
        SFFilterGenerator.NPoleFilterDefListNode fdl = fds.get(0);
        SFFilterGenerator.NPoleFilterDef fd = fdl.getDefinition();
        int pos = 0;
        double gain = fd.getGainfc();
        for (int n = 0; n < x.getLength(); ++n)
        {
            if (fdl.getPosition() == n)
            {
                ++pos;
                if (pos < fds.size())
                {
                    fdl = fds.get(pos);
                    fd = fdl.getDefinition();
                    gain = fd.getGainfc();
                }
            }
            for (int i = 0; i < poles; ++i)
            {
                xv[i] = xv[i + 1];
                yv[i] = yv[i + 1];
            }
            xv[fd.getPoles()] = x.getSample(n) / gain;
            double q = 0;
            for (int index = 0; index < xv.length; ++index)
            {
                q += xv[index] * fd.getXCeof(index);
                if (index < fd.getPoles())
                {
                    q += yv[index] * fd.getYCeof(index);
                }
            }
            yv[fd.getPoles()] = q;
            y.setSample(n, q);
        }
    }

    protected static void filterLoop(SFSignal x, SFSignal y, SFFilterGenerator.NPoleFilterDef fd, double gain)
    {
        double[] xv = new double[fd.getPoles() + 1];
        double[] yv = new double[fd.getPoles() + 1];

        for (int n = 0; n < x.getLength(); ++n)
        {
            for (int i = 0; i < fd.getPoles(); ++i)
            {
                xv[i] = xv[i + 1];
                yv[i] = yv[i + 1];
            }
            xv[fd.getPoles()] = x.getSample(n) / gain;
            double q = 0;
            for (int index = 0; index < xv.length; ++index)
            {
                q += xv[index] * fd.getXCeof(index);
                if (index < fd.getPoles())
                {
                    q += yv[index] * fd.getYCeof(index);
                }
            }
            yv[fd.getPoles()] = q;
            y.setSample(n, q);
        }
    }

    public SFNPoleFilterOperator()
    {
        super();
    }

}