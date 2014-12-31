/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
/**
 * 
 */
package com.nerdscentral.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * @author a1t
 * 
 */
public class SFData extends SFSignal implements Serializable
{

    public static class NotCollectedException extends Exception
    {
        private static final long serialVersionUID = 1L;
    }

    // Directory to send swap files to
    private static final String                      SONIC_FIELD_TEMP = "sonicFieldTemp";             //$NON-NLS-1$
    private static long                              CHUNK_LEN        = 1024 * 1024;
    private static final long                        serialVersionUID = 1L;
    private final int                                length;
    private volatile boolean                         killed           = false;
    private static File[]                            coreFile;
    private static RandomAccessFile[]                coreFileAccessor;
    private ByteBuffer[]                             chunks;
    private static FileChannel[]                     channelMapper;
    private static int                               fileRoundRobbin  = 0;
    private final NotCollectedException              javaCreated;
    private final String                             pythonCreated;
    private static ConcurrentLinkedDeque<ByteBuffer> freeChunks       = new ConcurrentLinkedDeque<>();

    private static class ResTracker
    {
        public ResTracker(NotCollectedException javaCreated, String pythonCreated)
        {
            j = javaCreated;
            p = pythonCreated;
        }

        NotCollectedException j;
        String                p;
    }

    private static ConcurrentHashMap<SFData, ResTracker> resourceTracker = new ConcurrentHashMap<>();

