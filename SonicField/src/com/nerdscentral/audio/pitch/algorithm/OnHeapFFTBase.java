/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch.algorithm;

import java.lang.ref.SoftReference;

public class OnHeapFFTBase
{
    private static final ThreadLocal<SoftReference<OnHeapFFT>> cachedF = new ThreadLocal<SoftReference<OnHeapFFT>>()
                                                                       {                                            /**/
                                                                       };
    private static final ThreadLocal<SoftReference<OnHeapFFT>> cachedR = new ThreadLocal<SoftReference<OnHeapFFT>>()
                                                                       {                                            /**/
                                                                       };

    @SuppressWarnings("resource")
    public static void fft(final double[] inputReal, double[] inputImag, double[] newArray, boolean forward)
    {
        int n = inputReal.length;
        SoftReference<OnHeapFFT> sref = forward ? cachedF.get() : cachedR.get();
        OnHeapFFT transform = null;
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
            transform = new OnHeapFFT(n, forward);
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

        long length = inputReal.length << 1;
        double radice = 1.0 / Math.sqrt(n);
        for (int i = 0; i < length; i += 2)
        {
            int i2 = i >> 1;
            newArray[i] = inputReal[i2] * radice;
            newArray[i + 1] = inputImag[i2] * radice;
        }
    }
}
