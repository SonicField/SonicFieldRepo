/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.List;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_CrossMultiply implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_CrossMultiply.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (SFSignal sampleA = Caster.makeSFSignal(l.get(0)); SFSignal sampleB = Caster.makeSFSignal(l.get(1)))
        {
            int lenA = sampleA.getLength();
            int lenB = sampleB.getLength();
            int len = lenA > lenB ? lenA : lenB;
            try (SFData out = SFData.build(len))
            {
                for (int i = 0; i < len; i += 2)
                {
                    double reA = i >= lenA ? 0 : sampleA.getSample(i);
                    double reB = i >= lenB ? 0 : sampleB.getSample(i);
                    int j = i + 1;
                    double imA = j >= lenA ? 0 : sampleA.getSample(j);
                    double imB = j >= lenB ? 0 : sampleB.getSample(j);
                    out.setSample(i, reA * reB - imA * imB);
                    out.setSample(j, imA * reB + imB * reA);
                }
                return Caster.prep4Ret(out);
            }
        }
    }
}
