/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_RectangularJoin implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_RectangularJoin.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        throw new RuntimeException("Broken - should use indices of 2."); //$NON-NLS-1$
        // List<Object> l = Caster.makeBunch(input);
        // SFSignal sampleA = Caster.makeSFSignal(l.get(0));
        // SFSignal sampleB = Caster.makeSFSignal(l.get(1));
        // int lenA = sampleA.getLength();
        // int lenB = sampleB.getLength();
        // int len = lenA > lenB ? lenA : lenB;
        // SFSignal out = SFData.build(len, false);
        // for (int i = 0; i < len; i += 2)
        // {
        // double a = i >= lenA ? 0 : sampleA.getSample(i);
        // double b = i >= lenB ? 0 : sampleB.getSample(i);
        // int j = i + 1;
        // out.setSample(i, a);
        // out.setSample(j, b);
        // }
        // return out;
    }
}
