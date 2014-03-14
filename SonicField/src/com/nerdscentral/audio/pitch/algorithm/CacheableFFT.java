package com.nerdscentral.audio.pitch.algorithm;

public class CacheableFFT
{

    private final int      n, m;

    // Lookup tables. Only need to recompute when size of FFT changes.
    private final double[] cos;
    private final double[] sin;
    private final boolean  forward;

    public boolean isForward()
    {
        return forward;
    }

    public int size()
    {
        return n;
    }

    public CacheableFFT(int n1, boolean isForward)
    {
        this.forward = isForward;
        this.n = n1;
        this.m = (int) (Math.log(n1) / Math.log(2));

        // Make sure n is a power of 2
        if (n1 != (1 << m)) throw new RuntimeException(Messages.getString("CacheableFFT.0")); //$NON-NLS-1$

        cos = new double[n1 / 2];
        sin = new double[n1 / 2];
        double dir = isForward ? -2 * Math.PI : 2 * Math.PI;

        for (int i = 0; i < n1 / 2; i++)
        {
            cos[i] = Math.cos(dir * i / n1);
            sin[i] = Math.sin(dir * i / n1);
        }

    }

    /***************************************************************
     * fft.c Douglas L. Jones University of Illinois at Urbana-Champaign January 19, 1992
     * http://cnx.rice.edu/content/m12016/latest/
     * 
     * fft: in-place radix-2 DIT DFT of a complex input
     * 
     * input: n: length of FFT: must be a power of two m: n = 2**m input/output x: double array of length n with real part of
     * data y: double array of length n with imag part of data
     * 
     * Permission to copy and use this program is granted as long as this header is included.
     ****************************************************************/
    public void fft(double[] x, double[] y)
    {
        int i, j, k, n1, n2, a;
        double c, s, t1, t2;

        // Bit-reverse
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++)
        {
            n1 = n2;
            while (j >= n1)
            {
                j = j - n1;
                n1 = n1 / 2;
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

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++)
        {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++)
            {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2)
                {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }

}