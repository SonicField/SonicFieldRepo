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
import com.nerdscentral.audio.core.SFConstants;
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
        private final File tmpFile;
        private boolean    swapped = true;
        private SFSignal   signal;
        private final int  len;

        protected Translate(SFSignal data) throws SFPL_RuntimeException
        {
            try
            {
                tmpFile = File.createTempFile(Messages.getString("SF_SwapSignal.1"), Messages.getString("SF_SwapSignal.2"), SFConstants.getLocalisedTempDir()); //$NON-NLS-1$ //$NON-NLS-2$
                tmpFile.deleteOnExit();
                System.out.println(Messages.getString("SF_SwapSignal.3") + tmpFile.getPath()); //$NON-NLS-1$
            }
            catch (IOException e)
            {
                throw new SFPL_RuntimeException(Messages.getString(Messages.getString("SF_SwapSignal.4")), e); //$NON-NLS-1$
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
                throw new SFPL_RuntimeException(Messages.getString(Messages.getString("SF_SwapSignal.5")), e); //$NON-NLS-1$
            }
        }

        @Override
        public double getSample(int index)
        {
            if (swapped)
            {
                synchronized (this)
                {
                    if (swapped)
                    {
                        System.out.println(Messages.getString("SF_SwapSignal.6") + tmpFile.getPath()); //$NON-NLS-1$

                        signal = SFData.build(len);
                        try (
                            FileInputStream fs = new FileInputStream(tmpFile);
                            DataInputStream ds = new DataInputStream(new BufferedInputStream(fs)))
                        {
                            if (len != ds.readInt()) throw new RuntimeException(Messages.getString("SF_SwapSignal.7")); //$NON-NLS-1$
                            for (int i = 0; i < len; ++i)
                            {
                                signal.setSample(i, ds.readDouble());
                            }
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(Messages.getString(Messages.getString("SF_SwapSignal.8")), e); //$NON-NLS-1$
                        }
                        tmpFile.delete();
                        swapped = false;
                    }
                }
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
        // TODO MAKE THIS WORK!
        SFSignal data = Caster.makeSFSignal(input);
        return data.flush();
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_SwapSignal.9"); //$NON-NLS-1$
    }

}
