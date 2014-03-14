package com.nerdscentral.audio.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessInputStream extends InputStream
{

    private final RandomAccessFile file;

    @Override
    public int read(byte[] b) throws IOException
    {
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        return file.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException
    {
        throw new IOException(Messages.getString("RandomAccessInputStream.0")); //$NON-NLS-1$
    }

    @Override
    public int available() throws IOException
    {
        return super.available();
    }

    @Override
    public void close() throws IOException
    {
        super.close();
    }

    @Override
    public synchronized void mark(int readlimit)
    {
        // do nothing
    }

    @Override
    public synchronized void reset() throws IOException
    {
        throw new IOException(Messages.getString("RandomAccessInputStream.1")); //$NON-NLS-1$
    }

    @Override
    public boolean markSupported()
    {
        return false;
    }

    public RandomAccessInputStream(RandomAccessFile ra)
    {
        super();
        file = ra;
    }

    @Override
    public int read() throws IOException
    {
        return file.read();
    }

}
