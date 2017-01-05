/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
/**
 * 
 */
package com.nerdscentral.audio.core;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.data.OffHeapArray;
import com.nerdscentral.data.UnsafeProvider;
import com.nerdscentral.sython.SFPL_RuntimeException;

import sun.misc.Unsafe;

/**
 * @author a1t
 * 
 */
public class SFData extends SFSignal implements Serializable
{
    // JIT time alias in effect, maybe remove in the future
    private static final Unsafe unsafe = UnsafeProvider.unsafe;

    private static class MemoryZoneStack extends ThreadLocal<Stack<SFMemoryZone>>
    {
        @Override
        protected Stack<SFMemoryZone> initialValue()
        {
            return new Stack<>();
        }
    }

    private static class ByteBufferWrapper
    {
        private long              address = 0;
        private final FileChannel underLyingFile;
        private final long        fileOffset;
        // buffer is used to hold a reference to the buffer
        // but its data is read via unsafe
        private MappedByteBuffer  buffer;

        public long getAddress()
        {
            if (address == 0) remap();
            return address;
        }

        static Method getAddressMethod;
        static Method getCleanerMethod;
        static Method cleanMethod = null;

        private void remap()
        {
            try
            {
                buffer = underLyingFile.map(MapMode.READ_WRITE, fileOffset, CHUNK_LEN << 3l);
                if (getAddressMethod == null)
                {
                    getAddressMethod = buffer.getClass().getMethod("address");
                    getAddressMethod.setAccessible(true);
                    getCleanerMethod = buffer.getClass().getMethod("cleaner");
                    getCleanerMethod.setAccessible(true);
                }
                buffer.order(ByteOrder.nativeOrder());
                address = (long) getAddressMethod.invoke(buffer);
                // System.out.println("Remap: " + address);
            }
            catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | NoSuchMethodException | SecurityException e)
            {
                throw new RuntimeException(e);
            }
        }

        public void unmap()
        {
            // System.out.println("Unmap: " + this);
            try
            {
                Object cleaner = getCleanerMethod.invoke(buffer);
                if (cleaner != null)
                {
                    if (cleanMethod == null)
                    {
                        cleanMethod = cleaner.getClass().getMethod("clean");
                    }
                    cleanMethod.invoke(cleaner);
                }
                else
                {
                    throw new RuntimeException("Now cleaner method on MappedByteBuffer");
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            address = 0;
        }

        public ByteBufferWrapper(FileChannel file, long offsetInFile) throws SecurityException, IllegalArgumentException
        {
            fileOffset = offsetInFile;
            underLyingFile = file;
            // Grab the chunk of file now so it does not get used twice in a race.
            remap();
        }
    }

    public static class NotCollectedException extends Exception
    {
        private static final long serialVersionUID = 1L;
    }

    // Directory to send swap files to
    private static final String                             SONIC_FIELD_TEMP        = "sonicFieldTemp";                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              //$NON-NLS-1$
    private static final String                             SONIC_FIELD_SAFE_MEMORY = "sonicFieldSaferMemory";                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         //$NON-NLS-1$
    private static final String                             SONIC_FIELD_TRUE        = "true";                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            //$NON-NLS-1$

    private static final long                               CHUNK_SHIFT             = 16;
    private static final long                               CHUNK_LEN               = (long) Math.pow(2, CHUNK_SHIFT);
    private static final long                               CHUNK_MASK              = CHUNK_LEN - 1;
    private static final double                             HEAP_RATIO              = SFConstants.HEAP_RATIO;
    private static final double                             HEAP_SIZE_CHUNKS        = SFConstants.HEAP_SIZE / (8 * CHUNK_LEN);

    private static final long                               serialVersionUID        = 1L;
    private final int                                       length;
    private volatile boolean                                killed                  = false;
    private static File[]                                   coreFile;
    private static RandomAccessFile[]                       coreFileAccessor;
    private ByteBufferWrapper[]                             chunks;
    private static FileChannel[]                            channelMapper;
    private static int                                      fileRoundRobbin         = 0;
    private final long                                      chunkIndex;
    // shadows chunkIndex for memory management as chunkIndex must
    // be final to enable optimisation
    private long                                            allocked;
    private static final AtomicLong                         totalCount              = new AtomicLong();
    private static final AtomicLong                         freeCount               = new AtomicLong();
    private static ConcurrentLinkedDeque<ByteBufferWrapper> freeChunks              = new ConcurrentLinkedDeque<>();
    private static final boolean                            saferMemory;
    @SuppressWarnings("synthetic-access")
    private static final MemoryZoneStack                    memoryZoneStack         = new MemoryZoneStack();

    static
    {
        String safe = System.getProperty(SONIC_FIELD_SAFE_MEMORY);
        saferMemory = safe != null && safe.equals(SONIC_FIELD_TRUE);
        if (saferMemory) System.out.println(Messages.getString("SFData.4")); //$NON-NLS-1$
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
            freeCount.decrementAndGet();
            ++chunkCount;
            countDown -= CHUNK_LEN;
        }

        if (countDown > 0)
        {
            // If the GC has found something, make sure the finalizers are run here so we get
            // the chunks freed.
            System.runFinalization();
            mapMoreData(countDown, chunkCount);
        }
        getChunkAddresses();
    }

