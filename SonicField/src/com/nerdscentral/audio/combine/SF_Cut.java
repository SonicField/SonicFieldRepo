/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_Cut implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);

        SFSignal data = Caster.makeSFSignal(inList.get(2));
        double start = Caster.makeDouble(inList.get(0));
        double end = Caster.makeDouble(inList.get(1));
        int istart = (int) (start * SFConstants.SAMPLE_RATE_MS);
        int iend = (int) (end * SFConstants.SAMPLE_RATE_MS);
        if (iend >= data.getLength())
        {
            iend = data.getLength() - 1;
        }
        SFSignal out = SFData.build(iend - istart, false);
        for (int i = istart; i < iend; ++i)
        {
            out.setSample(i - istart, data.getSample(i));
        }
        return out;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Cut.0"); //$NON-NLS-1$
    }

}