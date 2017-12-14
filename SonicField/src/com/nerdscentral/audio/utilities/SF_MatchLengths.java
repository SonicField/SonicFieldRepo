package com.nerdscentral.audio.utilities;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_MatchLengths implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return com.nerdscentral.audio.utilities.Messages.getString("SF_MatchLengths.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> in = Caster.makeBunch(input);
        int minLength = Integer.MAX_VALUE;
        for (Object ob : in)
        {
            SFSignal dataIn = Caster.makeSFSignal(ob);
            int len = dataIn.getLength();
            if (len < minLength)
            {
                minLength = len;
            }
        }

        ArrayList<SFSignal> out = new ArrayList<>();
        for (Object ob : in)
        {
            SFSignal dataIn = Caster.makeSFSignal(ob);
            SFSignal dataOut = dataIn;
            if (dataIn.getLength() != minLength)
            {
                dataOut = SFData.build(minLength, false);
                for (int i = 0; i < minLength; ++i)
                {
                    dataOut.setSample(i, dataIn.getSample(i));
                }
            }
            out.add(dataOut);
        }

        return out;
    }
}
