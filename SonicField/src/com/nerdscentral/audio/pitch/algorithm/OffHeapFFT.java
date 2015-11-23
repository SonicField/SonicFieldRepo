package com.nerdscentral.audio.pitch.algorithm;

import com.nerdscentral.data.OffHeapArray;

public class OffHeapFFT implements AutoCloseable
{

    private final long         n, m;

    // Lookup tables. Only need to recompute when size of FFT changes.
    private final OffHeapArray cos;
    private final OffHeapArray sin;
    private volatile boolean   closed = false;

    public long size()
    {
        return n;
    }

    public OffHeapFFT(long n1, boolean isForward)
    {
        this.n = n1;
        this.m = (int) (Math.log(n1) / Math.log(2));

        // Make sure n is a power of 2
        if (n1 != (1 << m)) throw new RuntimeException(Messages.getString("CacheableFFT.0")); //$NON-NLS-1$

        cos = OffHeapArray.doubleArray(n1 >> 1);
        sin = OffHeapArray.doubleArray(n1 >> 1);
        double dir = isForward ? -2 * Math.PI : 2 * Math.PI;

        for (long i = 0; i < n1 >> 1; ++i)
        {
            cos.setDouble(i, Math.cos(dir * i / n1));
            sin.setDouble(i, Math.sin(dir * i / n1));
        }

    }

    public void fft(OffHeapArray x, OffHeapArray y)
    {
        if (closed) throw new RuntimeException(Messages.getString("OffHeapFFT.0")); //$NON-NLS-1$
        long i, j, k, n1, n2, a;
        double c, s, t1, t2;

        x.checkBoundsDouble(0, size());
        y.checkBoundsDouble(0, size());

        // Bit-reverse
        j = 0;
        n2 = n >> 1;
        for (i = 1; i < n - 1; ++i)
        {
            n1 = n2;
            while (j >= n1)
            {
                j = j - n1;
                n1 >>= 1;
            }
            j = j + n1;

            if (i < j)
            {
                t1 = x.getDouble(i);
                x.setDouble(i, x.getDouble(j));
                x.setDouble(j, t1);
                t1 = y.getDouble(i);
                y.setDouble(i, y.getDouble(j));
                y.setDouble(j, t1);
            }
        }

        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; ++i)
        {
            n1 = n2;
            n2 <<= 1;
            a = 0;

            for (j = 0; j < n1; j++)
            {
                c = cos.getDouble(a);
                s = sin.getDouble(a);
                a += 1 << (m - i - 1);
                for (k = j; k < n; k += n2)
                {
                    long kn1 = k + n1;
                    t1 = c * x.getDouble(kn1) - s * y.getDouble(kn1);
                    t2 = s * x.getDouble(kn1) + c * y.getDouble(kn1);
                    x.setDouble(kn1, x.getDouble(k) - t1);
                    y.setDouble(kn1, y.getDouble(k) - t2);
                    x.setDouble(k, x.getDouble(k) + t1);
                    y.setDouble(k, y.getDouble(k) + t2);
                }
            }
        }
    }

    @Override
    public void close() throws RuntimeException
    {
        if (closed) throw new RuntimeException(Messages.getString("OffHeapFFT.1")); //$NON-NLS-1$
        closed = true;
        sin.close();
        cos.close();
    }

    @Override
    public void finalize() throws RuntimeException
    {
        close();
    }

}