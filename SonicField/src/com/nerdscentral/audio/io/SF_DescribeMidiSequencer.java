package com.nerdscentral.audio.io;

import javax.sound.midi.Sequencer;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_DescribeMidiSequencer implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_DescribeMidiSequencer.15"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        try (Sequencer sequencer = Caster.makeMidiSequencer(input))
        {
            StringBuilder ret = new StringBuilder();

            ret.append(Messages.getString("SF_DescribeMidiSequencer.0")); //$NON-NLS-1$
            ret.append(sequencer.getLoopCount());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.1")); //$NON-NLS-1$
            ret.append(sequencer.getLoopEndPoint());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.2")); //$NON-NLS-1$
            ret.append(sequencer.getLoopStartPoint());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.3")); //$NON-NLS-1$
            ret.append(sequencer.getMaxReceivers());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.4")); //$NON-NLS-1$
            ret.append(sequencer.getMaxTransmitters());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.5")); //$NON-NLS-1$
            ret.append(sequencer.getMicrosecondLength());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.6")); //$NON-NLS-1$
            ret.append(sequencer.getMicrosecondPosition());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.7")); //$NON-NLS-1$
            ret.append(sequencer.getTempoFactor());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.8")); //$NON-NLS-1$
            ret.append(sequencer.getTempoInBPM());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.9")); //$NON-NLS-1$
            ret.append(sequencer.getTempoInMPQ());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.10")); //$NON-NLS-1$
            ret.append(sequencer.getTickLength());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.11")); //$NON-NLS-1$
            ret.append(sequencer.getTickPosition());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.12")); //$NON-NLS-1$
            ret.append(sequencer.isOpen());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.13")); //$NON-NLS-1$
            ret.append(sequencer.isRecording());
            ret.append(System.lineSeparator());

            ret.append(Messages.getString("SF_DescribeMidiSequencer.14")); //$NON-NLS-1$
            ret.append(sequencer.isRunning());
            ret.append(System.lineSeparator());

            return ret.toString();
        }
    }
}
