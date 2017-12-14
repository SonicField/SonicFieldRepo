/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;

import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_WriteToSignalPool implements SFPL_Operator, SFPL_RefPassThrough
{

    static class WriteStruct
    {
        DataOutputStream writeStream = null;
        RandomAccessFile writeFile   = null;
    }

    private static ThreadLocal<HashMap<String, WriteStruct>> fileMap          = new ThreadLocal<HashMap<String, WriteStruct>>()
                                                                              {
                                                                                  @Override
                                                                                  protected HashMap<String, WriteStruct> initialValue()
                                                                                  {
                                                                                      return new HashMap<>();
                                                                                  }
                                                                              };

    /**
     * 
     */
    private static final long                                serialVersionUID = 1L;

    @SuppressWarnings("resource")
    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {

        /* Writes any SFSignal out to disk in signal format at the end of
         * the signal file so that one file can manage many signals. It will
         * cache an open file handle to the pool which leaks file handles but
         * is efficient for writing a log of signals. Will return the position 
         * from which to read the signal. The purpose is to allow efficient disk
         * caches.
         * 
         * It takes arguments:
         * String: the file name (including the path).
         * SFSignal: the signal to write.
         * 
         * The resulting file is not designed to persist between invocations; it is only
         * designed to be stable within one process.
         */
        List<Object> inList = Caster.makeBunch(input);
        SFSignal data = Caster.makeSFSignal(inList.get(0));
        String fileName = Caster.makeString(inList.get(1));
        HashMap<String, WriteStruct> lmap = fileMap.get();
        WriteStruct writeStruct = lmap.get(fileName);
        if (writeStruct == null)
        {
            try
            {

                RandomAccessFile base = new RandomAccessFile(fileName, "rw"); //$NON-NLS-1$
                FileOutputStream fs = new FileOutputStream(base.getFD());
                DataOutputStream writeStream = new DataOutputStream(new BufferedOutputStream(fs));
                writeStruct = new WriteStruct();
                writeStruct.writeFile = base;
                writeStruct.writeStream = writeStream;
                lmap.put(fileName, writeStruct);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        long writePosition = 0;
        // Synchronously get the end of the file and then seek enough to add the entire
        // new section then write a byte of zero at that point to extend the file. This is
        // a quick way to ensure we reserve the space and then can get out of the syc.
        synchronized (SF_WriteToSignalPool.class)
        {
            try
            {
                writePosition = writeStruct.writeFile.length();
                writeStruct.writeFile.seek(writePosition + 4 + data.getLength() * 8);
                writeStruct.writeFile.writeByte(0);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        try
        {
            writeStruct.writeFile.seek(writePosition);
            writeStruct.writeFile.writeInt(data.getLength());
            DataOutputStream ds = writeStruct.writeStream;
            for (int i = 0; i < data.getLength(); ++i)
            {
                ds.writeDouble(data.getSample(i));
            }
            writeStruct.writeStream.flush();
            return writePosition;
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException("Error writing to signal pool: " + e.getMessage());
        }
    }

    @Override
    public String Word()
    {
        return "WriteToSignalPool";
    }

}
