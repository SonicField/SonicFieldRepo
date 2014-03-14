/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import java.util.List;

import javax.sound.sampled.Mixer;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * "file.wav",?myMixer:Sf.PlayFile ... Computer makes a sound ...
 * 
 * @author AlexTu
 * 
 */
public class SF_PlayFile implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try
        {
            return SF2JavaSound.playFile((String) l.get(0), (Mixer) l.get(1));
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("cSFPL_SonicFieldLib.6"), e); //$NON-NLS-1$
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.7"); //$NON-NLS-1$
    }

}