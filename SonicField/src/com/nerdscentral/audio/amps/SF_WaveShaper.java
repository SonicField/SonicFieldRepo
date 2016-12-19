/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.amps;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_WaveShaper implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    static class Translator extends SFSingleTranslator
    {
        final double e0, e1, e2, e3, e4, e5;

        protected Translator(SFSignal input, double d0, double d1, double d2, double d3, double d4, double d5)
        {
            super(input);
            e0 = d0;
            e1 = d1;
            e2 = d2;
            e3 = d3;
            e4 = d4;
            e5 = d5;
        }

        @Override
        public double getSample(int index)
        {
            double din = getInputSample(index);
            double d1 = din;
            double dout = din * e5;
            din *= d1; // 2
            dout += din * e4;
            din *= d1; // 3
            dout += din * e3;
            din *= d1; // 4
            dout += din * e2;
            din *= d1; // 5
            dout += din * e1;
            din *= d1; // 6
            dout += din * e0;
            return dout;
        }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_WaveShaper.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> in = Caster.makeBunch(input);
        if (in.size() != 7) throw new SFPL_RuntimeException(Messages.getString("SF_WaveShaper.1")); //$NON-NLS-1$
        double[] coefficients = new double[6];
        for (int i = 0; i < 6; ++i)
        {
            coefficients[i] = Caster.makeDouble(in.get(i));
        }
        SFSignal data = Caster.makeSFSignal(in.get(6));
        // this replicates for us
        double e0 = coefficients[0];
        double e1 = coefficients[1];
        double e2 = coefficients[2];
        double e3 = coefficients[3];
        double e4 = coefficients[4];
        double e5 = coefficients[5];
        return new Translator(data, e0, e1, e2, e3, e4, e5);
    }
}
