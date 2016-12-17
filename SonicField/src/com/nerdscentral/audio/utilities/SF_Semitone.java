/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * SF.Semitone Println ... twelth root of two .etc ...
 * 
 * @author AlexTu
 * 
 */
public class SF_Semitone implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        return SFConstants.TWELTH_ROOT_TWO;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SFPLSonicFieldLib.3"); //$NON-NLS-1$
    }

}