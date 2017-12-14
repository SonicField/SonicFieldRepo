/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.generators;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_NumericShape implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_NumericShape.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> l = Caster.makeBunch(input);
        double totalTime = 0;
        if (l.size() < 2) throw new SFPL_RuntimeException(Messages.getString("SF_NumericShape.1"));  //$NON-NLS-1$
        for (Object o : l)
        {
            List<Object> el = Caster.makeBunch(o);
            if (el.size() != 2) throw new SFPL_RuntimeException(Messages.getString("SF_NumericShape.2"));  //$NON-NLS-1$
        }
        totalTime = Caster.makeDouble(Caster.makeBunch(l.get(l.size() - 1)).get(0));
        SFSignal shape = SFData.build((int) (totalTime * SFConstants.SAMPLE_RATE / 1000.0d), false);
        int mustEnd = shape.getLength() - 1;
        int position = 0;
        for (int i = 0; i < l.size() - 1; ++i)
        {
            List<Object> start = Caster.makeBunch(l.get(i));
            List<Object> end = Caster.makeBunch(l.get(i + 1));
            double startX = Caster.makeDouble(start.get(0));
            double endX = Caster.makeDouble(end.get(0));
            double startY = Caster.makeDouble(start.get(1));
            double endY = Caster.makeDouble(end.get(1));
            if (startX > endX)
            {
                throw new RuntimeException(Messages.getString("SF_NumericShape.3") + startX + Messages.getString("SF_NumericShape.4") + endX); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (startX < 0) throw new RuntimeException(Messages.getString("SF_NumericShape.5")); //$NON-NLS-1$
            double len = (endX - startX) * SFConstants.SAMPLE_RATE / 1000.0d;
            double diff = endY - startY;
            double min = SFMaths.min(len, shape.getLength());
            for (double x = 0; x < min; ++x)
            {
                double y = diff * x / len + startY;
                if (position > mustEnd) break;
                shape.setSample(position++, y);
            }
        }
        return shape;
    }
}
