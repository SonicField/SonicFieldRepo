/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

public class CubicInterpolator
{
    /**
     * Given 0<=x<=1 representing x at point1 when x=0 and x at ppoint2 when x=1 it gives an estimation of what the intermediate
     * value should be.
     * 
     * @param p0
     *            value at point 0
     * @param p1
     *            value at point 1
     * @param p2
     *            value at point 2
     * @param p3
     *            value at point 3
     * @param x
     *            position between point 1 and point 2.
     * @return estimation of value at point x using cubic interpolation of the surrounding points.
     */
    public static double getValue(double p0, double p1, double p2, double p3, double x)
    {
        return p1 + 0.5 * x * (p2 - p0 + x * (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3 + x * (3.0 * (p1 - p2) + p3 - p0)));
    }
}
