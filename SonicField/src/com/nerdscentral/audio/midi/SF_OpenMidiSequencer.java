package com.nerdscentral.audio.midi;

import javax.sound.midi.MidiUnavailableException;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_OpenMidiSequencer implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_OpenMidiSequencer.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try
        {
            MidiFunctions.openSequencer(Caster.makeMidiSequencer(input));
        }
        catch (MidiUnavailableException e)
        {
            throw new SFPL_RuntimeException(e);
        }
        return input;
    }
}
