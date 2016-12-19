/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator.NPoleFilterDef;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_NBandPassBessel extends SFNPoleFilterOperator implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_NBandPassBessel.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        SFSignal x = Caster.makeSFSignal(l.get(0));
        double frequencyA = Caster.makeDouble(l.get(1));
        double frequencyB = Caster.makeDouble(l.get(2));
        double order = Caster.makeDouble(l.get(3));
        if (order > 6) throw new SFPL_RuntimeException(Messages.getString("SFFilter_NBandPass.1") + ((int) order)); //$NON-NLS-1$
        NPoleFilterDef fd = SFFilterGenerator.computeBesselNBP(frequencyA, frequencyB, (int) order);
        SFSignal y = x.replicateEmpty();
        filterLoop(x, y, fd, fd.getGainfc());
        return y;
    }
}
