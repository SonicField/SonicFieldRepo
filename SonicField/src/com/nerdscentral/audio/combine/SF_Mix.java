/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1,-3:sf.ExponentialVolume !s1_normal... forwards an SFData less 3 db ...
 * 
 * @author AlexTu
 * 
 */
public class SF_Mix implements SFPL_Operator
{

    /**
     * 
     */
    private static final long     serialVersionUID = 1L;

    private final static SF_MixAt mixer            = new SF_MixAt();
    private final static Double   zero             = new Double(0);

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        // Defer to MixAt where all the optimisation for mixing can happen
        List<Object> il = Caster.makeBunch(input);
        ArrayList<ArrayList<Object>> out = new ArrayList<>(il.size());
        for (Object o : il)
        {
            SFSignal s = Caster.makeSFSignal(o);
            ArrayList<Object> pair = new ArrayList<>(2);
            pair.add(s);
            pair.add(zero);
            out.add(pair);
        }
        return mixer.Interpret(out);
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Add.3");  //$NON-NLS-1$
    }

}