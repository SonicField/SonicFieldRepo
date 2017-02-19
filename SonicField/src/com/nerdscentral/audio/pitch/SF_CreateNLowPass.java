/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;




import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator.NPoleFilterDef;
import com.nerdscentral.audio.pitch.algorithm.SFInLineIIRFilter;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;


public class SF_CreateNLowPass implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_CreateNLowPass.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        double frequency = Caster.makeDouble(lin.get(0));
        int order = (int) (Caster.makeDouble(lin.get(1)));
        NPoleFilterDef fdIn = SFFilterGenerator.computeButterworthNLP(frequency, order);
        return new SFInLineIIRFilter(fdIn, fdIn.getGaindc());
    }

}
