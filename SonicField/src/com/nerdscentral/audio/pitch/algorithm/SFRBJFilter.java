package com.nerdscentral.audio.pitch.algorithm;

import com.nerdscentral.audio.SFConstants;

/*
 * RBJ Filters from C++ version by arguru[AT]smartelectronix[DOT]com based on eq filter cookbook by Robert Bristow-Johnson
 * <rbj@audioimagination.com> This code is believed to be public domain and license free after best efforts to establish its
 * licensing.
 */
public class SFRBJFilter
{

    // filter coeffs
    double b0a0, b1a0, b2a0, a1a0, a2a0;

    // in/out history
    double ou1, ou2, in1, in2;

    public SFRBJFilter()
    {
        // reset filter coeffs
        b0a0 = b1a0 = b2a0 = a1a0 = a2a0 = 0.0;

        // reset in/out history
        ou1 = ou2 = in1 = in2 = 0.0f;
    }

    public double filter(double in0)
    {
        // filter
        final double yn = b0a0 * in0 + b1a0 * in1 + b2a0 * in2 - a1a0 * ou1 - a2a0 * ou2;

        // push in/out buffers
        in2 = in1;
        in1 = in0;
        ou2 = ou1;
        ou1 = yn;

        // return output
        return yn;
    }

    public enum FilterType
    {
        LOWPASS, HIGHPASS, BANDPASS_SKIRT, BANDPASS_PEAK, NOTCH, ALLPASS, PEAK, LOWSHELF, HIGHSHELF
    }

    public void calc_filter_coeffs(final FilterType type, final double frequency, final double q, final double db_gain)
    {
        boolean q_is_bandwidth;
        final double sample_rate = SFConstants.SAMPLE_RATE;
        switch (type)
        {
        case ALLPASS:
        case HIGHPASS:
        case LOWPASS:
        case LOWSHELF:
        case HIGHSHELF:
            q_is_bandwidth = false;
            break;
        default:
            q_is_bandwidth = true;
            break;
        }
        // System.out.println("Q Is Bandwidth " + q_is_bandwidth);
        // temp pi
        final double temp_pi = 3.1415926535897932384626433832795;

        // temp coef vars
        double alpha, a0 = 0, a1 = 0, a2 = 0, b0 = 0, b1 = 0, b2 = 0;

        // peaking, lowshelf and hishelf
        if (type == FilterType.PEAK || type == FilterType.HIGHSHELF || type == FilterType.LOWSHELF)
        {
            final double A = Math.pow(10.0, (db_gain / 40.0));
            final double omega = 2.0 * temp_pi * frequency / sample_rate;
            final double tsin = Math.sin(omega);
            final double tcos = Math.cos(omega);
            if (type == FilterType.PEAK) alpha = tsin * Math.sinh(Math.log(2.0) / 2.0 * q * omega / tsin);
            else
                alpha = tsin / 2.0 * Math.sqrt((A + 1 / A) * (1 / q - 1) + 2);

            final double beta = Math.sqrt(A) / q;

            // peaking
            if (type == FilterType.PEAK)
            {
                b0 = (1.0 + alpha * A);
                b1 = (-2.0 * tcos);
                b2 = (1.0 - alpha * A);
                a0 = (1.0 + alpha / A);
                a1 = (-2.0 * tcos);
                a2 = (1.0 - alpha / A);
            }

            // lowshelf
            if (type == FilterType.LOWSHELF)
            {
                b0 = (A * ((A + 1.0) - (A - 1.0) * tcos + beta * tsin));
                b1 = (2.0 * A * ((A - 1.0) - (A + 1.0) * tcos));
                b2 = (A * ((A + 1.0) - (A - 1.0) * tcos - beta * tsin));
                a0 = ((A + 1.0) + (A - 1.0) * tcos + beta * tsin);
                a1 = (-2.0 * ((A - 1.0) + (A + 1.0) * tcos));
                a2 = ((A + 1.0) + (A - 1.0) * tcos - beta * tsin);
            }

            // hishelf
            if (type == FilterType.HIGHSHELF)
            {
                b0 = (A * ((A + 1.0) + (A - 1.0) * tcos + beta * tsin));
                b1 = (-2.0 * A * ((A - 1.0) + (A + 1.0) * tcos));
                b2 = (A * ((A + 1.0) + (A - 1.0) * tcos - beta * tsin));
                a0 = ((A + 1.0) - (A - 1.0) * tcos + beta * tsin);
                a1 = (2.0 * ((A - 1.0) - (A + 1.0) * tcos));
                a2 = ((A + 1.0) - (A - 1.0) * tcos - beta * tsin);
            }
        }
        else
        {
            // other filters
            final double omega = 2.0 * temp_pi * frequency / sample_rate;
            final double tsin = Math.sin(omega);
            final double tcos = Math.cos(omega);

            if (q_is_bandwidth) alpha = tsin * Math.sinh(Math.log(2.0) / 2.0 * q * omega / tsin);
            else
                alpha = tsin / (2.0 * q);

            // lowpass
            if (type == FilterType.LOWPASS)
            {
                b0 = (1.0 - tcos) / 2.0;
                b1 = 1.0 - tcos;
                b2 = (1.0 - tcos) / 2.0;
                a0 = 1.0 + alpha;
                a1 = -2.0 * tcos;
                a2 = 1.0 - alpha;
            }

            // hipass
            if (type == FilterType.HIGHPASS)
            {
                b0 = (1.0 + tcos) / 2.0;
                b1 = -(1.0 + tcos);
                b2 = (1.0 + tcos) / 2.0;
                a0 = 1.0 + alpha;
                a1 = -2.0 * tcos;
                a2 = 1.0 - alpha;
            }

            // bandpass csg
            if (type == FilterType.BANDPASS_SKIRT)
            {
                b0 = tsin / 2.0;
                b1 = 0.0;
                b2 = -tsin / 2;
                a0 = 1.0 + alpha;
                a1 = -2.0 * tcos;
                a2 = 1.0 - alpha;
            }

            // bandpass czpg
            if (type == FilterType.BANDPASS_PEAK)
            {
                b0 = alpha;
                b1 = 0.0;
                b2 = -alpha;
                a0 = 1.0 + alpha;
                a1 = -2.0 * tcos;
                a2 = 1.0 - alpha;
            }

            // notch
            if (type == FilterType.NOTCH)
            {
                b0 = 1.0;
                b1 = -2.0 * tcos;
                b2 = 1.0;
                a0 = 1.0 + alpha;
                a1 = -2.0 * tcos;
                a2 = 1.0 - alpha;
            }

            // allpass
            if (type == FilterType.ALLPASS)
            {
                b0 = 1.0 - alpha;
                b1 = -2.0 * tcos;
                b2 = 1.0 + alpha;
                a0 = 1.0 + alpha;
                a1 = -2.0 * tcos;
                a2 = 1.0 - alpha;
            }
        }

        // set filter coeffs
        b0a0 = (b0 / a0);
        b1a0 = (b1 / a0);
        b2a0 = (b2 / a0);
        a1a0 = (a1 / a0);
        a2a0 = (a2 / a0);
    }
}
