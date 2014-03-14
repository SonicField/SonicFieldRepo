/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import java.util.List;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * (?left,?right:),"temp.wav":Sf.WriteFile16 ... converts SFData channels to a 16 bit wav file a SFConstants.SAMPLE_RATE sps ...
 * 
 * @author AlexTu
 * 
 */
public class SF_WriteFile16 implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        List<Object> channels = Caster.makeBunch(inList.get(0));
        String fileName = (String) inList.get(1);
        try
        {
            SF2JavaSound.WriteWav(fileName, channels, false);
            return channels;
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("cSFPL_SonicFieldLib.14"), e); //$NON-NLS-1$
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.15"); //$NON-NLS-1$
    }

}