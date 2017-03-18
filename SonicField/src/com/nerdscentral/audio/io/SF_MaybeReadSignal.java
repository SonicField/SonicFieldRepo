/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.File;
import java.util.ArrayList;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_MaybeReadSignal implements SFPL_Operator
{
    static final SF_ReadSignal reader           = new SF_ReadSignal();

    /**
     * 
     */
    private static final long  serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        // synchronized (SF_MaybeReadSignal.class)
        // {
        /* Takes a file name and looks for it in the restart directory.
         * If that is found then it will 
         * 
         */
        String fileName = Caster.makeString(input);
        int hash = fileName.hashCode() & 0xFF;
        String dirName = SFConstants.RESTART_DIRECTORY + File.separator + Integer.toHexString(hash);
        (new File(dirName)).mkdirs();
        String pathName = dirName + File.separator + fileName;

        ArrayList<Object> rets = new ArrayList<>();
        rets.add(pathName);
        if ((new File(pathName)).exists())
        {
            rets.add(((SFSignal) reader.Interpret(pathName)).realise());
        }
        else
        {
            rets.add(null);
        }
        return rets;
        // }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_MaybeReadSignal.0"); //$NON-NLS-1$
    }

}