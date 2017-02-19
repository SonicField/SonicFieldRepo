/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch.algorithm;

import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.hypot;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import com.nerdscentral.sython.SFMaths;

/** A straight port from C - so very, very static! */
public class SFComplex
{
    public double re;
    public double im;

    public SFComplex(double r, double i)
    {
        this.re = r;
        this.im = i;
    }

    public SFComplex()
    {
    }

    static SFComplex czero = new SFComplex(0.0, 0.0);

    static SFComplex evaluate(SFComplex[] topco, int nz, SFComplex[] botco, int np, double z)
    {
        /* evaluate response, substituting for z */
        SFComplex z2 = new SFComplex(z, 0);
        return cdiv(eval(topco, nz, z2), eval(botco, np, z2));
    }

    static SFComplex evaluate(SFComplex[] topco, SFComplex[] botco, int np, SFComplex z)
    {
        /* evaluate response, substituting for z */
        return cdiv(eval(topco, np, z), eval(botco, np, z));
    }

    static SFComplex eval(SFComplex[] coeffs, int np, SFComplex z)
    {
        /* evaluate polynomial in z, substituting for z */
        SFComplex sum;
        int i;
        sum = czero;
        for (i = np; i >= 0; i--)
            sum = cadd(cmul(sum, z), coeffs[i]);
        return sum;
    }

    static SFComplex csqrt(SFComplex x)
    {
        SFComplex z = new SFComplex();
        double r = hypot(x.im, x.re);
        z.re = Xsqrt(0.5 * (r + x.re));
        z.im = Xsqrt(0.5 * (r - x.re));
        if (x.im < 0.0) z.im = -z.im;
        return z;
    }

    static double Xsqrt(double x)
    { /*
       * because of deficiencies in hypot on Sparc, it's possible for arg of Xsqrt to be small and -ve, which logically it can't
       * be (since r >= |x.re|). Take it as 0. Probably non relevant In Java!
       */
        return (x >= 0.0) ? sqrt(x) : 0.0;
    }

    static SFComplex cexp(SFComplex x)
    {
        SFComplex z = new SFComplex();
        double r;
        r = exp(x.re);
        z.re = r * cos(x.im);
        z.im = r * sin(x.im);
        return z;
    }

    static SFComplex cconj(SFComplex x)
    {
        SFComplex z = new SFComplex();
        z.re = x.re;
        z.im = -x.im;
        return z;
    }

    static SFComplex cadd(SFComplex x, SFComplex y)
    {
        SFComplex z = new SFComplex();
        z.re = x.re + y.re;
        z.im = x.im + y.im;
        return z;
    }

    static SFComplex csub(SFComplex x, SFComplex y)
    {
        SFComplex z = new SFComplex();
        z.re = x.re - y.re;
        z.im = x.im - y.im;
        return z;
    }

    static SFComplex cmul(SFComplex x, SFComplex y)
    {
        SFComplex z = new SFComplex();
        z.re = (x.re * y.re - x.im * y.im);
        z.im = (x.im * y.re + x.re * y.im);
        return z;
    }

    static SFComplex cdiv(SFComplex x, SFComplex y)
    {
        SFComplex z = new SFComplex();
        double mag = y.re * y.re + y.im * y.im;
        z.re = (x.re * y.re + x.im * y.im) / mag;
        z.im = (x.im * y.re - x.re * y.im) / mag;
        return z;
    }

    static void multin(SFComplex w, int npz, SFComplex coeffs[])
    { /* multiply factor (z-w) into coeffs */
        SFComplex nw = SFComplex.cmul(w, new SFComplex(-1, 0));
        for (int i = npz; i >= 1; i--)
        {
            coeffs[i] = SFComplex.cadd((SFComplex.cmul(nw, coeffs[i])), coeffs[i - 1]);
        }
        coeffs[0] = SFComplex.cmul(nw, coeffs[0]);
    }

    static SFComplex expj(double theta)
    {
        return new SFComplex(SFMaths.cos(theta), SFMaths.sin(theta));
    }

    static SFComplex evaluate(SFComplex topco[], int nz, SFComplex botco[], int np, SFComplex z)
    {
        return SFComplex.cdiv(eval(topco, nz, z), eval(botco, np, z));
    }

}
