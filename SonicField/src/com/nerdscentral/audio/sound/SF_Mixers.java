/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.sound;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * Sf.Mixers,{,",":StrCat Print}:ListInvoke ... USB Audio,Microsoft ... integer of lesser magnitude it.
 * 
 * @author AlexTu
 * 
 */
public class SF_Mixers implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        return SF2JavaSound.getMixers();
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.4"); //$NON-NLS-1$
    }

}