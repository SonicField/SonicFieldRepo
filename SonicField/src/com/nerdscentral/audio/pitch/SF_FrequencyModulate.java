/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_FrequencyModulate implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (SFSignal shape = Caster.makeSFSignal(l.get(1)); SFSignal sampleA = Caster.makeSFSignal(l.get(0));)
        {
            if (sampleA.getLength() != shape.getLength()) throw new SFPL_RuntimeException(Messages.getString("SF_Resample.1"));  //$NON-NLS-1$
            int len = sampleA.getLength();
            try (SFSignal ret = sampleA.replicateEmpty())
            {
                double pos = 0;
                // Perform a dry run to get the required timing correction
                for (int i = 0; i < len; ++i)
                {
                    // ret.setSample(i, sampleA.getSampleCubic(pos));
                    double sn = shape.getSample(i);
                    if (sn > 0)
                    {
                        sn += 1.0d;
                    }
                    else if (sn == 0)
                    {
                        sn = 1.0d;
                    }
                    else
                    {
                        sn = -sn + 1.0d;
                        sn = 1.0d / sn;
                    }
                    pos += sn;
                }
                double correction = (len - 1) / pos;
                // Now do it for real
                pos = 0;
                for (int i = 0; i < len; ++i)
                {
                    ret.setSample(i, sampleA.getSampleCubic(pos * correction));
                    double sn = shape.getSample(i);
                    if (sn > 0)
                    {
                        sn += 1.0d;
                    }
                    else if (sn == 0)
                    {
                        sn = 1.0d;
                    }
                    else
                    {
                        sn = -sn + 1.0d;
                        sn = 1.0d / sn;
                    }
                    pos += sn;
                }

                return Caster.prep4Ret(ret);
            }
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_FMModulate.0"); //$NON-NLS-1$
    }

}