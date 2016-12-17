/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_WriteFileString implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        String data = Caster.makeString(inList.get(0));
        String fileName = Caster.makeString(inList.get(1));
        File file = new File(fileName);
        try (PrintStream ps = new PrintStream(file);)
        {
            ps.print(data);
            return data;
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("SF_WriteFileString.1"), e);  //$NON-NLS-1$
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_WriteFileString.0"); //$NON-NLS-1$
    }

}