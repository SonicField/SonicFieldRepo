/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;


public class SF_ValueAt implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ValueAt.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        @SuppressWarnings("unchecked")
        List<Object> in = (List<Object>) input;
        double index = Caster.makeDouble(in.get(1)) * SFConstants.SAMPLE_RATE_MS;
        return (Caster.makeSFSignal(in.get(0))).getSampleCubic(index);
    }
}