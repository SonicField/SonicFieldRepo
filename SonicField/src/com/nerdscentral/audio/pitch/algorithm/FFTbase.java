package com.nerdscentral.audio.pitch.algorithm;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Orlando Selenu
 * 
 */
public class FFTbase
{
    /**
     * The Fast Fourier Transform (generic version, with NO optimizations).
     * 
     * @param inputReal
     *            an array of length n, the real part
     * @param inputImag
     *            an array of length n, the imaginary part
     * @param DIRECT
     *            TRUE = direct transform, FALSE = inverse transform
     * @return a new array of length 2n
     */
    public static double[] _fft(final double[] inputReal, double[] inputImag, boolean DIRECT)
    {
        // - n is the dimension of the problem
        // - nu is its logarithm in base e
        int n = inputReal.length;

        // If n is a power of 2, then ld is an integer (_without_ decimals)
        double ld = Math.log(n) / Math.log(2.0);

        // Here I check if n is a power of 2. If exist decimals in ld, I quit
        // from the function returning null.
        if (((int) ld) - ld != 0)
        {
            System.out.println(Messages.getString("FFTbase.0")); //$NON-NLS-1$
            return null;
        }

        // Declaration and initialization of the variables
        // ld should be an integer, actually, so I don't lose any information in
        // the cast
        int nu = (int) ld;
        int n2 = n / 2;
        int nu1 = nu - 1;
        double tReal, tImag, p, arg, c, s;

        // Here I check if I'm going to do the direct transform or the inverse
        // transform.
        double constant;
        if (DIRECT) constant = -2 * Math.PI;
        else
            constant = 2 * Math.PI;

        double[] xReal = inputReal;
        double[] xImag = inputImag;

        // First phase - calculation
        int k = 0;
        // Cache values of sin and cos as these are
        // stupid expensive to compute, also avoids doing
        // bit reversal more that once
        // double[] coses = new double[n];
        // double[] sines = new double[n];

        for (int l = 1; l <= nu; l++)
        {
            while (k < n)
            {
                for (int i = 1; i <= n2; i++)
                {
                    // if (cache)
                    // {
                    p = bitreverseReference(k >> nu1, nu);
                    // direct FFT or inverse FFT
                    arg = constant * p / n;
                    // coses[k] = c = Math.cos(arg);
                    // sines[k] = s = Math.sin(arg);
                    c = Math.cos(arg);
                    s = Math.sin(arg);
                    // }
                    // else
                    // {
                    // c = coses[k];
                    // s = sines[k];
                    // }
                    tReal = xReal[k + n2] * c + xImag[k + n2] * s;
                    tImag = xImag[k + n2] * c - xReal[k + n2] * s;
                    xReal[k + n2] = xReal[k] - tReal;
                    xImag[k + n2] = xImag[k] - tImag;
                    xReal[k] += tReal;
                    xImag[k] += tImag;
                    k++;
                }
                k += n2;
            }
            k = 0;
            nu1--;
            n2 /= 2;
        }

        // Second phase - recombination
        k = 0;
        int r;
        while (k < n)
        {
            r = bitreverseReference(k, nu);
            if (r > k)
            {
                tReal = xReal[k];
                tImag = xImag[k];
                xReal[k] = xReal[r];
                xImag[k] = xImag[r];
                xReal[r] = tReal;
                xImag[r] = tImag;
            }
            k++;
        }

        // Here I have to mix xReal and xImag to have an array (yes, it should
        // be possible to do this stuff in the earlier parts of the code, but
        // it's here to readibility).
        double[] newArray = new double[xReal.length * 2];
        double radice = 1 / Math.sqrt(n);
        for (int i = 0; i < newArray.length; i += 2)
        {
            int i2 = i / 2;
            // I used Stephen Wolfram's Mathematica as a reference so I'm going
            // to normalize the output while I'm copying the elements.
            newArray[i] = xReal[i2] * radice;
            newArray[i + 1] = xImag[i2] * radice;
        }
        return newArray;
    }

    static AtomicReference<SoftReference<CacheableFFT>> cachedF = new AtomicReference<>(null);
    static AtomicReference<SoftReference<CacheableFFT>> cachedR = new AtomicReference<>(null);

    public static double[] fft(final double[] inputReal, double[] inputImag, boolean forward)
    {
        int n = inputReal.length;
        SoftReference<CacheableFFT> sref = forward ? cachedF.get() : cachedR.get();
        CacheableFFT transform = null;
        if (sref != null)
        {
            transform = sref.get();
        }
        if (transform != null)
        {
            if (/*transform.isForward() != forward || */transform.size() != n)
            {
                transform = null;
            }
        }
        if (transform == null)
        {
            transform = new CacheableFFT(n, forward);
            sref = new SoftReference<>(transform);
            if (forward)
            {
                cachedF.set(sref);
            }
            else
            {
                cachedR.set(sref);
            }
        }
        transform.fft(inputReal, inputImag);

        double[] newArray = new double[inputReal.length * 2];
        double radice = 1 / Math.sqrt(n);
        for (int i = 0; i < newArray.length; i += 2)
        {
            int i2 = i / 2;
            newArray[i] = inputReal[i2] * radice;
            newArray[i + 1] = inputImag[i2] * radice;
        }
        return newArray;
    }

    /**
     * The reference bitreverse function.
     */
    private static int bitreverseReference(int j, int nu)
    {
        int j2;
        int j1 = j;
        int k = 0;
        for (int i = 1; i <= nu; i++)
        {
            j2 = j1 / 2;
            k = 2 * k + j1 - 2 * j2;
            j1 = j2;
        }
        return k;
    }
}
