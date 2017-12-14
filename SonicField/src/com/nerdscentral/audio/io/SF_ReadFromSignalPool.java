/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ReadFromSignalPool implements SFPL_Operator, SFPL_RefPassThrough
{

    static class ReadStruct
    {
        DataInputStream              readStream = null;
        RandomAccessFile             readFile   = null;
        ClearableBufferedInputStream buffStream = null;
    }

    static class ClearableBufferedInputStream extends BufferedInputStream
    {

        public ClearableBufferedInputStream(InputStream ins)
        {
            super(ins);
        }

        public ClearableBufferedInputStream(InputStream ins, int size)
        {
            super(ins, size);
        }

        public void clear()
        {
            /** Clears the internal buffer */
            pos = 0;
            count = 0;
        }
    }

    private static ThreadLocal<HashMap<String, ReadStruct>> fileMap          = new ThreadLocal<HashMap<String, ReadStruct>>()
                                                                             {
                                                                                 @Override
                                                                                 protected HashMap<String, ReadStruct> initialValue()
                                                                                 {
                                                                                     return new HashMap<>();
                                                                                 }
                                                                             };

    /**
     * 
     */
    private static final long                               serialVersionUID = 1L;

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
        String fileName = Caster.makeString(inList.get(0));
        Long position = Caster.makeLong(inList.get(1));
        HashMap<String, ReadStruct> lmap = fileMap.get();
        ReadStruct readStruct = lmap.get(fileName);
        if (readStruct == null)
        {
            try
            {
                readStruct = lmap.get(fileName);
                if (readStruct == null)
                {
                    RandomAccessFile base = new RandomAccessFile(fileName, "r"); //$NON-NLS-1$
                    FileInputStream fs = new FileInputStream(base.getFD());
                    ClearableBufferedInputStream buffS = new ClearableBufferedInputStream(fs);
                    DataInputStream readStream = new DataInputStream(buffS);
                    readStruct = new ReadStruct();
                    readStruct.readFile = base;
                    readStruct.readStream = readStream;
                    readStruct.buffStream = buffS;
                    lmap.put(fileName, readStruct);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        try
        {
            readStruct.readFile.seek(position);
            int len = readStruct.readFile.readInt();
            SFData data = SFData.build(len, false);
            DataInputStream ds = readStruct.readStream;
            for (int i = 0; i < len; ++i)
            {
                data.setSample(i, ds.readDouble());
            }
            readStruct.buffStream.clear();
            return data;
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException("Error reading from signal pool: " + e.getMessage());
        }
    }

    @Override
    public String Word()
    {
        return "ReadFromSignalPool";
    }

}
