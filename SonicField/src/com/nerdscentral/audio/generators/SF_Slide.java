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

public class SF_Slide implements SFPL_Operator
{
    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_Slide.0");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {

        List<Object> l = Caster.makeBunch(input);
        double totalTime = 0;
        final double PI2 = SFMaths.PI * 2.0d;
        if (l.size() < 2) throw new SFPL_RuntimeException(Messages.getString("SF_Slide.1"));  //$NON-NLS-1$
        for (Object o : l)
        {
            List<Object> el = Caster.makeBunch(o);
            if (el.size() != 2) throw new SFPL_RuntimeException(Messages.getString("SF_Slide.2"));  //$NON-NLS-1$
        }
        totalTime = Caster.makeDouble(Caster.makeBunch(l.get(l.size() - 1)).get(0));
        SFSignal data = SFData.build((int) (totalTime * SFConstants.SAMPLE_RATE / 1000.0d), true);
        int position = 0;
        double sinPos = 0;
        double scl = PI2 / SFConstants.SAMPLE_RATE;
        for (int i = 0; i < l.size() - 1; ++i)
        {
            List<Object> start = Caster.makeBunch(l.get(i));
            List<Object> end = Caster.makeBunch(l.get(i + 1));
            int startX = Caster.makeInt(start.get(0));
            int endX = Caster.makeInt(end.get(0));
            double startY = Caster.makeDouble(start.get(1));
            double endY = Caster.makeDouble(end.get(1));
            double len = (endX - startX) * SFConstants.SAMPLE_RATE / 1000.0d;
            double diff = endY - startY;
            double scalb = diff / len;
            for (double x = 0; x < len; ++x)
            {
                double pitch = x * scalb + startY;
                data.setSample(position, SFMaths.sin(sinPos));
                sinPos += scl * pitch;
                ++position;
            }
        }
        return data;
    }
}
