/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.nerdscentral.audio.pitch.CubicInterpolator;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_RuntimeException;

public abstract class SFSignal implements AutoCloseable
{

    private final static AtomicLong uniqueId       = new AtomicLong(0);
    private final long              myId;
    protected final AtomicInteger   referenceCount = new AtomicInteger(1);

    public abstract boolean isKilled();

    public abstract SFSignal replicate();

    private static ThreadLocal<String> pythonStack = new ThreadLocal<>();

    public final SFData replicateEmpty()
    {
        return SFData.build(this.getLength());
    }

    public abstract double getSample(int index);

    protected SFSignal()
    {
        myId = uniqueId.incrementAndGet();
    }

    /**
     * Returns a linearly interpolated sample based on the samples either side of the the passed index. This is used for super
     * sampling or pitch effects.
     * 
     * @param index
     * @return
     */
    public final double getSampleLinear(double index)
    {
        double s = SFMaths.floor(index);
        double e = SFMaths.ceil(index);
        if (s < 0 || e >= getLength())
        {
            return 0;
        }
        if (s == e) return getSample((int) s);
        double a = getSample((int) s);
        double b = getSample((int) e);
        return ((index - s) * b + (e - index) * a);
    }

    /**
     * Returns a cubic interpolated sample based on the samples either side of the the passed index. This is used for super
     * sampling or pitch effects. Cubic interpolation uses two samples either side of the required point and so at the ends of
     * the sample this will fall back to linear interpolation.
     * 
     * @param index
     * @return
     */
    public final double getSampleCubic(double index)
    {
        int s = (int) SFMaths.floor(index);
        int e = (int) SFMaths.ceil(index);
        if (s < 0 || e >= getLength())
        {
            return 0;
        }
        if (s > getLength() - 3 || index < 1)
        {
            if (s == e) return getSample(s);
            double a = getSample(s);
            double b = getSample(e);
            return ((index - s) * b + (e - index) * a);
        }
        return CubicInterpolator.getValue(getSample(s - 1), getSample(s), getSample(s + 1), getSample(s + 2), index - s);
    }

    public abstract double setSample(int index, double value);

    public abstract int getLength();

    public abstract void setAt(int pos, SFSignal data) throws SFPL_RuntimeException;

    public abstract void setFrom(int pos, SFSignal dataIn) throws SFPL_RuntimeException;

    public abstract double[] getDataInternalOnly();

    /**
     * Get a globally unique identifier for this SFSingal
     * 
     * @return
     */
    public final long getUniqueId()
    {
        return myId;
    }

    public abstract void release();

    @Override
    public void close() throws RuntimeException
    {
        int c = referenceCount.decrementAndGet();
        if (c == 0) release();
        else if (c < 0) throw new RuntimeException(Messages.getString("SFSignal.1")); //$NON-NLS-1$
    }

    public void incrReferenceCount()
    {
        referenceCount.incrementAndGet();
    }

    public SFSignal __pos__()
    {
        incrReferenceCount();
        return this;
    }

    public SFSignal __neg__()
    {
        close();
        return this;
    }

    public int getReferenceCount()
    {
        return referenceCount.get();
    }

    public void decrReferenceCount()
    {
        referenceCount.decrementAndGet();
    }

    public static String getPythonStack()
    {
        return pythonStack.get();
    }

    public static void setPythonStack(String ps)
    {
        SFSignal.pythonStack.set(ps);
    }

    public void clear()
    {
        // NOOP on root class
    }

    @SuppressWarnings("static-method")
    public boolean isRealised()
    {
        return false;
    }
}