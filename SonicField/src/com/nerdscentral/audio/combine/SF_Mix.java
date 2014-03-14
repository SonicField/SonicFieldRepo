/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.SFMultipleTranslator;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1,-3:Sf.Volume !s1_normal... forwards an SFData less 3 db ...
 * 
 * @author AlexTu
 * 
 */
public class SF_Mix implements SFPL_Operator
{
    static class Translator extends SFMultipleTranslator
    {

        protected Translator(List<SFSignal> input)
        {
            super(input);
        }

        @Override
        public double getSample(int index)
        {
            double d = 0;
            for (int mindex = 0; mindex < getNMembers(); ++mindex)
            {
                d += getInputSample(mindex, index);
            }
            return d;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        List<SFSignal> incomming = new ArrayList<>();
        for (Object each : inList)
        {
            try (SFSignal data = Caster.makeSFSignal(each);)
            {
                incomming.add(data);
                data.incrReference();
            }
        }
        Translator ret = new Translator(incomming);
        for (SFSignal x : incomming)
        {
            x.decrReference();
        }
        return ret;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Add.3");  //$NON-NLS-1$
    }

}