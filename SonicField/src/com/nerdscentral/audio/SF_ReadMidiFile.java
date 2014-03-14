/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.audio.io.MidiFunctions;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ReadMidiFile implements SFPL_Operator
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
            return MidiFunctions.readMidiFile(Caster.makeString(input));
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("SF_ReadMidiFile.0"), e); //$NON-NLS-1$
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_ReadMidiFile.1"); //$NON-NLS-1$
    }

}