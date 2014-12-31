package com.nerdscentral.audio.midi;

import javax.sound.midi.Track;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_DescribeMidiTrack implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_DescribeMidiTrack.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        Track track = Caster.makeMidiTrack(input);
        StringBuilder ret = new StringBuilder();
        ret.append(Messages.getString("SF_DescribeMidiTrack.1")); //$NON-NLS-1$
        ret.append(track.toString());
        ret.append(System.lineSeparator());

        ret.append(Messages.getString("SF_DescribeMidiTrack.2")); //$NON-NLS-1$
        ret.append(track.size());
        ret.append(System.lineSeparator());

        ret.append(Messages.getString("SF_DescribeMidiTrack.3")); //$NON-NLS-1$
        ret.append(track.ticks());
        ret.append(System.lineSeparator());

        return ret.toString();
    }
}
