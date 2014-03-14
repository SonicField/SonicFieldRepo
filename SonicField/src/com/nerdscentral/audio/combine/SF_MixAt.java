/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFMultipleTranslator;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1,-3:Sf.Volume !s1_normal... forwards an SFData less 3 db ...
 * 
 * @author AlexTu
 * 
 */
public class SF_MixAt implements SFPL_Operator
{

    static class Translator extends SFMultipleTranslator
    {
        private final int length;

        @Override
        public int getLength()
        {
            return length;
        }

        final int[] offsets;

        protected Translator(List<SFSignal> input, List<Integer> offsetsIn)
        {
            super(input);
            offsets = new int[offsetsIn.size()];
            int llength = 0;
            for (int i = 0; i < offsets.length; ++i)
            {
                offsets[i] = offsetsIn.get(i);
                int l = input.get(i).getLength() + offsets[i];
                if (l > llength) llength = l;
            }
            length = llength;
        }

        @Override
        public double getSample(int index)
        {
            double ret = 0;
            for (int sig = 0; sig < getNMembers(); ++sig)
            {
                ret += getInputSample(sig, index - offsets[sig]);
            }
            return ret;
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        int nMembers = inList.size();
        List<SFSignal> signals = new ArrayList<>(nMembers);
        List<Integer> offsets = new ArrayList<>(nMembers);
        for (Object each : inList)
        {
            List<Object> dataList = Caster.makeBunch(each);
            double offset = Caster.makeDouble(dataList.get(1)) * SFConstants.SAMPLE_RATE_MS;
            offsets.add((int) SFMaths.floor(offset));
            signals.add(Caster.makeSFSignal(dataList.get(0)));
        }
        return new Translator(signals, offsets);
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_MixAt.0"); //$NON-NLS-1$
    }

}