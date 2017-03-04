/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_WriteSignal implements SFPL_Operator, SFPL_RefPassThrough
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        /* Writes any SFSignal out to disk in signal format.
         * It takes arguments:
         * String: the file name (including the path).
         * SFSignal: the signal to write.
         * 
         * This method first writes to a file with an underscore appended.
         * Once the entire file has been written and flushed then it will rename to
         * the correct file name. This is to help ensure any file without the prepended
         * file name will not be a partial write and thus can be use in restarts etc.
         */
        List<Object> inList = Caster.makeBunch(input);
        SFSignal data = Caster.makeSFSignal(inList.get(0));
        String fileName = Caster.makeString(inList.get(1));
        String tmpName = fileName + "_"; // $NON-NLS-1$
        File file = new File(tmpName);
        try (
            FileOutputStream fs = new FileOutputStream(file);
            DataOutputStream ds = new DataOutputStream(new BufferedOutputStream(fs)))
        {
            ds.writeInt(data.getLength());
            for (int i = 0; i < data.getLength(); ++i)
            {
                ds.writeDouble(data.getSample(i));
            }
            ds.flush();
            java.nio.file.Files.move(Paths.get(tmpName), Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("SF_WriteSignal.1") + e.getMessage());  //$NON-NLS-1$
        }
        return data;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_WriteSignal.0"); //$NON-NLS-1$
    }

}
