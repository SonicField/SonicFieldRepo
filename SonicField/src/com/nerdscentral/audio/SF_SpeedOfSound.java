/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * SF.SpeedOfSound Println ... 340.etc ...
 * 
 * @author AlexTu
 * 
 */
public class SF_SpeedOfSound implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        return SFConstants.SPEED_OF_SOUND * 1000;
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.29"); //$NON-NLS-1$
    }

}