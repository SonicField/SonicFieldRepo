/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
/**
 * 
 */
package com.nerdscentral.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final String         SOMIC_FIELD_SWAP_LIMIT = "sonicFieldSwapLimit";    //$NON-NLS-1$
    // Directory to send swap files to
    private static final String         SONIC_FIELD_TEMP       = "sonicFieldTemp";     //$NON-NLS-1$
    // Default swap limit
    private static final int            MAX_IN_RAM             = 8192 * 1024;
    private static final long           serialVersionUID       = 1L;
    private double[]                    data;
    private final int                   length;
    private volatile boolean            killed                 = false;
    private File                        coreFile;
    private RandomAccessFile            coreFileAccessor;
    private final long                  TWOGIG                 = 2097152;
    private List<ByteBuffer>            chunks                 = new ArrayList<>();
    private final static long           swapLimit;
    private final static File           tempDir;
    private FileChannel                 channelMapper;
    private final NotCollectedException javaCreated;
    private final String                pythonCreated;

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
        String swapLimitraw = System.getProperty(SOMIC_FIELD_SWAP_LIMIT);
        if (swapLimitraw == null)
        {
            swapLimit = MAX_IN_RAM; // 64 megabytes or 87 seconds at 96000
        }
        else
        {
            swapLimit = Long.parseLong(swapLimitraw);
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
        if (size > Integer.MAX_VALUE) throw new RuntimeException(Messages.getString("SFData.12")); //$NON-NLS-1$
        if (tempDir != null)
        {
            coreFile = File.createTempFile("SonicFieldSwap" + getUniqueId(), ".mem", tempDir);  //$NON-NLS-1$//$NON-NLS-2$
        }
        else
        {
            coreFile = File.createTempFile("SonicFieldSwap" + getUniqueId(), ".mem");  //$NON-NLS-1$//$NON-NLS-2$            
        }
        // This is a for testing - to avoid the disk filling up
        coreFile.deleteOnExit();
        // Now create the actual file
        coreFileAccessor = new RandomAccessFile(coreFile, "rw"); //$NON-NLS-1$
        channelMapper = coreFileAccessor.getChannel();
        long countDown = size;
        long from = 0;
        while (countDown > 0)
        {
            long len = Math.min(TWOGIG, countDown);
            ByteBuffer chunk = channelMapper.map(MapMode.READ_WRITE, from, len);
            chunks.add(chunk);
            from += len;
            countDown -= len;
        }
        System.gc();
    }

    @Override
    public void release()
    {
        data = null;
        chunks = null;
        if (coreFileAccessor != null)
        {
            try
            {
                coreFileAccessor.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        if (coreFile != null)
        {
            coreFile.delete();
        }
        resourceTracker.remove(this);
    }

    private SFData(double[] dataIn)
    {

        this.length = dataIn.length;
        data = dataIn;
        NotCollectedException nc = new NotCollectedException();
        nc.fillInStackTrace();
        pythonCreated = getPythonStack();
        resourceTracker.put(this, new ResTracker(nc, pythonCreated));
        javaCreated = nc;
    }

    private SFData(int lengthIn, boolean forceSwap)
    {
        if (!forceSwap && lengthIn <= swapLimit)
        {
            data = new double[lengthIn];
        }
        else
        {
            data = null;
            try
            {
                makeMap(lengthIn * 8);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        this.length = lengthIn;
        NotCollectedException nc = new NotCollectedException();
        nc.fillInStackTrace();
        pythonCreated = getPythonStack();
        resourceTracker.put(this, new ResTracker(nc, pythonCreated));
        javaCreated = nc;
    }

    public SFData(int length2)
    {
        this(length2, false);
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
        return new SFData(size, false);
    }

    public final static SFData build(int size, boolean forceSwap)
    {
        return new SFData(size, forceSwap);
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
        if (data != null)
        {
            return this.data[index];
        }
        while (true)
        {
            ByteBuffer byteBuffer = null;
            try
            {
                byteBuffer = setUpBuffer(index);
                return byteBuffer.getDouble();
            }
            catch (BufferUnderflowException b)
            {
                if (byteBuffer != null)
                {
                    System.err.println(Messages.getString("SFData.20") + index + Messages.getString("SFData.21") + byteBuffer.capacity() + " : " + byteBuffer.position()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
            }
        }
    }

    private ByteBuffer setUpBuffer(int index)
    {
        long bytePos = index * 8;
        long pos = bytePos % TWOGIG;
        long bufPos = (bytePos - pos) / TWOGIG;
        ByteBuffer byteBuffer = chunks.get((int) bufPos);
        byteBuffer.position((int) pos);
        return byteBuffer;
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#setSample(int, double)
     */
    @Override
    public final double setSample(int index, double value)
    {
        if (data != null)
        {
            return this.data[index] = value;
        }
        ByteBuffer byteBuffer = setUpBuffer(index);
        byteBuffer.putDouble(value);
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

    public static final SFData build(float[] input)
    {
        SFData data = new SFData(input.length);
        for (int i = 0; i < input.length; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    public static SFSignal build(double[] input)
    {
        SFSignal data = new SFData(input.length);
        for (int i = 0; i < input.length; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    public static SFSignal wrap(double[] input)
    {
        return new SFData(input);
    }

    public static final SFData build(double[] input, int j)
    {
        SFData data = new SFData(j);
        for (int i = 0; i < j; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    public double[] getData()
    {
        if (data != null) return data;
        System.err.println(Messages.getString("SFData.22")); //$NON-NLS-1$
        double[] ret = new double[length];
        for (int i = 0; i < length; ++i)
        {
            ret[i] = getSample(i);
        }
        return ret;
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

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#getDataInternalOnly()
     */
    @Override
    public double[] getDataInternalOnly()
    {
        return data;
    }

    public static SFData realiseSwap(SFSignal in)
    {
        if (in instanceof SFData && ((SFData) in).data == null) return (SFData) in;
        int len = in.getLength();
        SFData output = SFData.build(len, true);
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
}
