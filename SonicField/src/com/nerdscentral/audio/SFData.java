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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import sun.misc.Unsafe;

import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * @author a1t
 * 
 */
public class SFData extends SFSignal implements Serializable
{

    private static Unsafe getUnsafe()
    {
        try
        {
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); //$NON-NLS-1$
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static final Unsafe unsafe = getUnsafe();

    private static class ByteBufferWrapper
    {
        public long       address;
        @SuppressWarnings("unused")
        // buffer is used to hold a reference to the buffer
        // but its data is read via unsafe
        public ByteBuffer buffer;

        public ByteBufferWrapper(ByteBuffer b) throws NoSuchMethodException, SecurityException, IllegalAccessException,
                        IllegalArgumentException, InvocationTargetException
        {
            Method addM = b.getClass().getMethod("address"); //$NON-NLS-1$
            addM.setAccessible(true);
            address = (long) addM.invoke(b);
            buffer = b;
        }
    }

    public static class NotCollectedException extends Exception
    {
        private static final long serialVersionUID = 1L;
    }

    // Directory to send swap files to
    private static final String                             SONIC_FIELD_TEMP = "sonicFieldTemp";               //$NON-NLS-1$
    private static final long                               CHUNK_SHIFT      = 16;
    private static final long                               CHUNK_LEN        = (long) Math.pow(2, CHUNK_SHIFT);
    private static final long                               CHUNK_MASK       = CHUNK_LEN - 1;
    private static final long                               serialVersionUID = 1L;
    private final int                                       length;
    private volatile boolean                                killed           = false;
    private static File[]                                   coreFile;
    private static RandomAccessFile[]                       coreFileAccessor;
    private ByteBufferWrapper[]                             chunks;
    private static FileChannel[]                            channelMapper;
    private static int                                      fileRoundRobbin  = 0;
    private final NotCollectedException                     javaCreated;
    private final String                                    pythonCreated;
    private final long                                      chunkIndex;
    // shadows chunkIndex for memory management as chunkIndex must
    // be final to enable optimisation
    private long                                            allocked;
    private static ConcurrentLinkedDeque<ByteBufferWrapper> freeChunks       = new ConcurrentLinkedDeque<>();

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

    private final static ConcurrentHashMap<SFData, ResTracker> resourceTracker = new ConcurrentHashMap<>();

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

    private void makeMap(long size) throws IOException, SecurityException, IllegalArgumentException, IllegalAccessException,
                    InvocationTargetException, NoSuchMethodException
    {
        long countDown = size;
        int chunkCount = 0;
        while (countDown > 0)
        {
            ++chunkCount;
            countDown -= CHUNK_LEN;
        }

        countDown = size;
        chunks = new ByteBufferWrapper[chunkCount];
        chunkCount = 0;
        while (countDown > 0)
        {
            ByteBufferWrapper chunk = freeChunks.poll();
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
                    ByteBuffer chunk = channelMapper[fileRoundRobbin].map(MapMode.READ_WRITE, from, CHUNK_LEN << 3l);
                    chunk.order(ByteOrder.nativeOrder());
                    chunks[chunkCount] = new ByteBufferWrapper(chunk);
                    ++chunkCount;
                    countDown -= CHUNK_LEN;
                    if (++fileRoundRobbin >= coreFile.length) fileRoundRobbin = 0;
                }
            }
        }
        // now we have the chunks we get the address of the underlying memory
        // of each and place that in the off heap lookup so we no longer reference
        // them via objects but purely as raw memory
        long offSet = 0;
        for (ByteBufferWrapper chunk : chunks)
        {
            unsafe.putAddress(chunkIndex + offSet, chunk.address);
            offSet += 8;
        }
    }

    @Override
    public void clear()
    {
        for (long i = 0; i < chunks.length; ++i)
        {
            long address = unsafe.getAddress(chunkIndex + (i << 3l));
            unsafe.setMemory(address, CHUNK_LEN << 3l, (byte) 0);
        }
    }

    @Override
    public void release()
    {
        for (ByteBufferWrapper chunk : chunks)
        {
            freeChunks.push(chunk);
        }
        chunks = null;
        resourceTracker.remove(this);
        synchronized (unsafe)
        {
            if (allocked != 0) unsafe.freeMemory(chunkIndex);
            allocked = 0;
        }
    }

    private SFData(int l)
    {
        allocked = 0;
        try
        {
            try
            {
                // Allocate the memory for the index - final so do it here
                long size = (1 + (l >> CHUNK_SHIFT)) << 3;
                allocked = chunkIndex = unsafe.allocateMemory(size);
                if (allocked == 0)
                {
                    throw new RuntimeException("Out of memory allocating " + size); //$NON-NLS-1$
                }
                makeMap(l);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            this.length = l;
            NotCollectedException nc = new NotCollectedException();
            nc.fillInStackTrace();
            pythonCreated = getPythonStack();
            resourceTracker.put(this, new ResTracker(nc, pythonCreated));
            javaCreated = nc;
        }
        catch (Throwable t)
        {
            if (allocked != 0)
            {
                unsafe.freeMemory(allocked);
                allocked = 0;
            }
            throw t;
        }
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#isKilled()
     */
    @Override
    public boolean isKilled()
    {
        return killed;
    }

    public final static SFData build(int l)
    {
        return new SFData(l);
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

    private long getAddress(long index)
    {
        long pos = index & CHUNK_MASK;
        long bufPos = index >> CHUNK_SHIFT;
        long address = chunkIndex + (bufPos << 3);
        // System.out.println("Index: " + index + " address: " + unsafe.getAddress(address) + " pos: " + pos);
        return unsafe.getAddress(address) + (pos << 3l);
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#getSample(int)
     */
    @Override
    public final double getSample(int index)
    {
        return unsafe.getDouble(getAddress(index));
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#setSample(int, double)
     */
    @Override
    public final double setSample(int index, double value)
    {
        unsafe.putDouble(getAddress(index), value);
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
        long end = pos2 + data2.getLength();
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
        long length = getLength();
        if (length > Integer.MAX_VALUE)
        {
            throw new RuntimeException(Messages.getString("SFData.5") + length); //$NON-NLS-1$
        }
        int len = (int) length;
        double[] ret = new double[len];
        for (int index = 0; index < len; ++len)
        {
            ret[index] = getSample(index);
        }
        return ret;
    }

    @Override
    public boolean isRealised()
    {
        return true;
    }

    private static long getAbsLen(SFData a, long aOffset, SFData b, long bOffset)
    {
        // Is there enough room irrespective of chunks

        // How much space left in b
        long absLen = b.getLength() - bOffset;

        // How much we can copy from a
        long wantLen = a.getLength() - aOffset;

        // abs len is the amount we could copy ignoring chunks
        if (wantLen < absLen) absLen = wantLen;

        // Find the end of the next chunks
        long aEnd = CHUNK_LEN + (aOffset & CHUNK_MASK);
        long bEnd = CHUNK_LEN + (bOffset & CHUNK_MASK);

        long maxChunkLen = aEnd > bEnd ? bEnd : aEnd;

        if (absLen > maxChunkLen)
        {
            absLen = maxChunkLen;
        }
        return absLen;
    }

    // Mixes a onto b for as long as that can be done without flipping chuncks
    // returns how many doubles it managed to mix, this can be call repeatedly
    // to then mix two entire buffers. If return is zero then the end of the
    // buffer has been reached.
    private static long addChunks(SFData a, long aOffset, SFData b, long bOffset)
    {
        long absLen = getAbsLen(a, aOffset, b, bOffset);

        // Not absLen is the longest continuous mix we can make from
        // a to b.
        long aAddr = a.getAddress(aOffset);
        long bAddr = b.getAddress(bOffset);
        for (long pos = 0; pos < absLen; pos += 8)
        {
            double aValue = unsafe.getDouble(aAddr + pos);
            long putAddr = bAddr + pos;
            double bValue = unsafe.getDouble(putAddr);
            // Due to no compile time polymorphism we have to duplicate
            // this method for each operator in the next line or suffer
            // virtual dispatch :|
            unsafe.putDouble(putAddr, aValue + bValue);
        }
        return absLen;
    }

    private static long copyChunks(SFData a, long aOffset, SFData b, long bOffset)
    {
        long absLen = getAbsLen(a, aOffset, b, bOffset);

        // Not absLen is the longest continuous mix we can make from
        // a to b.
        long aAddr = a.getAddress(aOffset);
        long bAddr = b.getAddress(bOffset);
        for (long pos = 0; pos < absLen; pos += 8)
        {
            double aValue = unsafe.getDouble(aAddr + pos);
            long putAddr = bAddr + pos;
            // Due to no compile time polymorphism we have to duplicate
            // this method for each operator in the next line or suffer
            // virtual dispatch :|
            unsafe.putDouble(putAddr, aValue);
        }
        return absLen;
    }

    private static long multiplyChunks(SFData a, long aOffset, SFData b, long bOffset)
    {
        long absLen = getAbsLen(a, aOffset, b, bOffset);

        // Not absLen is the longest continuous mix we can make from
        // a to b.
        long aAddr = a.getAddress(aOffset);
        long bAddr = b.getAddress(bOffset);
        for (long pos = 0; pos < absLen; pos += 8)
        {
            double aValue = unsafe.getDouble(aAddr + pos);
            long putAddr = bAddr + pos;
            double bValue = unsafe.getDouble(putAddr);
            // Due to no compile time polymorphism we have to duplicate
            // this method for each operator in the next line or suffer
            // virtual dispatch :|
            unsafe.putDouble(putAddr, aValue * bValue);
        }
        return absLen;
    }

    private static long divideChunks(SFData a, long aOffset, SFData b, long bOffset)
    {
        long absLen = getAbsLen(a, aOffset, b, bOffset);

        // Not absLen is the longest continuous mix we can make from
        // a to b.
        long aAddr = a.getAddress(aOffset);
        long bAddr = b.getAddress(bOffset);
        for (long pos = 0; pos < absLen; pos += 8)
        {
            double aValue = unsafe.getDouble(aAddr + pos);
            long putAddr = bAddr + pos;
            double bValue = unsafe.getDouble(putAddr);
            // Due to no compile time polymorphism we have to duplicate
            // this method for each operator in the next line or suffer
            // virtual dispatch :|
            unsafe.putDouble(putAddr, aValue / bValue);
        }
        return absLen;
    }

    private static long subtractChunks(SFData a, long aOffset, SFData b, long bOffset)
    {
        long absLen = getAbsLen(a, aOffset, b, bOffset);

        // Not absLen is the longest continuous mix we can make from
        // a to b.
        long aAddr = a.getAddress(aOffset);
        long bAddr = b.getAddress(bOffset);
        for (long pos = 0; pos < absLen; pos += 8)
        {
            double aValue = unsafe.getDouble(aAddr + pos);
            long putAddr = bAddr + pos;
            double bValue = unsafe.getDouble(putAddr);
            // Due to no compile time polymorphism we have to duplicate
            // this method for each operator in the next line or suffer
            // virtual dispatch :|
            unsafe.putDouble(putAddr, aValue - bValue);
        }
        return absLen;
    }

    /**
     * Stores useful information about a SFData signal so it can be returned as a single return from an optimised analysis run.
     * 
     */
    public static class Stats
    {
        public double maxValue   = 0;
        public double minValue   = 0;
        public double totalValue = 0;

    }

    private static long scanChunks(SFData b, long bOffset, Stats stats)
    {
        long absLen = b.getLength() - bOffset;

        // Not absLen is the longest continuous mix we can make from
        // a to b.
        long bAddr = b.getAddress(bOffset);
        for (long pos = 0; pos < absLen; pos += 8)
        {
            long putAddr = bAddr + pos;
            double bValue = unsafe.getDouble(putAddr);
            if (bValue > stats.maxValue) stats.maxValue = bValue;
            if (bValue < stats.minValue) stats.minValue = bValue;
            stats.totalValue += bValue;
        }
        return absLen;
    }

    public Stats getStats()
    {
        Stats stats = new Stats();
        long done = 0;
        long thisAt = 0;
        while ((done = scanChunks(this, thisAt, stats)) != 0)
        {
            thisAt += done;
        }
        return stats;
    }

    public enum OPERATION
    {
        ADD, COPY, MULTIPLY, DIVIDE, SUBTRACT
    }

    // Note that this is laboriously layed out to give
    // separate call sites for each operation to help give
    // static dispatch after optimisation
    public void operateOnto(int at, SFSignal in, OPERATION operation)
    {
        if (in.isRealised())
        {
            // TODO this is a bit ambiguous around the meaning of isRealise() but
            // in the current implementation where only SFData returns true for
            // isRealise() we are OK.
            long thisAt = at;
            long otherAt = 0;
            long done = 0;
            SFData data = (SFData) in;
            switch (operation)
            {
            case ADD:
                while ((done = addChunks(data, otherAt, this, thisAt)) != 0)
                {
                    thisAt += done;
                    otherAt += done;
                }
                break;
            case COPY:
                while ((done = copyChunks(data, otherAt, this, thisAt)) != 0)
                {
                    thisAt += done;
                    otherAt += done;
                }
                break;
            case MULTIPLY:
                while ((done = multiplyChunks(data, otherAt, this, thisAt)) != 0)
                {
                    thisAt += done;
                    otherAt += done;
                }
                break;
            case DIVIDE:
                while ((done = divideChunks(data, otherAt, this, thisAt)) != 0)
                {
                    thisAt += done;
                    otherAt += done;
                }
                break;
            case SUBTRACT:
                while ((done = subtractChunks(data, otherAt, this, thisAt)) != 0)
                {
                    thisAt += done;
                    otherAt += done;
                }
                break;
            }
        }
        else
        {
            int len = in.getLength();
            if (len > getLength()) len = getLength();
            switch (operation)
            {
            case ADD:
                for (int index = 0; index < len; ++index)
                {
                    long address = getAddress(index + at);
                    double value = unsafe.getDouble(address);
                    unsafe.putDouble(address, in.getSample(index) + value);
                }
                break;
            case COPY:
                for (int index = 0; index < len; ++index)
                {
                    long address = getAddress(index + at);
                    unsafe.putDouble(address, in.getSample(index));
                }
                break;
            case MULTIPLY:
                for (int index = 0; index < len; ++index)
                {
                    long address = getAddress(index + at);
                    double value = unsafe.getDouble(address);
                    unsafe.putDouble(address, in.getSample(index) * value);
                }
                break;
            case DIVIDE:
                for (int index = 0; index < len; ++index)
                {
                    long address = getAddress(index + at);
                    double value = unsafe.getDouble(address);
                    unsafe.putDouble(address, in.getSample(index) / value);
                }
                break;
            case SUBTRACT:
                for (int index = 0; index < len; ++index)
                {
                    long address = getAddress(index + at);
                    double value = unsafe.getDouble(address);
                    unsafe.putDouble(address, in.getSample(index) - value);
                }
                break;
            }
        }
    }
}
