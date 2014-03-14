package com.nerdscentral.audio.io;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Track;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_AddMidiNote implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_AddMidiNote.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        Track track = Caster.makeMidiTrack(lin.get(0));
        int channel = Caster.makeInt(lin.get(1));
        int note = Caster.makeInt(lin.get(2));
        int time = Caster.makeInt(lin.get(3));
        int length = Caster.makeInt(lin.get(4));
        int velocity = Caster.makeInt(lin.get(5));
        try
        {
            MidiFunctions.addNote(track, time, length, channel, note, velocity);
        }
        catch (InvalidMidiDataException e)
        {
            throw new SFPL_RuntimeException(e);
        }
        return track;
    }
}