    private void getChunkAddresses()
    {
        // now we have the chunks we get the address of the underlying memory
        // of each and place that in the off heap lookup so we no longer reference
        // them via objects but purely as raw memory
        long offSet = 0;
        for (ByteBufferWrapper chunk : chunks)
        {
            unsafe.putAddress(chunkIndex + offSet, chunk.getAddress());
            offSet += 8;
        }
    }

    private void mapMoreData(long countDown, int chunkCount)
    {
        synchronized (coreFile)
        {
            // System.out.println("Adding " + (countDown / CHUNK_LEN) + " chunks");
            while (countDown > 0)
            {
                long from = coreFile[fileRoundRobbin].length();
                @SuppressWarnings("resource")
                FileChannel channel = channelMapper[fileRoundRobbin];
                if (++fileRoundRobbin >= coreFile.length) fileRoundRobbin = 0;
                chunks[chunkCount] = new ByteBufferWrapper(channel, from);
                ++chunkCount;
                totalCount.addAndGet(1);
                countDown -= CHUNK_LEN;
            }
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
    public synchronized void release()
    {
        // If we are already released, do nothing;
        if (chunks == null) return;

        for (ByteBufferWrapper chunk : chunks)
        {
            chunk.unmap();
            freeChunks.push(chunk);
            freeCount.incrementAndGet();
        }
        chunks = null;
        if (saferMemory) freeUnsafeMemory();
    }

    private void freeUnsafeMemory()
    {
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
        Stack<SFMemoryZone> stack = memoryZoneStack.get();
        if (stack.size() > 0)
        {
            SFMemoryZone zone = stack.peek();
            zone.localData.add(this);
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
        SFData ret = new SFData(l);
        ret.clear();
        return ret;
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

    public static final SFData build(OffHeapArray input, int j)
    {
        SFData data = SFData.build(j);
        input.checkBoundsDouble(0, j);
        for (int i = 0; i < j; ++i)
        {
            data.setSample(i, input.getDouble(i));
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
            System.out.println(Messages.getString("SFData.9") + pos2 + Messages.getString("SFData.10") + data2.getLength() //$NON-NLS-1$ //$NON-NLS-2$
                            + Messages.getString("SFData.11") + length);  //$NON-NLS-1$
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
    public double[] getDataInternalOnly()
    {
        double[] ret = new double[length];
        for (int index = 0; index < length; ++index)
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
        long aEnd = CHUNK_LEN + (aOffset & ~CHUNK_MASK);
        long bEnd = CHUNK_LEN + (bOffset & ~CHUNK_MASK);
        long aDif = aEnd - aOffset;
        long bDif = bEnd - bOffset;

        long maxChunkLen = aDif > bDif ? bDif : aDif;

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
        long end = absLen << 3l;
        for (long pos = 0; pos < end; pos += 8)
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
        long end = absLen << 3l;
        for (long pos = 0; pos < end; pos += 8)
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
        long end = absLen << 3l;
        for (long pos = 0; pos < end; pos += 8)
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
        long end = absLen << 3l;
        for (long pos = 0; pos < end; pos += 8)
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
        long end = absLen << 3l;
        for (long pos = 0; pos < end; pos += 8)
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

    @Override
    public void preTouch()
    {
        // Pass.
        // TODO Not sure if we need this or not - comment out for now.
        // Pre-touch the incoming to try and get it memory if possible.
        // In reverse order so if some if swapped out it is more likely to be
        // older and so not so much waste.
        /*
        synchronized (coreFile)
        {
            for (int pointer = length - 1; pointer > 0; pointer -= SFConstants.PAGE_SIZE_DOUBLES)
            {
                // The throw is really just to defeat the optimiser, but why not
                // take advantage?
                if (getSample(pointer) == Double.NaN)
                {
                    throw new RuntimeException("Nan found whilst pre-touching");
                }
            }
        }*/
    }

    public static SFMemoryZone popZone()
    {
        // Relase everything in the head of the zone thread local stack.
        SFMemoryZone zone = memoryZoneStack.get().pop();
        // System.out.println("Popped: " + zone);
        int count = 0;
        for (SFData data : zone.localData)
        {
            count += data.chunks.length;
            data.release();
        }
        System.out.println("Releasing: " + count + " chunks total:" + totalCount.get() + " free: " + freeCount.get());
        // Reap a few references - why not?
        zone.localData.clear();
        zone.localData = null;
        return zone;
    }

    public static SFMemoryZone unstackZone()
    {
        return memoryZoneStack.get().pop();
    }

    public static void stackZone(SFMemoryZone sfMemoryZone)
    {
        pushZone(sfMemoryZone);
    }

    public static SFMemoryZone peekZone()
    {
        Stack<SFMemoryZone> zone = memoryZoneStack.get();
        if (zone.size() == 0) return null;
        return zone.peek();
    }

    public static void pushZone(SFMemoryZone sfMemoryZone)
    {
        // Push to a thread local stack a collection which holds all new SFData objects.
        // So that they can be released explicitly.
        // System.out.println("Pushing: " + sfMemoryZone);
        memoryZoneStack.get().push(sfMemoryZone);
    }
}