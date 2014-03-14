/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch.algorithm;

public class SFInLineIIRFilter implements Cloneable
{

    final double[]                         xv;
    final double[]                         yv;
    final double                           gain;
    final int                              npoles;
    final SFFilterGenerator.NPoleFilterDef fd;

    public SFInLineIIRFilter(SFFilterGenerator.NPoleFilterDef fdIn, double gainIn)
    {
        fd = fdIn;
        xv = new double[fd.getPoles() + 1];
        yv = new double[fd.getPoles() + 1];
        this.gain = gainIn;
        npoles = fd.getPoles();
    }

    public double filterSample(double datum)
    {

        for (int i = 0; i < fd.getPoles(); ++i)
        {
            xv[i] = xv[i + 1];
            yv[i] = yv[i + 1];
        }
        xv[fd.getPoles()] = datum / gain;
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
        return q;
    }

    public SFInLineIIRFilter duplicate()
    {
        return new SFInLineIIRFilter(fd, gain);
    }
}
