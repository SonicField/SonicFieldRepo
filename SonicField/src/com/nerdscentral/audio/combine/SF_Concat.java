/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.List;

import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1,?s2:Sf.Concat ... forwards an SFData which is sample1 followed b sample2 ...
 * 
 * @author AlexTu
 * 
 */
public class SF_Concat implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        int len = 0;
        for (Object sfd : inList)
        {
            len += (Caster.makeSFSignal(sfd).getLength());
        }
        if (len > Integer.MAX_VALUE) throw new SFPL_RuntimeException(Messages.getString("SF_Concat.3"));  //$NON-NLS-1$
        SFData out = SFData.build(len);
        len = 0;
        for (Object sfd : inList)
        {
            SFSignal thisData = Caster.makeSFSignal(sfd);
            int thisLen = thisData.getLength();
            for (int i = 0; i < thisLen; ++i)
            {
                out.setSample(i + len, thisData.getSample(i));
            }
            len += thisLen;
        }
        return out;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Concat.4");  //$NON-NLS-1$
    }

}