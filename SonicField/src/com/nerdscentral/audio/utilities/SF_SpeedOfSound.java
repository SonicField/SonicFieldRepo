/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFConstants;
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
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        return SFConstants.SPEED_OF_SOUND * 1000;
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.29"); //$NON-NLS-1$
    }

}