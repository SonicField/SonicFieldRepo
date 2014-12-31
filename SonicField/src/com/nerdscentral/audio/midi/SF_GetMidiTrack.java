package com.nerdscentral.audio.midi;

import java.util.List;

import javax.sound.midi.Sequence;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_GetMidiTrack implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_GetMidiTrack.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        Sequence sequence = Caster.makeMidiSequence(lin.get(0));
        int trackNo = Caster.makeInt(lin.get(1));
        return MidiFunctions.getSequenceTrack(sequence, trackNo);
    }
}
