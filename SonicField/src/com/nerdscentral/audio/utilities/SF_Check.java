/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Check implements SFPL_Operator, SFPL_RefPassThrough
{

    public SF_Check()
    {
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SFPLSonicFieldLib.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        boolean ok = true;
        SFSignal in = Caster.makeSFSignal(input);
        int len = in.getLength();

        for (int i = 0; i < len; ++i)
        {
            double q = in.getSample(i);
            if (Double.isInfinite(q) || Double.isNaN(q))
            {
                System.err.println(Messages.getString("SF_Check.0") + i + Messages.getString("SF_Check.1") + Double.isFinite(q) //$NON-NLS-1$ //$NON-NLS-2$
                                + Messages.getString("SF_Check.2") + Double.isNaN(q));  //$NON-NLS-1$
                ok = false;
                break;
            }
        }
        return ok;
    }
}