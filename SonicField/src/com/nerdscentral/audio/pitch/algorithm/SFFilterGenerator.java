/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch.algorithm;

import static com.nerdscentral.audio.pitch.algorithm.SFComplex.cadd;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.cconj;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.cdiv;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.cexp;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.cmul;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.csqrt;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.csub;
import static com.nerdscentral.audio.pitch.algorithm.SFComplex.evaluate;
import static com.nerdscentral.sython.SFMaths.PI;
import static com.nerdscentral.sython.SFMaths.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.hypot;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;

import com.nerdscentral.audio.SFConstants;

/**
 * This code is based on a rewrite of code by Tony Fisher RIP
 * 
 * 
 */
public class SFFilterGenerator
{

    public static class NPoleFilterDefListNode
    {
        private NPoleFilterDef definition;
        private int            position;

        public NPoleFilterDef getDefinition()
        {
            return definition;
        }

        public void setDefinition(NPoleFilterDef definition1)
        {
            this.definition = definition1;
        }

        /**
         * The position up to (not inclusive) to which this definition is valid. The last node should have a value at or beyond
         * the length of the sample.
         * 
         * @return
         */
        public int getPosition()
        {
            return position;
        }

        public void setPosition(int position1)
        {
            this.position = position1;
        }
    }

    public static class NPoleFilterDef
    {
        private final double xn[];
        private final double yn[];
        private final int    poles;

        public NPoleFilterDef(int polesIn)
        {
            xn = new double[polesIn + 1];
            yn = new double[polesIn];
            poles = polesIn;
        }

        public int getPoles()
        {
            return poles;
        }

        public double getXCeof(int index)
        {
            return xn[poles - index];
        }

        public double getYCeof(int index)
        {
            return yn[index];
        }

        void setXCeof(int index, double value)
        {
            xn[index] = value;
        }

        void setYCeof(int index, double value)
        {
            yn[index] = value;
        }

        public double getGaindc()
        {
            return gaindc;
        }

        public void setGaindc(double gaindc1)
        {
            this.gaindc = gaindc1;
        }

        public double getGainhf()
        {
            return gainhf;
        }

        public void setGainhf(double gainhf1)
        {
            this.gainhf = gainhf1;
        }

        public double getGainfc()
        {
            return gainfc;
        }

        public void setGainfc(double gainfc1)
        {
            this.gainfc = gainfc1;
        }

        private double gaindc;
        private double gainhf;
        private double gainfc;
    }

    private final static double TWOPI        = 2.0 * PI;
    private final double        EPS          = 1e-10;
    private final int           MAXORDER     = 10;
    private final int           MAXPOLES     = (2 * MAXORDER);          /* to allow for doubling of poles in BP filter */

    private final int           opt_be       = 0x0001;                  /* -Be Bessel cheracteristic */
    private final int           opt_bu       = 0x0002;                  /* -Bu Butterworth characteristic */
    private final int           opt_ch       = 0x0004;                  /* -Ch Chebyshev characteristic */

    private final int           opt_lp       = 0x0008;                  /* -Lp low-pass */
    private final int           opt_hp       = 0x0010;                  /* -Hp high-pass */
    private final int           opt_bp       = 0x0020;                  /* -Bp band-pass */

    @SuppressWarnings("unused")
    private final int           opt_ap       = 0x00080;

    // private final int opt_a = 0x0040; /* -a alpha value */
    // private final int opt_e = 0x0100; /* -e execute filter */
    // private final int opt_l = 0x0200; /* -l just list filter parameters */
    // private final int opt_o = 0x0400; /* -o order of filter */
    private final int           opt_p        = 0x0800;                  /* -p specified poles only */
    private final int           opt_w        = 0x1000;                  /* -w don't pre-warp */

    private int                 order, numpoles;
    private double              raw_alpha1, raw_alpha2;
    private SFComplex           dc_gain;
    private SFComplex           fc_gain;
    private SFComplex           hf_gain;
    private int                 opts;                                   /* option flag bits */

    private double              warped_alpha1, warped_alpha2;
    private long                polemask;
    // private boolean optsok;

