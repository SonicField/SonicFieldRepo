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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongFunction;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.data.OffHeapArray;
import com.nerdscentral.data.UnsafeProvider;
import com.nerdscentral.sython.SFPL_RuntimeException;

import sun.misc.Cleaner;
import sun.misc.Unsafe;

/**
 * @author a1t
 * 
 */
public class SFData extends SFSignal implements Serializable
{
    // JIT time alias in effect, maybe remove in the future
    static final Unsafe unsafe = UnsafeProvider.unsafe;

    private static class MemoryZoneStack extends ThreadLocal<Stack<SFMemoryZone>>
    {
        @Override
        protected Stack<SFMemoryZone> initialValue()
        {
            return new Stack<>();
        }
    }

    static class ByteBufferWrapper
    {
        private long              address = 0;
        private final FileChannel underLyingFile;
        private final long        fileOffset;
        // buffer is used to hold a reference to the buffer
        // but its data is read via unsafe
        private MappedByteBuffer  buffer;
        private static long       _adOff  = 0;
        static
        {
            try
            {
                _adOff = unsafe.objectFieldOffset(ByteBufferWrapper.class.getDeclaredField("address")); //$NON-NLS-1$
            }
            catch (NoSuchFieldException | SecurityException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        private final static long adOff = _adOff;

        private final static boolean isUnmapped(ByteBufferWrapper obj)
        {
            return unsafe.getLongVolatile(obj, adOff) == 0;
        }

        private final static long setAddress(ByteBufferWrapper obj, long value)
        {
            return unsafe.getAndSetLong(obj, adOff, value);
        }

        public long getAddress()
        {
            if (isUnmapped(this)) remap();
            return address;
        }

        static Method getAddressMethod;

        synchronized void remap()
        {
            // Already done by another thread;
            if (!isUnmapped(this)) return;
            address = 0; // Stops warnings this should be final etc.
            try
            {
                buffer = underLyingFile.map(MapMode.READ_WRITE, fileOffset, CHUNK_LEN << 3l);
                if (getAddressMethod == null)
                {
                    getAddressMethod = buffer.getClass().getMethod("address"); //$NON-NLS-1$
                    getAddressMethod.setAccessible(true);
                }
                buffer.order(ByteOrder.nativeOrder());
                setAddress(this, (long) getAddressMethod.invoke(buffer));
            }
            catch (IOException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | NoSuchMethodException | SecurityException e)
            {
                throw new RuntimeException(e);
            }
        }

        public ByteBufferWrapper(FileChannel file, long offsetInFile) throws SecurityException, IllegalArgumentException
        {
            fileOffset = offsetInFile;
            underLyingFile = file;
            // Map the data so the underlying channel will create the disk storage for it and
            // so the file size etc continues to make sense.
            remap();
        }

        static Field cleanerField = null;

        public synchronized void release()
        {
            setAddress(this, 0);
            if (isUnmapped(this)) return;
            try
            {
                if (cleanerField == null)
                {
                    cleanerField = buffer.getClass().getDeclaredField("cleaner"); //$NON-NLS-1$
                    cleanerField.setAccessible(true);
                }
                Cleaner cleaner = (Cleaner) cleanerField.get(buffer);
                cleaner.clean();
            }
            catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
            {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

    }

    public static class NotCollectedException extends Exception
    {
        private static final long serialVersionUID = 1L;
    }

    // $NON-NLS-1$
    static final long                                       CHUNK_SHIFT          = 13;
    static final long                                       CHUNK_LEN            = (long) Math.pow(2, CHUNK_SHIFT);
    static final long                                       CHUNK_MASK           = CHUNK_LEN - 1;

    // How frequently to print of memory state once this many samples has been allocated and/or freed.
    private static final long                               MEM_REPORT_THRESHOLD = (long) SFConstants.SAMPLE_RATE
                    * SFConstants.HEART_BEAT;

    private static final long                               serialVersionUID     = 1L;
    private final int                                       length;
    ByteBufferWrapper[]                                     chunks;
    private final long                                      chunkIndex;
    // shadows chunkIndex for memory management as chunkIndex must
    // be final to enable optimisation
    private long                                            allocked;
    private static final AtomicLong                         totalCount           = new AtomicLong();
    private static final AtomicLong                         freeCount            = new AtomicLong();
    private static final AtomicLong                         reportTicker         = new AtomicLong();
    private static ConcurrentLinkedDeque<ByteBufferWrapper> freeChunks           = new ConcurrentLinkedDeque<>();
    private static ConcurrentLinkedDeque<ByteBufferWrapper> allChunks            = new ConcurrentLinkedDeque<>();
    @SuppressWarnings("synthetic-access")
    private static final MemoryZoneStack                    memoryZoneStack      = new MemoryZoneStack();
    private final AtomicBoolean                             kept                 = new AtomicBoolean(false);
    private final AtomicBoolean                             dead                 = new AtomicBoolean(false);
    private final AtomicBoolean                             pinned               = new AtomicBoolean(false);

    // TODO: The file channels are thread local but the chunks are not. Thus over time chunks will disperse
    // across threads and the locality will be broken. If we can make some mechanism to allow this were needed
    // but migrate chunks back to being thread local where possible to improve disk data locallity this would
    // be good.

    private static ConcurrentHashMap<File, FileChannel>     fileMap              = new ConcurrentHashMap<>();

    protected static FileChannel getRotatingChannel()
    {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        try
        {
            File dir = SFConstants.getRotatingTempDir();
            FileChannel channel = fileMap.get(dir);
            if (channel != null) return channel;

            // OK, we do not have an existing swap file in this dir to make a new one.
            File coreFile = File.createTempFile("SonicFieldSwap" + pid, ".mem", dir);  //$NON-NLS-1$//$NON-NLS-2$
            coreFile.deleteOnExit();
            // Now create the actual file
            @SuppressWarnings("resource")
            RandomAccessFile rfile = new RandomAccessFile(coreFile, "rw"); //$NON-NLS-1$
            channel = rfile.getChannel();
            fileMap.put(dir, channel);
            return channel;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void dataHeartBeat(int count)
    {
        /* Periodically perform actions based on when a particular amount of data has been allocated to SFData objects
         * or freed from them.
         */
        if (reportTicker.addAndGet(count) > MEM_REPORT_THRESHOLD)
        {
            reportTicker.set(0);
            final LongFunction<Long> toGigs = chunks -> chunks * CHUNK_LEN * 8 / SFConstants.ONE_MEG;
            System.out.println(Messages.getString("SFData.20") + toGigs.apply(totalCount.get()) //$NON-NLS-1$
                            + Messages.getString("SFData.21") + toGigs.apply((freeCount.get())));  //$NON-NLS-1$
            System.gc();
        }
    }

    private void makeMap(long size) throws SecurityException, IllegalArgumentException, IOException
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

    private void mapMoreData(long countDown, int chunkCount) throws IOException
    {
        synchronized (fileMap)
        {
            // System.out.println("Adding " + (countDown / CHUNK_LEN) + " chunks");
            while (countDown > 0)
            {
                @SuppressWarnings("resource")
                FileChannel channel = getRotatingChannel();
                long from = channel.size();
                ByteBufferWrapper chunk = chunks[chunkCount] = new ByteBufferWrapper(channel, from);
                allChunks.add(chunk);
                ++chunkCount;
                totalCount.addAndGet(1);
                countDown -= CHUNK_LEN;
            }
        }
    }

    @Override
    public void release()
    {
        checkLive();
        if (pinned.get()) throw new RuntimeException("Trying to release pinned memory."); //$NON-NLS-1$
        dataHeartBeat(getLength());
        dead.set(true);
        synchronized (fileMap)
        {
            // If we are already released, do nothing;
            if (chunks == null) return;

            for (ByteBufferWrapper chunk : chunks)
            {
                chunk.release();
                freeChunks.add(chunk);
                freeCount.incrementAndGet();
            }
            chunks = null;
            freeUnsafeMemory();
        }
    }

    @Override
    public boolean isReleased()
    {
        return dead.get();
    }

    private void checkLive()
    {
        if (dead.get())
        {
            throw new RuntimeException(Messages.getString("SFData.22")); //$NON-NLS-1$
        }
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
            synchronized (zone.localData)
            {
                zone.localData.add(this);
            }
        }
        dataHeartBeat(l);
    }

    public final static SFData build(int l, boolean clear)
    {
        SFData ret = new SFData(l);
        if (clear)
        {
            for (long i = 0; i < ret.chunks.length; ++i)
            {
                unsafe.setMemory(unsafe.getAddress(ret.chunkIndex + (i << 3l)), CHUNK_LEN << 3l, (byte) 0);
            }
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see com.nerdscentral.audio.SFSignal#replicate()
     */
    @Override
    public final SFSignal replicate()
    {
        checkLive();
        SFSignal data1 = new SFData(this.getLength());
        int l = this.getLength();
        for (int i = 0; i < l; ++i)
        {
            data1.setSample(i, this.getSample(i));
        }
        return data1;
    }

    private long getAddress(long index)
    {
        if (SFConstants.CHECK_MEMORY)
        {
            if (index < 0 || index > length) throw new RuntimeException("Access out of bounds."); //$NON-NLS-1$
        }
        long pos = index & CHUNK_MASK;
        long bufPos = index >> CHUNK_SHIFT;
        long address = chunkIndex + (bufPos << 3);
        long addr = unsafe.getAddress(address) + (pos << 3l);
        if (SFConstants.CHECK_MEMORY)
        {
            if (addr == 0) throw new RuntimeException("Accessing released memory."); //$NON-NLS-1$
        }
        return addr;
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
        // No nead to clear as we set the entire buffer.
        SFSignal data = new SFData(input.length);
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

    public static final SFSignal build(double[] input, int j)
    {
        SFSignal data = SFData.build(j, false);
        for (int i = 0; i < j; ++i)
        {
            data.setSample(i, input[i]);
        }
        return data;
    }

    public static final SFSignal build(OffHeapArray input, int j)
    {
        SFSignal data = SFData.build(j, false);
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
        checkLive();
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
        checkLive();
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
        checkLive();
        return Messages.getString("SFData.2") + length + Messages.getString("SFData.3"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static SFSignal realise(SFSignal in)
    {
        if (in instanceof SFData) return in;
        int len = in.getLength();
        SFSignal output = new SFData(len);
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
        checkLive();
        return true;
    }

    private static long getAbsLen(SFSignal a, long aOffset, SFSignal b, long bOffset)
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
        checkLive();
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

    // Note that this is laboriously laid out to give
    // separate call sites for each operation to help give
    // static dispatch after optimisation
    public void operateOnto(int at, SFSignal in, OPERATION operation)
    {
        checkLive();
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

    public static SFMemoryZone popZone()
    {
        // Release everything in the head of the zone thread local stack.
        SFMemoryZone zone = memoryZoneStack.get().pop();
        // System.out.println("Popped: " + zone);
        synchronized (zone.localData)
        {
            for (SFData data : zone.localData)
            {
                // Pass
                ByteBufferWrapper[] someChunks = data.chunks;
                if (someChunks != null)
                {
                    // Not already released.
                    if (data.kept.get())
                    {
                        data.kept.set(false);
                    }
                    else
                    {
                        if (!data.pinned.get())
                        {
                            data.release();
                        }
                    }
                }
            }
            zone.localData.clear();
        }
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

    @Override
    public SFSignal pin()
    {
        checkLive();
        pinned.set(true);
        return this;
    }

    @Override
    public SFSignal keep()
    {
        checkLive();
        kept.set(true);
        return this;
    }
}