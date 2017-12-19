/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFData;
import com.nerdscentral.audio.core.SFMultipleTranslator;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * ?s1,-3:sf.ExponentialVolume !s1_normal... forwards an SFData less 3 db ...
 * 
 * @author AlexTu
 * 
 */
public class SF_MixAt implements SFPL_Operator
{
    private static final int TRANS_CUTTOFF = 16;

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
    public Object Interpret(final Object input) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        int nMembers = inList.size();
        List<SFSignal> signals = new ArrayList<>(nMembers);
        List<Integer> offsets = new ArrayList<>(nMembers);
        boolean anyRealised = false;
        for (Object each : inList)
        {
            List<Object> dataList = Caster.makeBunch(each);
            double offset = Caster.makeDouble(dataList.get(1)) * SFConstants.SAMPLE_RATE_MS;
            offsets.add((int) SFMaths.floor(offset));
            SFSignal signal = Caster.makeSFSignal(dataList.get(0));
            signals.add(signal);
            anyRealised = anyRealised || signal.isRealised();
        }
        int count = signals.size();
        if (anyRealised || count > TRANS_CUTTOFF)
        {
            return largeMix(signals, offsets);
        }
        Translator ret = new Translator(signals, offsets);
        return ret;
    }

    public static SFSignal largeMix(List<SFSignal> signals, List<Integer> offsets)
    {
        int count = signals.size();

        int length = 0;
        for (int i = 0; i < count; ++i)
        {
            int tl = offsets.get(i);
            tl += signals.get(i).getLength();
            if (tl > length) length = tl;
        }
        SFData out = SFData.build(length, true);
        for (int i = 0; i < count; ++i)
        {
            int at = offsets.get(i);
            SFSignal in = signals.get(i);
            out.operateOnto(at, in, SFData.OPERATION.ADD);
        }
        return out;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_MixAt.0"); //$NON-NLS-1$
    }

}