    private final SFComplex[]   spoles       = new SFComplex[MAXPOLES];
    private final SFComplex[]   zpoles       = new SFComplex[MAXPOLES];
    private final SFComplex[]   zzeros       = new SFComplex[MAXPOLES];
    private final double[]      xcoeffs      = new double[MAXPOLES + 1];
    private final double[]      ycoeffs      = new double[MAXPOLES + 1];

    SFComplex[]                 bessel_poles = new SFComplex[] { /*
                                                                  * table produced by /usr/fisher/bessel -- N.B. only one member
                                                                  * of each C.Conj. pair is listed
                                                                  */
                                             new SFComplex(-1.000000e+00, 0.000000e+00),
            new SFComplex(-8.660254e-01, -5.000000e-01), new SFComplex(-9.416000e-01, 0.000000e+00),
            new SFComplex(-7.456404e-01, -7.113666e-01), new SFComplex(-9.047588e-01, -2.709187e-01),
            new SFComplex(-6.572112e-01, -8.301614e-01), new SFComplex(-9.264421e-01, 0.000000e+00),
            new SFComplex(-8.515536e-01, -4.427175e-01), new SFComplex(-5.905759e-01, -9.072068e-01),
            new SFComplex(-9.093907e-01, -1.856964e-01), new SFComplex(-7.996542e-01, -5.621717e-01),
            new SFComplex(-5.385527e-01, -9.616877e-01), new SFComplex(-9.194872e-01, 0.000000e+00),
            new SFComplex(-8.800029e-01, -3.216653e-01), new SFComplex(-7.527355e-01, -6.504696e-01),
            new SFComplex(-4.966917e-01, -1.002509e+00), new SFComplex(-9.096832e-01, -1.412438e-01),
            new SFComplex(-8.473251e-01, -4.259018e-01), new SFComplex(-7.111382e-01, -7.186517e-01),
            new SFComplex(-4.621740e-01, -1.034389e+00), new SFComplex(-9.154958e-01, 0.000000e+00),
            new SFComplex(-8.911217e-01, -2.526581e-01), new SFComplex(-8.148021e-01, -5.085816e-01),
            new SFComplex(-6.743623e-01, -7.730546e-01), new SFComplex(-4.331416e-01, -1.060074e+00),
            new SFComplex(-9.091347e-01, -1.139583e-01), new SFComplex(-8.688460e-01, -3.430008e-01),
            new SFComplex(-7.837694e-01, -5.759148e-01), new SFComplex(-6.417514e-01, -8.175836e-01),
            new SFComplex(-4.083221e-01, -1.081275e+00), };

    private final SFComplex     cmone        = new SFComplex(-1.0, 0.0);
    private final SFComplex     czero        = new SFComplex(0.0, 0.0);
    private final SFComplex     cone         = new SFComplex(1.0, 0.0);
    private final SFComplex     ctwo         = new SFComplex(2.0, 0.0);
    private final SFComplex     chalf        = new SFComplex(0.5, 0.0);

    private SFComplex cneg(SFComplex z)
    {
        return csub(czero, z);
    }

    public static NPoleFilterDef computeButterworthNHP(double frequency, int order)
    {
        SFFilterGenerator gen = new SFFilterGenerator();
        gen.opts |= gen.opt_bu | gen.opt_hp;
        gen.order = order;
        setUpGuts(gen, frequency);
        NPoleFilterDef ret = new NPoleFilterDef(order);
        gen.storeNPole(ret);
        return ret;
    }

    public static NPoleFilterDef computeButterworthNLP(double frequency, int order)
    {
        SFFilterGenerator gen = new SFFilterGenerator();
        gen.opts |= gen.opt_bu | gen.opt_lp;
        gen.order = order;
        setUpGuts(gen, frequency);
        NPoleFilterDef ret = new NPoleFilterDef(order);
        gen.storeNPole(ret);
        return ret;
    }

    public static NPoleFilterDef computeBesselNLP(double frequency, int order)
    {
        SFFilterGenerator gen = new SFFilterGenerator();
        gen.opts |= gen.opt_be | gen.opt_lp;
        gen.order = order;
        setUpGuts(gen, frequency);
        NPoleFilterDef ret = new NPoleFilterDef(order);
        gen.storeNPole(ret);
        return ret;
    }

