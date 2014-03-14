/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * "file.wav",?myMixer:sf.playFile ... Computer makes a sound ...
 * 
 * @author AlexTu
 * 
 */
public class SF_LineWait implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        SFLineListener l = (SFLineListener) input;
        try
        {
            while (!l.hasStopped())
                Thread.sleep(10);
            return true;
        }
        catch (InterruptedException e)
        {
            throw new SFPL_RuntimeException(Messages.getString("cSFPL_SonicFieldLib.22"), e); //$NON-NLS-1$
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("cSFPL_SonicFieldLib.23"); //$NON-NLS-1$
    }

}