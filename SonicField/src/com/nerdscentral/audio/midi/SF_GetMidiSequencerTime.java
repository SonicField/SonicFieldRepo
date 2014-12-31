package com.nerdscentral.audio.midi;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.Sequencer;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_GetMidiSequencerTime implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_GetMidiSequencerTime.0"); //$NON-NLS-1$
    }

    @SuppressWarnings("resource")
    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        Sequencer sequencer = Caster.makeMidiSequencer(lin.get(0));
        long previousTime = Caster.makeLong(lin.get(1));
        long time = sequencer.getMicrosecondPosition() / 1000;
        List<Object> ret = new ArrayList<>(2);
        while (time == previousTime)
        {
            try
            {
                // 0.5 milliseconds
                Thread.sleep(0, 5000);
            }
            catch (InterruptedException e)
            {
                // yum
            }
            if (!sequencer.isRunning())
            {
                ret.add(0, (double) time);
                ret.add(1, false);
                return ret;
            }
            time = sequencer.getMicrosecondPosition() / 1000;
        }
        ret.add(0, (double) time);
        ret.add(1, true);
        return ret;
    }
}