    public static NPoleFilterDef computeBesselNHP(double frequency, int order)
    {
        SFFilterGenerator gen = new SFFilterGenerator();
        gen.opts |= gen.opt_be | gen.opt_hp;
        gen.order = order;
        setUpGuts(gen, frequency);
        NPoleFilterDef ret = new NPoleFilterDef(order);
        gen.storeNPole(ret);
        return ret;
    }

    public static NPoleFilterDef computeBesselNBP(double frequencyA, double frequencyB, int order)
    {
        SFFilterGenerator gen = new SFFilterGenerator();
        gen.opts |= gen.opt_ch | gen.opt_bp;
        gen.order = order;
        setUpGuts(gen, frequencyA, frequencyB);
        NPoleFilterDef ret = new NPoleFilterDef(gen.numpoles);
        gen.storeNPole(ret);
        return ret;
    }

    private static void setUpGuts(SFFilterGenerator gen, double frequency)
    {
        setUpGuts(gen, frequency, frequency);
    }

    private static void setUpGuts(SFFilterGenerator gen, double frequencyA, double frequencyB)
    {
        gen.raw_alpha1 = frequencyA / SFConstants.SAMPLE_RATE;
        gen.raw_alpha2 = frequencyB / SFConstants.SAMPLE_RATE;
        gen.setdefaults();
        gen.compute_s();
        gen.normalize();
        gen.compute_z();
        gen.expandpoly();
    }

    public static NPoleFilterDef computeButterworthNBP(double frequencyA, double frequencyB, int order)
    {
        SFFilterGenerator gen = new SFFilterGenerator();
        gen.opts |= gen.opt_bu | gen.opt_bp;
        gen.order = order;
        setUpGuts(gen, frequencyA, frequencyB);
        NPoleFilterDef ret = new NPoleFilterDef(gen.numpoles);
        gen.storeNPole(ret);
        return ret;
    }

    void setdefaults()
    {
        if ((opts & opt_p) == 0) polemask = -1;
        if ((opts & opt_bp) == 0) raw_alpha2 = raw_alpha1;
    }

    static double asinh(double value)
    {
        double returned;

        if (value > 0) returned = log(value + sqrt(value * value + 1));
        else
            returned = -log(-value + sqrt(value * value + 1));
        return (returned);

    }

    void compute_s()
    {
        numpoles = 0;
        if ((opts & opt_be) != 0)
        { /* Bessel filter */
            int i;
            int p = (order * order) / 4; /* ptr into table */
            if ((order & 1) != 0) choosepole(bessel_poles[p++]);
            for (i = 0; i < order / 2; i++)
            {
                choosepole(bessel_poles[p]);
                choosepole(cconj(bessel_poles[p]));
                p++;
            }
        }
        if ((opts & (opt_bu | opt_ch)) != 0)
        { /* Butterworth filter */
            int i;
            for (i = 0; i < 2 * order; i++)
            {
                SFComplex s = new SFComplex();
                s.re = 0.0;
                s.im = ((order & 1) != 0) ? (i * PI) / order : ((i + 0.5) * PI) / order;
                choosepole(cexp(s));
            }
        }
    }

    void choosepole(SFComplex z)
    {
        if (z.re < 0.0)
        {
            if ((polemask & 1) != 0) spoles[numpoles++] = z;
            polemask >>= 1;
        }
    }

