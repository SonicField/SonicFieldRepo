/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.core;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_SetSampleRate implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
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