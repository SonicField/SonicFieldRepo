/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFMultipleTranslator;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Divide implements SFPL_Operator
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

            double div = getInputSample(1, index, 1);
            if (SFMaths.abs(div) < Double.MIN_NORMAL)
            {
                div = div < 0 ? -Double.MIN_NORMAL : Double.MIN_NORMAL;
            }
            return getInputSample(0, index) / div;
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Divide.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        List<SFSignal> list = new ArrayList<>(2);
        list.add(Caster.makeSFSignal(l.get(0)));
        list.add(Caster.makeSFSignal(l.get(1)));
        Translator ret = new Translator(list);
        return ret;
    }
}