    void normalize()
    {
        SFComplex w1 = new SFComplex();
        SFComplex w2 = new SFComplex();
        int i;
        /* for bilinear transform, perform pre-warp on alpha values */
        if ((opts & opt_w) != 0)
        {
            warped_alpha1 = raw_alpha1;
            warped_alpha2 = raw_alpha2;
        }
        else
        {
            warped_alpha1 = tan(PI * raw_alpha1) / PI;
            warped_alpha2 = tan(PI * raw_alpha2) / PI;
        }
        w1.re = TWOPI * warped_alpha1;
        w1.im = 0.0;
        w2.re = TWOPI * warped_alpha2;
        w2.im = 0.0;
        /* transform prototype into appropriate filter type (lp/hp/bp) */
        switch ((opts & (opt_lp + opt_hp + opt_bp)))
        {
        case opt_lp:
            for (i = 0; i < numpoles; i++)
                spoles[i] = cmul(spoles[i], w1);
            break;

        case opt_hp:
            for (i = 0; i < numpoles; i++)
                spoles[i] = cdiv(w1, spoles[i]);
            /* also N zeros at (0,0) */
            break;

        case opt_bp:
        {
            SFComplex w0 = new SFComplex();
            SFComplex bw = new SFComplex();
            w0 = csqrt(cmul(w1, w2));
            bw = csub(w2, w1);
            for (i = 0; i < numpoles; i++)
            {
                SFComplex hba = new SFComplex();
                SFComplex temp = new SFComplex();
                hba = cmul(chalf, cmul(spoles[i], bw));
                temp = cdiv(w0, hba);
                temp = csqrt(csub(cone, cmul(temp, temp)));
                spoles[i] = cmul(hba, cadd(cone, temp));
                spoles[numpoles + i] = cmul(hba, csub(cone, temp));
            }
            /* also N zeros at (0,0) */
            numpoles *= 2;
            break;
        }
        }
    }

    void compute_z() /* given S-plane poles, compute Z-plane poles */
    {
        int i;
        for (i = 0; i < numpoles; i++)
        { /* use bilinear transform */
            SFComplex top = new SFComplex();
            SFComplex bot = new SFComplex();
            top = cadd(ctwo, spoles[i]);
            bot = csub(ctwo, spoles[i]);
            zpoles[i] = cdiv(top, bot);
            switch ((opts & (opt_lp + opt_hp + opt_bp)))
            {
            case opt_lp:
                zzeros[i] = cmone;
                break;
            case opt_hp:
                zzeros[i] = cone;
                break;
            case opt_bp:
                zzeros[i] = ((i & 1) != 0) ? cone : cmone;
                break;
            }
        }
    }

    void expandpoly() /* given Z-plane poles & zeros, compute top & bot polynomials in Z, and then recurrence relation */
    {
        SFComplex[] topcoeffs = new SFComplex[MAXPOLES + 1];
        SFComplex[] botcoeffs = new SFComplex[MAXPOLES + 1];
        SFComplex st = new SFComplex();
        SFComplex zfc = new SFComplex();
        int i;
        expand(zzeros, topcoeffs);
        expand(zpoles, botcoeffs);
        dc_gain = evaluate(topcoeffs, botcoeffs, numpoles, cone);
        st.re = 0.0;
        st.im = TWOPI * 0.5 * (raw_alpha1 + raw_alpha2); /* "jwT" for centre freq. */
        zfc = cexp(st);
        fc_gain = evaluate(topcoeffs, botcoeffs, numpoles, zfc);
        hf_gain = evaluate(topcoeffs, botcoeffs, numpoles, cmone);
        for (i = 0; i <= numpoles; i++)
        {
            xcoeffs[i] = topcoeffs[i].re / botcoeffs[numpoles].re;
            ycoeffs[i] = -(botcoeffs[i].re / botcoeffs[numpoles].re);
        }
    }

    void expand(SFComplex[] pz, SFComplex[] coeffs)
    { /* compute product of poles or zeros as a polynomial of z */
        int i;
        coeffs[0] = cone;
        for (i = 0; i < numpoles; i++)
            coeffs[i + 1] = czero;
        for (i = 0; i < numpoles; i++)
            multin(pz[i], coeffs);
        /* check computed coeffs of z^k are all real */
        for (i = 0; i < numpoles + 1; i++)
        {
            if (abs(coeffs[i].im) > EPS)
            {
                System.err.println(Messages.getString("SFFilterGenerator.4") + i + Messages.getString("SFFilterGenerator.5")); //$NON-NLS-1$ //$NON-NLS-2$
                System.exit(1);
            }
        }
    }

