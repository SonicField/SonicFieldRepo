/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFGenerator;
import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_SwapSignal implements SFPL_Operator, SFPL_RefPassThrough
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static class Translate extends SFGenerator
    {
        private static final File DIRECTORY = new File("/Volumes/External1");
        private final File tmpFile;
        private boolean    swapped = true;
        private SFSignal   signal;
        private final int  len;

        protected Translate(SFSignal data) throws SFPL_RuntimeException
        {
            try
            {
                tmpFile = File.createTempFile("swap", ".mem", DIRECTORY);
                tmpFile.deleteOnExit();
                System.out.println("Swapping   to:" + tmpFile.getPath());
            }
            catch (IOException e)
            {
                throw new SFPL_RuntimeException(Messages.getString("Exception swapping out signal"), e);
            }
            len = data.getLength();
            try (
                FileOutputStream fs = new FileOutputStream(tmpFile);
                DataOutputStream ds = new DataOutputStream(new BufferedOutputStream(fs)))
            {
                ds.writeInt(data.getLength());
                for (int i = 0; i < data.getLength(); ++i)
                {
                    ds.writeDouble(data.getSample(i));
                }
            }
            catch (Exception e)
            {
                throw new SFPL_RuntimeException(Messages.getString("Exception swapping out signal"), e);
            }
        }

        @Override
        public double getSample(int index)
        {
            if (swapped)
            {
                System.out.println("Swapping from:" + tmpFile.getPath());

                signal = SFData.build(len);
                try (
                    FileInputStream fs = new FileInputStream(tmpFile);
                    DataInputStream ds = new DataInputStream(new BufferedInputStream(fs)))
                {
                    if (len != ds.readInt()) throw new RuntimeException("Length missmatch in swap in");
                    for (int i = 0; i < len; ++i)
                    {
                        signal.setSample(i, ds.readDouble());
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(Messages.getString("Exception swapping in signal"), e);
                }
                tmpFile.delete();
                swapped = false;
            }
            return signal.getSample(index);
        }

        @Override
        public int getLength()
        {
            return len;
        }

        @Override
        public void release()
        {
            // Pass
        }

        @Override
        public void finalize()
        {
            release();
        }

    }

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        SFSignal data = Caster.makeSFSignal(input);
        return new Translate(data);
    }

    @Override
    public String Word()
    {
        return "SwapSignal";
    }

}
