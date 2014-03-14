/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.volume;

import com.nerdscentral.audio.SFData;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Clean implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try (SFData dataIn = SFData.realise(Caster.makeSFSignal(input)); SFData dataOut = dataIn.replicateEmpty();)
        {
            int length = decimateFilter(dataIn, dataOut);
            // remove DC
            double dc = 0;
            for (int i = 0; i < length; ++i)
            {
                dc += dataOut.getSample(i);
            }
            dc = dc / length;
            for (int i = 0; i < length; ++i)
            {
                dataOut.setSample(i, dataOut.getSample(i) - dc);
            }
            return Caster.prep4Ret(dataOut);
        }
    }

    static int decimateFilter(SFData dataIn, SFData dataOut)
    {
        // Decimate
        Decimator d0 = new Decimator();
        Decimator d1 = new Decimator();
        int length = dataIn.getLength();
        int offset = -9;
        for (int index = 0; index < length + 9; index += 2)
        {
            double x1, x2 = 0;
            int ip1 = index + 1;
            if (ip1 < length)
            {
                x1 = d0.Calc(dataIn.getSample(index), dataIn.getSample(ip1));
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
                x2 = d1.Calc(dataIn.getSample(ip1), dataIn.getSample(ip2));
            }
            else
            {
                if (ip1 < length)
                {
                    x2 = d1.Calc(dataIn.getSample(ip1), 0);
                }
                else
                {
                    x2 = d1.Calc(0, 0);
                }
            }

            if (offset >= 1)
            {
                dataOut.setSample(offset, x1);
                int index2 = offset + 1;
                if (index2 < length) dataOut.setSample(index2, x2);
            }
            offset += 2;
        }
        return length;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Clean.0"); //$NON-NLS-1$
    }

}