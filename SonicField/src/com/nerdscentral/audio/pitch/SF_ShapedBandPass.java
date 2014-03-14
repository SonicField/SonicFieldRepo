/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.pitch;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator.NPoleFilterDef;
import com.nerdscentral.audio.pitch.algorithm.SFFilterGenerator.NPoleFilterDefListNode;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_ShapedBandPass extends SFNPoleFilterOperator implements SFPL_Operator
{

    private static final double minDifference    = 1.0005777895;
    private static final long   serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_ShapedBandPass.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        try (
            SFSignal x = Caster.makeSFSignal(l.get(0));
            SFSignal frequencyAShape = Caster.makeSFSignal(l.get(1));
            SFSignal frequencyBShape = Caster.makeSFSignal(l.get(2));)
        {
            double order = Caster.makeDouble(l.get(3));
            double frequencyA = 0;
            double frequencyB = 0;
            try (SFSignal y = x.replicateEmpty();)
            {
                List<SFFilterGenerator.NPoleFilterDefListNode> filters = new ArrayList<>();
                int length = frequencyAShape.getLength();
                if (frequencyBShape.getLength() != length)
                {
                    throw new SFPL_RuntimeException(Messages.getString("SF_ShapedBandPass.1")); //$NON-NLS-1$
                }
                if (x.getLength() != length)
                {
                    throw new SFPL_RuntimeException(Messages.getString("SF_ShapedBandPass.2")); //$NON-NLS-1$
                }
                for (int index = 0; index < length; ++index)
                {
                    double a = frequencyAShape.getSample(index);
                    double b = frequencyBShape.getSample(index);
                    double ar = a / frequencyA;
                    if (ar < 1.0) ar = 1.0 / ar;
                    double br = b / frequencyB;
                    if (br < 1.0) br = 1.0 / br;
                    if (index == 0 || ar > minDifference || br > minDifference)
                    {
                        // recalculate
                        frequencyA = a;
                        frequencyB = b;
                        // System.out.println("" + a + "," + b);
                        NPoleFilterDef fd = SFFilterGenerator.computeButterworthNBP(frequencyA, frequencyB, (int) order);
                        NPoleFilterDefListNode node = new NPoleFilterDefListNode();
                        node.setDefinition(fd);
                        node.setPosition(index);
                        filters.add(node);
                    }
                }
                filterLoop(x, y, filters);
                return Caster.prep4Ret(y);
            }
        }
    }
}
