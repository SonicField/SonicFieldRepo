package com.nerdscentral.audio.pitch.algorithm;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicReference;

import com.nerdscentral.data.OffHeapArray;

public class FFTbase
{

    static AtomicReference<SoftReference<OffHeapFFT>> cachedF = new AtomicReference<>(null);
    static AtomicReference<SoftReference<OffHeapFFT>> cachedR = new AtomicReference<>(null);

    @SuppressWarnings("resource")
    public static void fft(final OffHeapArray inputReal, OffHeapArray inputImag, OffHeapArray newArray, boolean forward)
    {
        newArray.checkBoundsDouble(0, inputReal.doubleSize() * 2);
        long n = inputReal.doubleSize();
        SoftReference<OffHeapFFT> sref = forward ? cachedF.get() : cachedR.get();
        OffHeapFFT transform = null;
        if (sref != null)
        {
            transform = sref.get();
        }
        if (transform != null)
        {
            if (transform.size() != n)
            {
                try
                {
                    transform.close();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                transform = null;
            }
        }
        if (transform == null)
        {
            transform = new OffHeapFFT(n, forward);
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

        long length = inputReal.doubleSize() * 2;
        double radice = 1 / Math.sqrt(n);
        for (long i = 0; i < length; i += 2)
        {
            long i2 = i / 2;
            newArray.setDouble(i, inputReal.getDouble(i2) * radice);
            newArray.setDouble(i + 1, inputImag.getDouble(i2) * radice);
        }
    }

}
