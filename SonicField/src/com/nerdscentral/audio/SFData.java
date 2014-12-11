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

    // Swap limit in samples beyond which signals are send to swap
    private static final String                      SONIC_FIELD_SWAP_LIMIT = "sonicFieldSwapLimit";        //$NON-NLS-1$
    // Directory to send swap files to
    private static final String                      SONIC_FIELD_TEMP       = "sonicFieldTemp";             //$NON-NLS-1$
    // Default swap limit
    private static final int                         MAX_IN_RAM             = 2048 * 1024;
    private static final long                        serialVersionUID       = 1L;
    private final int                                length;
    private volatile boolean                         killed                 = false;
    private static File                              coreFile;
    private static RandomAccessFile                  coreFileAccessor;
    private static long                              chunkLen;
    private ByteBuffer[]                             chunks;
    private final static long                        swapLimit;
    private final static File                        tempDir;
    private static FileChannel                       channelMapper;
    private final NotCollectedException              javaCreated;
    private final String                             pythonCreated;
    private static ConcurrentLinkedDeque<ByteBuffer> freeChunks             = new ConcurrentLinkedDeque<>();

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
        tempDir = new File(System.getProperty(SONIC_FIELD_TEMP));
        String swapLimitraw = System.getProperty(SONIC_FIELD_SWAP_LIMIT);
        if (swapLimitraw == null)
        {
            swapLimit = MAX_IN_RAM; // 64 megabytes or 87 seconds at 96000
        }
        else
        {
            swapLimit = Long.parseLong(swapLimitraw);
        }
        chunkLen = swapLimit / 2;

        String pid = ManagementFactory.getRuntimeMXBean().getName();
        try
        {
            if (tempDir != null)
            {
                coreFile = File.createTempFile("SonicFieldSwap" + pid, ".mem", tempDir); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else
            {
                coreFile = File.createTempFile("SonicFieldSwap" + pid, ".mem");  //$NON-NLS-1$//$NON-NLS-2$            
            }
            coreFile.deleteOnExit();
            // Now create the actual file
            coreFileAccessor = new RandomAccessFile(coreFile, "rw"); //$NON-NLS-1$
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        channelMapper = coreFileAccessor.getChannel();
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
            countDown -= chunkLen;
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
            countDown -= chunkLen;
        }
        if (countDown > 0)
        {
            synchronized (coreFile)
            {
                long from = coreFile.length();
                while (countDown > 0)
                {
                    ByteBuffer chunk = channelMapper.map(MapMode.READ_WRITE, from, chunkLen);
                    chunk.order(ByteOrder.nativeOrder());
                    chunks[chunkCount] = chunk;
                    ++chunkCount;
                    from += chunkLen;
                    countDown -= chunkLen;
                }
            }
        }
    }

    @Override
    public void clear()
    {
        assert (chunkLen % 8 == 0);
        for (ByteBuffer chunk : chunks)
        {
            for (int l = 0; l < chunkLen; l += 8)
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
        long pos = bytePos % chunkLen;
        long bufPos = (bytePos - pos) / chunkLen;
        return chunks[(int) bufPos].getDouble((int) pos);
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#setSample(int, double)
     */
    @Override
    public final double setSample(int index, double value)
    {
        long bytePos = index * 8l;
        long pos = bytePos % chunkLen;
        long bufPos = (bytePos - pos) / chunkLen;
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
