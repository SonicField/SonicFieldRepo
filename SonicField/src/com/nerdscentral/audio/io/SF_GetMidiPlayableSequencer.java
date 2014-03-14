package com.nerdscentral.audio.io;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_GetMidiPlayableSequencer implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_GetMidiPlayableSequencer.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        Sequence sequence = Caster.makeMidiSequence(lin.get(0));
        int deviceNo = Caster.makeInt(lin.get(1));
        try
        {
            return MidiFunctions.getPlayableSequencer(sequence, deviceNo);
        }
        catch (MidiUnavailableException | InvalidMidiDataException e)
        {
            throw new SFPL_RuntimeException(e);
        }
    }
}
