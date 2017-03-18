/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFGenerator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ReadSignal implements SFPL_Operator
{
    public static class Generator extends SFGenerator
    {
        private static final int       DBUFFER_LEN = 1024;
        private static final int       BBUFFER_LEN = 1024 * 8;
        private final RandomAccessFile file;
        private final int              length;
        private final double[]         buff;
        private long                   buffPos;
        private final byte[]           bbuff;

        protected Generator(RandomAccessFile fileIn)
        {
            file = fileIn;
            try
            {
                fileIn.seek(0);
                length = fileIn.readInt();
                buff = new double[DBUFFER_LEN];
                bbuff = new byte[BBUFFER_LEN];
                updateBuffer(0);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

        }

        private void updateBuffer(long index)
        {
            try
            {
                buffPos = index * 8l + 4l;
                ByteArrayInputStream bis = new ByteArrayInputStream(bbuff);
                DataInputStream dis = new DataInputStream(bis);
                long len = BBUFFER_LEN;
                if (file.getFilePointer() + BBUFFER_LEN > file.length())
                {
                    len = file.length() - file.getFilePointer();
                }
                file.seek(buffPos);
                file.read(bbuff, 0, (int) len);
                // System.err.println("Seeking " + buffPos + " len " + len);
                for (int i = 0; i < len / 8; ++i)
                {
                    buff[i] = dis.readDouble();
                }

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized double getSample(int index)
        {
            // TODO: Is simple synchronisation the best solution or would thread locality be a better bet?
            // We need to do something because two thread can be looking at different places entirely.
            // If that happens performance will completely suck anyhow and we _should_ convert to just
            // loading the whole thing into memory.
            if (index > length || index < 0)
            {
                throw new ArrayIndexOutOfBoundsException();
            }
            long toSeek = index;
            toSeek = toSeek * 8 + 4;
            toSeek -= buffPos;
            int off = (int) (toSeek / 8);
            // System.err.println("Off: " + off);
            try
            {
                return buff[off];
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                // System.err.println("Exception: " + e.getMessage());
                updateBuffer(index);
                toSeek = index;
                toSeek = toSeek * 8 + 4;
                toSeek -= buffPos;
                off = (int) (toSeek / 8);
                return buff[off];
            }
        }

        @Override
        public int getLength()
        {
            return length;
        }

        @Override
        public void release()
        {
            try
            {
                file.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("resource")
    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        String fileName = Caster.makeString(input);
        File file = new File(fileName);
        try
        {
            return new Generator(new RandomAccessFile(file, "r")); //$NON-NLS-1$
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_ReadSignal.0"); //$NON-NLS-1$
    }

}