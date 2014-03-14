/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * "file.wav" Sf.ReadFile ... forwards a SFData object ...
 * 
 * @author AlexTu
 * 
 */
public class SF_ReadFile implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        try
        {
            return SF2JavaSound.readFile((String) input);
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("cSFPL_SonicFieldLib.9"), e); //$NON-NLS-1$
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.10"); //$NON-NLS-1$
    }

}