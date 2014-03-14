package com.nerdscentral.audio.io;

import javax.sound.midi.Sequence;

import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_DescribeMidiSequence implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_DescribeMidiSequence.4");  //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        Sequence sequence = Caster.makeMidiSequence(input);
        StringBuilder ret = new StringBuilder();
        ret.append(Messages.getString("SF_DescribeMidiSequence.0"));  //$NON-NLS-1$
        ret.append(sequence.getDivisionType());
        ret.append(System.lineSeparator());

        ret.append(Messages.getString("SF_DescribeMidiSequence.1"));  //$NON-NLS-1$
        ret.append(sequence.getMicrosecondLength());
        ret.append(System.lineSeparator());

        ret.append(Messages.getString("SF_DescribeMidiSequence.2"));  //$NON-NLS-1$
        ret.append(sequence.getTickLength());
        ret.append(System.lineSeparator());

        ret.append(Messages.getString("SF_DescribeMidiSequence.3"));  //$NON-NLS-1$
        ret.append(sequence.getResolution());
        ret.append(System.lineSeparator());

        return ret.toString();
    }
}
