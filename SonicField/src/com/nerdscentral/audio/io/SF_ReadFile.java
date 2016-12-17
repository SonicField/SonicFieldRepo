/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.sound.SF2JavaSound;
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
    public Object Interpret(final Object input) throws SFPL_RuntimeException
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