    void multin(SFComplex w, SFComplex[] coeffs)
    { /* multiply factor (z-w) into coeffs */
        SFComplex nw = new SFComplex();
        int i;
        nw = cneg(w);
        for (i = numpoles; i >= 1; i--)
            coeffs[i] = cadd(cmul(nw, coeffs[i]), coeffs[i - 1]);
        coeffs[0] = cmul(nw, coeffs[0]);
    }

    void execute()
    {
        double[] xv = new double[MAXPOLES + 1];
        double[] yv = new double[MAXPOLES + 1];
        int i;
        if (!(xcoeffs[numpoles] == 1.0))
        {
            System.err.println(Messages.getString("SFFilterGenerator.6") + xcoeffs[numpoles]); //$NON-NLS-1$
            System.exit(1);
        }
        for (i = 0; i <= numpoles; i++)
            xv[i] = yv[i] = 0.0;
        for (;;)
        {
            double x = 0;
            int j;

            for (j = 0; j < numpoles; j++)
            {
                xv[j] = xv[j + 1];
                yv[j] = yv[j + 1];
            }
            xv[numpoles] = yv[numpoles] = x;
            for (j = 0; j < numpoles; j++)
                yv[numpoles] += (xcoeffs[j] * xv[j]) + (ycoeffs[j] * yv[j]);
            System.out.println(Messages.getString("SFFilterGenerator.7") + yv[numpoles]); //$NON-NLS-1$
        }
    }

    void printfilter()
    {
        System.out.println(String.format(Messages.getString("SFFilterGenerator.8"), raw_alpha1)); //$NON-NLS-1$
        // printf("raw alpha1    = %14.10f\n", );
        System.out.println(String.format(Messages.getString("SFFilterGenerator.9"), warped_alpha1)); //$NON-NLS-1$
        System.out.println(String.format(Messages.getString("SFFilterGenerator.10"), raw_alpha2)); //$NON-NLS-1$
        System.out.println(String.format(Messages.getString("SFFilterGenerator.11"), warped_alpha2)); //$NON-NLS-1$
        printgain(Messages.getString("SFFilterGenerator.12"), dc_gain); //$NON-NLS-1$
        printgain(Messages.getString("SFFilterGenerator.13"), fc_gain); //$NON-NLS-1$
        printgain(Messages.getString("SFFilterGenerator.14"), hf_gain); //$NON-NLS-1$
        printrat_s();
        printrat_z();
        printrecurrence();
    }

    void printgain(String str, SFComplex gain)
    {
        double r = hypot(gain.im, gain.re);
        System.out.print(String.format(Messages.getString("SFFilterGenerator.15"), str, r)); //$NON-NLS-1$
        if (r > EPS) System.out.print(String.format(Messages.getString("SFFilterGenerator.16"), atan2(gain.im, gain.re) / PI)); //$NON-NLS-1$
        System.out.println(Messages.getString("SFFilterGenerator.17")); //$NON-NLS-1$
    }

    void printrat_s()
    {
        int i;
        System.out.println(Messages.getString("SFFilterGenerator.18")); //$NON-NLS-1$
        switch (opts & (opt_lp + opt_hp + opt_bp))
        {
        case opt_lp:
            System.out.println(Messages.getString("SFFilterGenerator.19")); //$NON-NLS-1$
            break;

        case opt_hp:
            System.out.print('\t');
            prcomplex(czero);
            System.out.println(String.format(Messages.getString("SFFilterGenerator.20"), numpoles)); //$NON-NLS-1$
            break;

        case opt_bp:
            System.out.print('\t');
            prcomplex(czero);
            System.out.println(String.format(Messages.getString("SFFilterGenerator.21"), numpoles / 2)); //$NON-NLS-1$
            break;
        }
        System.out.println(Messages.getString("SFFilterGenerator.22")); //$NON-NLS-1$
        System.out.println(Messages.getString("SFFilterGenerator.23")); //$NON-NLS-1$
        for (i = 0; i < numpoles; i++)
        {
            System.out.print('\t');
            prcomplex(spoles[i]);
            System.out.println(Messages.getString("SFFilterGenerator.24")); //$NON-NLS-1$
        }
        System.out.println(Messages.getString("SFFilterGenerator.25")); //$NON-NLS-1$
    }

