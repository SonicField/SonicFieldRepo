/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch.algorithm;

public class OnHeapFFT implements AutoCloseable
{

    private final int        n, m;

    // Lookup tables. Only need to recompute when size of FFT changes.
    private final double[]   cos;
    private final double[]   sin;
    private volatile boolean closed = false;

    public long size()
    {
        return n;
    }

    public OnHeapFFT(int n1, boolean isForward)
    {
        this.n = n1;
        this.m = (int) (Math.log(n1) / Math.log(2));

        // Make sure n is a power of 2
        if (n1 != (1 << m)) throw new RuntimeException(Messages.getString("CacheableFFT.0")); //$NON-NLS-1$

        cos = new double[n1 >> 1];
        sin = new double[n1 >> 1];
        double dir = isForward ? -2 * Math.PI : 2 * Math.PI;

        for (int i = 0; i < n1 >> 1; ++i)
        {
            cos[i] = Math.cos(dir * i / n1);
            sin[i] = Math.sin(dir * i / n1);
        }

    }

    public void fft(double[] x, double[] y)
    {
        if (closed) throw new RuntimeException(Messages.getString("OffHeapFFT.0")); //$NON-NLS-1$
        int i, j, k, n1, n2, a;
        double c, s, t1, t2;

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
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
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
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);
                for (k = j; k < n; k += n2)
                {
                    int kn1 = k + n1;
                    t1 = c * x[kn1] - s * y[kn1];
                    t2 = s * x[kn1] + c * y[kn1];
                    x[kn1] = x[k] - t1;
                    y[kn1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }

    @Override
    public void close() throws RuntimeException
    {
        if (closed) throw new RuntimeException(Messages.getString("OffHeapFFT.1")); //$NON-NLS-1$
        closed = true;
    }

    @Override
    public void finalize() throws RuntimeException
    {
        close();
    }

}