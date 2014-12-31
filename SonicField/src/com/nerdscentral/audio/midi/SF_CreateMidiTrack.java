package com.nerdscentral.audio.midi;

import javax.sound.midi.Sequence;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_CreateMidiTrack implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_CreateMidiTrack.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        Sequence sequence = Caster.makeMidiSequence(input);
        return MidiFunctions.createTrack(sequence);
    }
}
