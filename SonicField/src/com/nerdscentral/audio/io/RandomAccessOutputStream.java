package com.nerdscentral.audio.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RandomAccessOutputStream extends OutputStream
{
    RandomAccessFile file;

    public RandomAccessOutputStream(RandomAccessFile ra)
    {
        super();
        file = ra;
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        file.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        file.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        super.flush();
    }

    @Override
    public void close() throws IOException
    {
        super.flush();
    }

    @Override
    public String toString()
    {
        return file.toString();
    }

    @Override
    public void write(int b) throws IOException
    {
        file.write(b);
    }

}
