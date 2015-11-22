package com.nerdscentral.data;

public final class OffHeapArray extends UnsafeProvider implements AutoCloseable
{

    private final long             address;
    private final long             length;
    private volatile boolean       closed;

    public final static long       LENGTH_OF_BYTE   = 1;
    public final static long       LENGTH_OF_SHORT  = 2;
    public final static long       LENGTH_OF_INT    = 4;
    public final static long       LENGTH_OF_LONG   = 8;
    public final static long       LENGTH_OF_FLOAT  = 4;
    public final static long       LENGTH_OF_DOUBLE = 8;

    private final static Allocator defaultAllocator = new SimpleOffHeapAllocator();

    public interface IndexedByte
    {
        byte get(long index);
    }

    public interface IndexedShort
    {
        short get(long index);
    }

    public interface IndexedInt
    {
        int get(long index);
    }

    public interface IndexedLong
    {
        long get(long index);
    }

    public interface IndexedFloat
    {
        float get(long index);
    }

    public interface IndexedDouble
    {
        double get(long index);
    }

    public final static class OutOfOffheapException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public OutOfOffheapException(long size)
        {
            super(Messages.getString("OffHeapArray.0") + size + Messages.getString("OffHeapArray.1")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public static interface Allocator
    {
        long allocate(long bytes);

        void free(long address);
    }

    public final static class SimpleOffHeapAllocator extends UnsafeProvider implements Allocator
    {
        @Override
        public final long allocate(long bytes)
        {
            long address = unsafe.allocateMemory(bytes);
            if (address == 0)
            {
                throw new OutOfOffheapException(bytes);
            }
            return address;
        }

        @Override
        public final void free(long address)
        {
            unsafe.freeMemory(address);
        }

    }

    private final Allocator allocator;

    private OffHeapArray(long lengthIn, long stepIn, Allocator allocatorIn)
    {
        allocator = allocatorIn;
        closed = true;
        length = lengthIn * stepIn;
        address = allocator.allocate(length);
        closed = false;
    }

    private final void checkBounds(long start, long end, long step)
    {
        if ((start < 0) || (start * step > length) || (end < 0) || (end * step > length))
        {
            throw new ArrayIndexOutOfBoundsException("" + start + "," + end); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public final void checkBoundsByte(long start, long end)
    {
        checkBounds(start, end, LENGTH_OF_BYTE);
    }

    public final void checkBoundsShort(long start, long end)
    {
        checkBounds(start, end, LENGTH_OF_SHORT);
    }

    public final void checkBoundsInt(long start, long end)
    {
        checkBounds(start, end, LENGTH_OF_INT);
    }

    public final void checkBoundsLong(long start, long end)
    {
        checkBounds(start, end, LENGTH_OF_LONG);
    }

    public final void checkBoundsFLoat(long start, long end)
    {
        checkBounds(start, end, LENGTH_OF_FLOAT);
    }

    public final void checkBoundsDouble(long start, long end)
    {
        checkBounds(start, end, LENGTH_OF_DOUBLE);
    }

    public final long byteSize()
    {
        return length / LENGTH_OF_BYTE;
    }

    public final long shortSize()
    {
        return length / LENGTH_OF_SHORT;
    }

    public final long intSize()
    {
        return length / LENGTH_OF_INT;
    }

    public final long longSize()
    {
        return length / LENGTH_OF_LONG;
    }

    public final long floatSize()
    {
        return length / LENGTH_OF_FLOAT;
    }

    public final long doubleSize()
    {
        return length / LENGTH_OF_DOUBLE;
    }

    public final void initialise()
    {
        initialise((byte) 0);
    }

    public final void initialise(byte value)
    {
        unsafe.setMemory(address, length, value);
    }

    public final void initialise(IndexedByte provider)
    {
        for (long i = 0; i < length; ++i)
        {
            setByte(i, provider.get(i));
        }
    }

    public final void initialise(IndexedShort provider)
    {
        for (long i = 0; i < length; i += LENGTH_OF_SHORT)
        {
            setShort(i, provider.get(i));
        }
    }

    public final void initialise(IndexedInt provider)
    {
        for (long i = 0; i < length; i += LENGTH_OF_INT)
        {
            setInt(i, provider.get(i));
        }
    }

    public final void initialise(IndexedLong provider)
    {
        for (long i = 0; i < length; i += LENGTH_OF_LONG)
        {
            setLong(i, provider.get(i));
        }
    }

    public final void initialise(IndexedFloat provider)
    {
        for (long i = 0; i < length; i += LENGTH_OF_FLOAT)
        {
            setFloat(i, provider.get(i));
        }
    }

    public final void initialise(IndexedDouble provider)
    {
        for (long i = 0; i < length; i += LENGTH_OF_DOUBLE)
        {
            setDouble(i, provider.get(i));
        }
    }

    public final static OffHeapArray byteArray(long length)
    {
        return new OffHeapArray(length, LENGTH_OF_BYTE, defaultAllocator);
    }

    public final static OffHeapArray shortArray(long length)
    {
        return new OffHeapArray(length, LENGTH_OF_SHORT, defaultAllocator);
    }

    public final static OffHeapArray intArray(long length)
    {
        return new OffHeapArray(length, LENGTH_OF_INT, defaultAllocator);
    }

    public final static OffHeapArray longArray(long length)
    {
        return new OffHeapArray(length, LENGTH_OF_LONG, defaultAllocator);
    }

    public final static OffHeapArray floatArray(long length)
    {
        return new OffHeapArray(length, LENGTH_OF_FLOAT, defaultAllocator);
    }

    public final static OffHeapArray doubleArray(long length)
    {
        return new OffHeapArray(length, LENGTH_OF_DOUBLE, defaultAllocator);
    }

    public final static OffHeapArray byteArray(long length, Allocator alloc)
    {
        return new OffHeapArray(length, LENGTH_OF_BYTE, alloc);
    }

    public final static OffHeapArray shortArray(long length, Allocator alloc)
    {
        return new OffHeapArray(length, LENGTH_OF_SHORT, alloc);
    }

    public final static OffHeapArray intArray(long length, Allocator alloc)
    {
        return new OffHeapArray(length, LENGTH_OF_INT, alloc);
    }

    public final static OffHeapArray longArray(long length, Allocator alloc)
    {
        return new OffHeapArray(length, LENGTH_OF_LONG, alloc);
    }

    public final static OffHeapArray floatArray(long length, Allocator alloc)
    {
        return new OffHeapArray(length, LENGTH_OF_FLOAT, alloc);
    }

    public final static OffHeapArray doubleArray(long length, Allocator alloc)
    {
        return new OffHeapArray(length, LENGTH_OF_DOUBLE, alloc);
    }

    public final void setByte(long index, byte value)
    {
        unsafe.putByte(address + index * LENGTH_OF_BYTE, value);
    }

    public final void setShort(long index, short value)
    {
        unsafe.putShort(address + index * LENGTH_OF_SHORT, value);
    }

    public final void setInt(long index, int value)
    {
        unsafe.putInt(address + index * LENGTH_OF_INT, value);
    }

    public final void setLong(long index, long value)
    {
        unsafe.putLong(address + index * LENGTH_OF_LONG, value);
    }

    public final void setFloat(long index, float value)
    {
        unsafe.putFloat(address + index * LENGTH_OF_FLOAT, value);
    }

    public final void setDouble(long index, double value)
    {
        unsafe.putDouble(address + index * LENGTH_OF_DOUBLE, value);
    }

    public final byte getByte(long index)
    {
        return unsafe.getByte(address + index * LENGTH_OF_BYTE);
    }

    public final short getShort(long index)
    {
        return unsafe.getShort(address + index * LENGTH_OF_SHORT);
    }

    public final int getInt(long index)
    {
        return unsafe.getInt(address + index * LENGTH_OF_INT);
    }

    public final long getLong(long index)
    {
        return unsafe.getLong(address + index * LENGTH_OF_LONG);
    }

    public final float getFloat(long index)
    {
        return unsafe.getFloat(address + index * LENGTH_OF_FLOAT);
    }

    public final double getDouble(long index)
    {
        return unsafe.getDouble(address + index * LENGTH_OF_DOUBLE);
    }

    @Override
    public final void close() throws RuntimeException
    {
        if (!closed)
        {
            synchronized (this)
            {
                if (!closed)
                {
                    closed = true;
                }
            }
            allocator.free(address);
        }
    }

    @Override
    public final void finalize() throws Exception
    {
        close();
    }

}
