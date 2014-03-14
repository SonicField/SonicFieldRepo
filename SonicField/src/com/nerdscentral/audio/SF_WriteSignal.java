/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_WriteSignal implements SFPL_Operator, SFPL_RefPassThrough
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        try (SFSignal data = Caster.makeSFSignal(inList.get(0));)
        {
            String fileName = Caster.makeString(inList.get(1));
            File file = new File(fileName);
            try (
                FileOutputStream fs = new FileOutputStream(file);
                DataOutputStream ds = new DataOutputStream(new BufferedOutputStream(fs)))
            {
                ds.writeInt(data.getLength());
                for (int i = 0; i < data.getLength(); ++i)
                {
                    ds.writeDouble(data.getSample(i));
                }
                ds.close();
            }
            catch (Exception e)
            {
                throw new SFPL_RuntimeException(Messages.getString("SF_WriteSignal.1"), e);  //$NON-NLS-1$
            }
            return Caster.prep4Ret(data);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_WriteSignal.0"); //$NON-NLS-1$
    }

}