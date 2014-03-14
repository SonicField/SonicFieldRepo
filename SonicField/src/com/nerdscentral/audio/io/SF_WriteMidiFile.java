/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.io;

import java.io.IOException;
import java.util.List;

import javax.sound.midi.Sequence;




import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;


public class SF_WriteMidiFile implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        Sequence sequence = Caster.makeMidiSequence(inList.get(0));
        String fileName = Caster.makeString(inList.get(1));
        try
        {
            MidiFunctions.saveSequence(sequence, fileName);
            return sequence;
        }
        catch (IOException e)
        {
            throw new SFPL_RuntimeException(e);
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_WriteMidiFile.0"); //$NON-NLS-1$
    }

}