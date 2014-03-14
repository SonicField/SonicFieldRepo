/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.amps;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * Implementation of a pulse width modulator which takes an numeric threshold T and when the signal is >T it outputs 1 and when
 * it is lower then T it outputs 0. Used with a saw tooth singal will give a classing pulse width modulator
 * 
 * @author alexander
 * 
 */
public class SF_Threshold implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Threshold.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> bunch = Caster.makeBunch(input);
        double data2 = Caster.makeDouble(bunch.get(1));
        try (SFSignal data1 = Caster.makeSFSignal(bunch.get(0)); SFSignal out = SFAmpAlgorithms.threshold(data1, data2);)
        {
            return Caster.prep4Ret(out);
        }
    }

}
