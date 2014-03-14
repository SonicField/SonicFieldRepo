package com.nerdscentral.audio.volume;

class Decimator
{

    private double R1, R2, R3, R4, R5, R6, R7, R8, R9;
    private final double h0, h1, h3, h5, h7, h9;

    Decimator()
    {
        h0 = (8192 / 16384.0);
        h1 = (5042 / 16384.0);
        h3 = (-1277 / 16384.0);
        h5 = (429 / 16384.0);
        h7 = (-116 / 16384.0);
        h9 = (18 / 16384.0);
    }

    double Calc(final double d, final double e)
    {
        double h9x0 = h9 * d;
        double h7x0 = h7 * d;
        double h5x0 = h5 * d;
        double h3x0 = h3 * d;
        double h1x0 = h1 * d;
        double R10 = R9 + h9x0;
        R9 = R8 + h7x0;
        R8 = R7 + h5x0;
        R7 = R6 + h3x0;
        R6 = R5 + h1x0;
        R5 = R4 + h1x0 + h0 * e;
        R4 = R3 + h3x0;
        R3 = R2 + h5x0;
        R2 = R1 + h7x0;
        R1 = h9x0;
        return R10;
    }
}
