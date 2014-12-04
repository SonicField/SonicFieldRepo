package com.nerdscentral.audio.pitch.algorithm;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Orlando Selenu
 * 
 */
public class FFTbase
{

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

}
