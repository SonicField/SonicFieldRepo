package com.nerdscentral.audio.pitch.algorithm;

import java.lang.ref.SoftReference;

import com.nerdscentral.data.OffHeapArray;

public class FFTbase
{
    private static final ThreadLocal<SoftReference<OffHeapFFT>> cachedF = new ThreadLocal<SoftReference<OffHeapFFT>>()
                                                                        {/**/
                                                                        };
    private static final ThreadLocal<SoftReference<OffHeapFFT>> cachedR = new ThreadLocal<SoftReference<OffHeapFFT>>()
                                                                        {/**/
                                                                        };

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
                transform.close();
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

        long length = inputReal.doubleSize() << 1;
        double radice = 1.0 / Math.sqrt(n);
        for (long i = 0; i < length; i += 2)
        {
            long i2 = i >> 1;
            newArray.setDouble(i, inputReal.getDouble(i2) * radice);
            newArray.setDouble(i + 1, inputImag.getDouble(i2) * radice);
        }
    }
}
