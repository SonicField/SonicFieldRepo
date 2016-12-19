/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFMultipleTranslator;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Multiply implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    class Translator extends SFMultipleTranslator
    {

        protected Translator(List<SFSignal> input)
        {
            super(input);
        }

        @Override
        public double getSample(int index)
        {
            return getInputSample(0, index) * getInputSample(1, index, 1);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Multiply.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        List<SFSignal> list = new ArrayList<>(2);
        list.add(Caster.makeSFSignal(l.get(0)));
        list.add(Caster.makeSFSignal(l.get(1)));
        if (list.get(0).isRealised() && list.get(1).isRealised())
        {
            SFData data = (SFData) list.get(0).replicate();
            data.operateOnto(0, list.get(1), SFData.OPERATION.MULTIPLY);
            return data;
        }
        Translator ret = new Translator(list);
        return ret;
    }

}