    static
    {
        File tempDir[];
        String tempEnv = System.getProperty(SONIC_FIELD_TEMP);
        String[] tdNames = null;
        if (tempEnv == null)
        {
            tdNames = new String[] { System.getProperty("java.io.tmpdir") }; //$NON-NLS-1$
        }
        else
        {
            tdNames = tempEnv.split(","); //$NON-NLS-1$
        }
        int nTemps = tdNames.length;
        tempDir = new File[nTemps];
        coreFile = new File[nTemps];
        coreFileAccessor = new RandomAccessFile[nTemps];
        channelMapper = new FileChannel[nTemps];
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        int index = 0;
        for (String tdName : tdNames)
        {
            tempDir[index] = new File(tdName);
            try
            {
                coreFile[index] = File.createTempFile("SonicFieldSwap" + pid, ".mem", tempDir[index]);  //$NON-NLS-1$//$NON-NLS-2$            
                coreFile[index].deleteOnExit();
                // Now create the actual file
                coreFileAccessor[index] = new RandomAccessFile(coreFile[index], "rw"); //$NON-NLS-1$
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            channelMapper[index] = coreFileAccessor[index].getChannel();
            ++index;
        }
    }

    public static void dumpNotCollected()
    {
        if (resourceTracker.size() != 0)
        {
            System.out.println(Messages.getString("SFData.14")); //$NON-NLS-1$
            System.out.println(Messages.getString("SFData.15")); //$NON-NLS-1$
            for (Entry<SFData, ResTracker> x : resourceTracker.entrySet())
            {
                printResourceError(Messages.getString("SFData.19"), x.getValue()); //$NON-NLS-1$
            }
        }
    }

    private void makeMap(long size) throws IOException
    {
        long countDown = size;
        int chunkCount = 0;
        while (countDown > 0)
        {
            ++chunkCount;
            countDown -= CHUNK_LEN;
        }

        countDown = size;
        chunks = new ByteBuffer[chunkCount];
        chunkCount = 0;
        while (countDown > 0)
        {
            ByteBuffer chunk = freeChunks.poll();
            if (chunk == null) break;
            chunks[chunkCount] = chunk;
            ++chunkCount;
            countDown -= CHUNK_LEN;
        }
        if (countDown > 0)
        {
            synchronized (coreFile)
            {
                while (countDown > 0)
                {
                    long from = coreFile[fileRoundRobbin].length();
                    ByteBuffer chunk = channelMapper[fileRoundRobbin].map(MapMode.READ_WRITE, from, CHUNK_LEN);
                    chunk.order(ByteOrder.nativeOrder());
                    chunks[chunkCount] = chunk;
                    ++chunkCount;
                    from += CHUNK_LEN;
                    countDown -= CHUNK_LEN;
                    if (++fileRoundRobbin >= coreFile.length) fileRoundRobbin = 0;
                }
            }
        }
    }

    @Override
    public void clear()
    {
        assert (CHUNK_LEN % 8 == 0);
        for (ByteBuffer chunk : chunks)
        {
            for (int l = 0; l < CHUNK_LEN; l += 8)
            {
                chunk.putLong(l, 0);
            }
        }
    }

    @Override
    public void release()
    {
        for (ByteBuffer chunk : chunks)
        {
            freeChunks.push(chunk);
        }
        chunks = null;
        resourceTracker.remove(this);
    }

    private SFData(int lengthIn)
    {
        try
        {
            if (lengthIn > Integer.MAX_VALUE) throw new RuntimeException(Messages.getString("SFData.12") + ": " + lengthIn); //$NON-NLS-1$ //$NON-NLS-2$

            makeMap(lengthIn * 8l);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        // }
        this.length = lengthIn;
        NotCollectedException nc = new NotCollectedException();
        nc.fillInStackTrace();
        pythonCreated = getPythonStack();
        resourceTracker.put(this, new ResTracker(nc, pythonCreated));
        javaCreated = nc;
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#isKilled()
     */
    @Override
    public boolean isKilled()
    {
        return killed;
    }

    public final static SFData build(int size)
    {
        return new SFData(size);
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#replicate()
     */
    @Override
    public final SFData replicate()
    {
        SFData data1 = new SFData(this.getLength());
        for (int i = 0; i < this.getLength(); ++i)
        {
            data1.setSample(i, this.getSample(i));
        }
        return data1;
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#getSample(int)
     */
    @Override
    public final double getSample(int index)
    {
        long bytePos = index * 8l;
        long pos = bytePos % CHUNK_LEN;
        long bufPos = (bytePos - pos) / CHUNK_LEN;
        return chunks[(int) bufPos].getDouble((int) pos);
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#setSample(int, double)
     */
    @Override
    public final double setSample(int index, double value)
    {
        long bytePos = index * 8l;
        long pos = bytePos % CHUNK_LEN;
        long bufPos = (bytePos - pos) / CHUNK_LEN;
        chunks[(int) bufPos].putDouble((int) pos, value);
        return value;
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#getLength()
     */
    @Override
    public final int getLength()
    {
        return this.length;
    }

    public static final SFSignal build(float[] input)
    {
        SFSignal data = SFData.build(input.length);
        for (int i = 0; i < input.length; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    public static SFSignal build(double[] input)
    {
        SFSignal data = SFData.build(input.length);
        for (int i = 0; i < input.length; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    public static final SFData build(double[] input, int j)
    {
        SFData data = SFData.build(j);
        for (int i = 0; i < j; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#setAt(int, com.nerdscentral.audio.SFData)
     */
    @Override
    public void setAt(int pos, SFSignal data2) throws SFPL_RuntimeException
    {
        int pos2 = pos;
        if (pos2 + data2.getLength() > length)
        {
            System.out.println(Messages.getString("SFData.9") + pos2 + Messages.getString("SFData.10") + data2.getLength() + Messages.getString("SFData.11") + length); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            throw new SFPL_RuntimeException(Messages.getString("SFData.0")); //$NON-NLS-1$
        }
        int end = pos2 + data2.getLength();
        for (int index = pos2; index < end; ++index)
        {
            setSample(index, data2.getSample(index - pos2));
        }
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#setFrom(int, com.nerdscentral.audio.SFData)
     */
    @Override
    public void setFrom(int pos, SFSignal data2) throws SFPL_RuntimeException
    {
        int pos2 = pos;
        if (pos2 + length > data2.getLength()) throw new SFPL_RuntimeException(Messages.getString("SFData.1")); //$NON-NLS-1$
        for (int index = 0; index < length; ++index)
        {
            setSample(index, data2.getSample(index - pos2));
        }
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#toString()
     */

    @Override
    public String toString()
    {
        return Messages.getString("SFData.2") + length + Messages.getString("SFData.3"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static SFData realiseSwap(SFSignal in)
    {
        if (in instanceof SFData) return (SFData) in;
        int len = in.getLength();
        SFData output = SFData.build(len);
        for (int i = 0; i < len; ++i)
        {
            output.setSample(i, in.getSample(i));
        }
        return output;
    }

    public static SFData realise(SFSignal in)
    {
        if (in instanceof SFData) return (SFData) in;
        int len = in.getLength();
        SFData output = SFData.build(len);
        for (int i = 0; i < len; ++i)
        {
            output.setSample(i, in.getSample(i));
        }
        return output;
    }

    @Override
    public void finalize()
    {
        if (referenceCount.get() != 0)
        {
            System.err.println(Messages.getString("SFData.13")); //$NON-NLS-1$
            System.err.println(pythonCreated);
            javaCreated.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#close()
     */
    @Override
    public void close() throws RuntimeException
    {
        try
        {
            super.close();
        }
        catch (RuntimeException e)
        {
            printResourceError(Messages.getString("SFData.16"), new ResTracker(this.javaCreated, this.pythonCreated)); //$NON-NLS-1$
            throw e;
        }
    }

    private static void printResourceError(String message, ResTracker data)
    {
        System.out.println(message);
        System.out.println(Messages.getString("SFData.17")); //$NON-NLS-1$
        for (StackTraceElement x : data.j.getStackTrace())
        {
            if (x.getClassName().contains(".reflect.")) break; //$NON-NLS-1$
            System.out.println(x.getClassName() + "!" + x.getFileName() + ":" + x.getLineNumber()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        System.out.println(Messages.getString("SFData.18")); //$NON-NLS-1$
        System.out.println(data.p);
    }

    @Override
    public double[] getDataInternalOnly()
    {
        double[] ret = new double[getLength()];
        int len = getLength();
        for (int index = 0; index < len; ++len)
        {
            ret[index] = getSample(index);
        }
        return ret;
    }
}
