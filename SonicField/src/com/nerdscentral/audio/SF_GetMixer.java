/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import javax.sound.sampled.Mixer;

import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * "usb audio" Sf.Mixer ... forwards a mixer ...
 * 
 * @author AlexTu
 * 
 */
public class SF_GetMixer implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        Mixer ret = SF2JavaSound.getMixer(input.toString());
        if (ret == null) throw new SFPL_RuntimeException(Messages.getString("cSFPL_SonicFieldLib.25") + input + " not found.");   //$NON-NLS-1$//$NON-NLS-2$
        return ret;
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.26"); //$NON-NLS-1$
    }

}