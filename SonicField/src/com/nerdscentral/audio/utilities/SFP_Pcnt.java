/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.utilities;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.core.SFSingleTranslator;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SFP_Pcnt implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    private final double      volume;
    private final String      name;

    public static class Translator extends SFSingleTranslator
    {
        private final double volumeInner;

        Translator(SFSignal input, double volumeIn)
        {
            super(input);
            volumeInner = volumeIn;
        }

        @Override
        public double getSample(int index)
        {
            return getInputSample(index) * volumeInner;
        }

    }

    public SFP_Pcnt(int pcnt)
    {
        volume = pcnt / 100.0;
        name = pcnt > 0 ? ("Pcnt" + pcnt) : "Pcnt_" + -pcnt; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String Word()
    {
        return name;
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        SFSignal in = Caster.makeSFSignal(input);
        return new Translator(in, volume);
    }

    public static List<SFP_Pcnt> getAll()
    {
        List<SFP_Pcnt> ret = new ArrayList<>(202);
        for (int v = -100; v < 101; ++v)
        {
            ret.add(new SFP_Pcnt(v));
        }
        return ret;
    }

}