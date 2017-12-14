/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.core;

import java.util.concurrent.atomic.AtomicLong;

import com.nerdscentral.audio.pitch.CubicInterpolator;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_RuntimeException;

public abstract class SFSignal
{

    private final static AtomicLong uniqueId = new AtomicLong(0);
    private final long              myId;

    public abstract SFSignal replicate();

    private static ThreadLocal<String> pythonStack = new ThreadLocal<>();

    public final SFSignal replicateEmpty()
    {
        return SFData.build(this.getLength(), true);
    }

    public abstract double getSample(int index);

    protected SFSignal()
    {
        myId = uniqueId.incrementAndGet();
    }

    public SFSignal __pos__()
    {
        return this;
    }

    public SFSignal __neg__()
    {
        return this;
    }

    public SFSignal pin()
    {
        return SFData.realise(this).pin();
    }

    public SFSignal keep()
    {
        return SFData.realise(this).keep();
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

    /**
     * Get a sample using periodic boundary conditions. As an optimisation it will only work negative values up it -length.
     * 
     * @param asked
     * @param length
     * @return the sample at the ask location assuming periodic boundaries
     */
    private final double getPeriodicSample(int asked, int length)
    {
        int correctedAsked = asked;
        if (asked < 0) correctedAsked += length;
        return getSample(correctedAsked % length);
    }

    /** As per getSampleCubic but using periodic boundary conditions */
    public final double getSampleCubicPeriodic(double index)
    {
        int s = (int) SFMaths.floor(index);
        int len = getLength();
        return CubicInterpolator.getValue(getPeriodicSample(s - 1, len), getPeriodicSample(s, len),
                        getPeriodicSample(s + 1, len), getPeriodicSample(s + 2, len), index - s);
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

    public static String getPythonStack()
    {
        return pythonStack.get();
    }

    public static void setPythonStack(String ps)
    {
        SFSignal.pythonStack.set(ps);
    }

    public SFSignal realise()
    {
        // FIXME: Parent has knowledge of child which is BAD.
        return SFData.realise(this);
    }

    @SuppressWarnings("static-method")
    public boolean isRealised()
    {
        return false;
    }

    @Override
    public void finalize()
    {
        release();
    }

    @SuppressWarnings("static-method")
    public boolean isReleased()
    {
        return false;
    }

}