    void printrat_z() /* print rational form of H(z) */
    {
        int i;
        System.out.println(Messages.getString("SFFilterGenerator.26")); //$NON-NLS-1$
        switch (opts & (opt_lp + opt_hp + opt_bp))
        {
        case opt_lp:
            System.out.print('\t');
            prcomplex(cmone);
            System.out.println(String.format(Messages.getString("SFFilterGenerator.27"), numpoles)); //$NON-NLS-1$
            break;

        case opt_hp:
            System.out.print('\t');
            prcomplex(cone);
            System.out.println(String.format(Messages.getString("SFFilterGenerator.28"), numpoles)); //$NON-NLS-1$
            break;

        case opt_bp:
            System.out.print('\t');
            prcomplex(cone);
            System.out.println(String.format(Messages.getString("SFFilterGenerator.29"), numpoles / 2)); //$NON-NLS-1$
            System.out.print('\t');
            prcomplex(cmone);
            System.out.println(String.format(Messages.getString("SFFilterGenerator.30"), numpoles / 2)); //$NON-NLS-1$
            break;
        }
        System.out.println(Messages.getString("SFFilterGenerator.31")); //$NON-NLS-1$
        System.out.println(Messages.getString("SFFilterGenerator.32")); //$NON-NLS-1$
        for (i = 0; i < numpoles; i++)
        {
            System.out.print('\t');
            prcomplex(zpoles[i]);
            System.out.println(Messages.getString("SFFilterGenerator.33")); //$NON-NLS-1$
        }
        System.out.println(Messages.getString("SFFilterGenerator.34")); //$NON-NLS-1$
    }

    void storeNPole(NPoleFilterDef in)
    {
        for (int pole = 0; pole <= numpoles; ++pole)
        {
            in.setXCeof(pole, xcoeffs[pole]);
        }
        for (int pole = 0; pole < numpoles; ++pole)
        {
            in.setYCeof(pole, ycoeffs[pole]);
        }
        in.setGaindc(hypot(dc_gain.re, dc_gain.im));
        in.setGainhf(hypot(hf_gain.re, hf_gain.im));
        in.setGainfc(hypot(fc_gain.re, fc_gain.im));
    }

    void printrecurrence() /* given (real) Z-plane poles & zeros, compute & print recurrence relation */
    {
        int i;
        System.out.print(String.format(Messages.getString("SFFilterGenerator.35"))); //$NON-NLS-1$
        System.out.print(String.format(Messages.getString("SFFilterGenerator.36"))); //$NON-NLS-1$
        for (i = 0; i < numpoles + 1; i++)
        {
            if (i > 0) System.out.print(String.format(Messages.getString("SFFilterGenerator.37"))); //$NON-NLS-1$
            System.out.print(String.format(Messages.getString("SFFilterGenerator.38"), xcoeffs[i], numpoles - i)); //$NON-NLS-1$
        }
        System.out.println(Messages.getString("SFFilterGenerator.39")); //$NON-NLS-1$
        for (i = 0; i < numpoles; i++)
        {
            System.out.print(String.format(Messages.getString("SFFilterGenerator.40"), ycoeffs[i], numpoles - i)); //$NON-NLS-1$
        }
        System.out.println(Messages.getString("SFFilterGenerator.41")); //$NON-NLS-1$
    }

    static void prcomplex(SFComplex z)
    {
        System.out.print(String.format(Messages.getString("SFFilterGenerator.42"), z.re, z.im)); //$NON-NLS-1$
    }

    static SFComplex reflect(SFComplex z)
    {
        double r = hypot(z.im, z.re);
        return cdiv(z, new SFComplex(sqrt(r), 0));
    }

    static void expand(SFComplex pz[], int npz, SFComplex coeffs[])
    { /* compute product of poles or zeros as a polynomial of z */
        int i;
        coeffs[0] = new SFComplex(1.0, 0);
        for (i = 0; i < npz; i++)
            coeffs[i + 1] = new SFComplex(0.0, 0);
        for (i = 0; i < npz; i++)
            SFComplex.multin(pz[i], npz, coeffs);
    }

}
