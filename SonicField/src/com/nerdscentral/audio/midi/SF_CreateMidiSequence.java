package com.nerdscentral.audio.midi;

import javax.sound.midi.InvalidMidiDataException;

import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_CreateMidiSequence implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_CreateMidiSequence.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try
        {
            return MidiFunctions.createSequence();
        }
        catch (InvalidMidiDataException e)
        {
            throw new SFPL_RuntimeException(e);
        }
    }
}
