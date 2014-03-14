/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.sython;

public final class SFMaths
{
    public static final double PI = Math.PI;
    public static final double E  = Math.E;

    public static double cos(double theta)
    {
        return Math.cos(theta);
    }

    public static double hypot(double x, double y)
    {
        return Math.hypot(x, y);
    }

    public static double abs(double x)
    {
        return x < 0 ? -x : x;
    }

    public static double sinSlow(double theta)
    {
        return Math.sin(theta);
    }

    public static double ceil(double a)
    {
        return Math.ceil(a);
    }

    public static double min(double a, double b)
    {
        return a < b ? a : b;
    }

    public static double max(final double a, final double b)
    {
        return a < b ? b : a;
    }

    public static double pow(double a, double b)
    {
        return Math.pow(a, b);
    }

    public static double floor(final double d)
    {
        return Math.floor(d);
    }

    public static double random()
    {
        return Math.random();
    }

    static double[]             table = null;
    static double               step;
    static double               invStep;
    static int                  size  = 0;

    static
    {

        size = 10000;
        table = new double[size];
        step = 2d * Math.PI / size;
        invStep = 1.0f / step;
        for (int i = 0; i < size; ++i)
        {
            table[i] = Math.sin(step * i);
        }
    }

    /**
     * Find a linear interpolation from the table
     * 
     * @param ang
     *            angle in radians
     * @return sin of angle a
     */
    private final static double pi2   = PI * 2;

    final public static double sin(double ang)
    {
        double t = ang % pi2;
        int indexA = (int) (t / step);
        int indexB = indexA + 1;
        if (indexB >= size) return table[indexA];
        double a = table[indexA];
        return a + (table[indexB] - a) * (t - (indexA * step)) * invStep;

    }
}
