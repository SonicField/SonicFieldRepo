/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.amps;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * Implementation of a pulse width modulator which takes an numeric threshold shape T and when the signal is >T it outputs 1 and
 * when it is lower then T it outputs 0. Used with a saw tooth signal will give a classing pulse width modulator
 * 
 * @author alexander
 * 
 */
public class SF_ShapedThreshold implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ShapedThreshold.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> bunch = Caster.makeBunch(input);
        SFSignal data1 = Caster.makeSFSignal(bunch.get(0));
        SFSignal data2 = Caster.makeSFSignal(bunch.get(1));
        return SFAmpAlgorithms.shapedThreshold(data1, data2);

    }

}
