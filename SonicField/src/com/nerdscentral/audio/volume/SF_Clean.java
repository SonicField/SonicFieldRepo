/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Clean implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        SFSignal sig = Caster.makeSFSignal(input);
        SFSignal dataIn = SFData.realise(sig);
        SFSignal dataOut = dataIn.replicateEmpty();
        decimateFilter(dataIn, dataOut);
        return dataOut;
    }

    public static double decimateFilter(SFSignal dataIn, SFSignal dataOut)
    {
        // Decimate
        Decimator d0 = new Decimator();
        Decimator d1 = new Decimator();
        int length = dataIn.getLength();
        int offset = -9;
        double max = 0;
        for (int index = 0; index < length + 9; index += 2)
        {
            double x1, x2, ip1Data = 0;
            int ip1 = index + 1;
            if (ip1 < length)
            {
                ip1Data = dataIn.getSample(ip1);
                x1 = d0.Calc(dataIn.getSample(index), ip1Data);
            }
            else
            {
                if (index < length)
                {
                    x1 = d0.Calc(dataIn.getSample(index), 0);
                }
                else
                {
                    x1 = d0.Calc(0, 0);
                }
            }
            int ip2 = ip1 + 1;
            if (ip2 < length)
            {
                x2 = d1.Calc(ip1Data, dataIn.getSample(ip2));
            }
            else
            {
                if (ip1 < length)
                {
                    x2 = d1.Calc(ip1Data, 0);
                }
                else
                {
                    x2 = d1.Calc(0, 0);
                }
            }

            if (offset >= 1)
            {
                dataOut.setSample(offset, x1);
                x1 = dataOut.getSample(offset);
                x1 = SFMaths.abs(x1);
                if (x1 > max)
                {
                    max = x1;
                }
                int index2 = offset + 1;
                if (index2 < length)
                {
                    dataOut.setSample(index2, x2);
                    x2 = dataOut.getSample(index2);
                    x2 = SFMaths.abs(x2);
                    if (x2 > max)
                    {
                        max = x2;
                    }
                }
            }
            offset += 2;
        }

        return max;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Clean.0"); //$NON-NLS-1$
    }

}