package com.nerdscentral.audio;

import com.nerdscentral.sython.SFPL_RuntimeException;

public abstract class SFGenerator extends SFSignal
{

    /* Override these to create a generator
     * ====================================
     */

    @Override
    public abstract double getSample(int index);

    @Override
    public abstract int getLength();

    /* The rest of the class
     * =====================
     */

    @Override
    public boolean isKilled()
    {
        // cannot swap out a generator
        return true;
    }

    @Override
    public SFSignal replicate()
    {
        return SFData.realise(this);
    }

    @Override
    public double setSample(int index, double value)
    {
        throw new RuntimeException(Messages.getString("SFGenerator.1")); //$NON-NLS-1$
    }

    @Override
    public void setAt(int pos, SFSignal data2) throws SFPL_RuntimeException
    {
        throw new RuntimeException(Messages.getString("SFGenerator.2")); //$NON-NLS-1$
    }

    @Override
    public void setFrom(int pos, SFSignal data2) throws SFPL_RuntimeException
    {
        throw new RuntimeException(Messages.getString("SFGenerator.3")); //$NON-NLS-1$
    }

    @Override
    public double[] getDataInternalOnly()
    {
        double[] ret = new double[getLength()];
        int len = getLength();
        for (int index = 0; index < len; ++len)
        {
            ret[index] = getSample(index);
        }
        return ret;
    }

}
