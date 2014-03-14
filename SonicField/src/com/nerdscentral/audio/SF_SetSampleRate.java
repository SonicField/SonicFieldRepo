/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_SetSampleRate implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        SFConstants.setSampleRate(Caster.makeDouble(input));
        return input;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_SetSampleRate.0"); //$NON-NLS-1$
    }

}