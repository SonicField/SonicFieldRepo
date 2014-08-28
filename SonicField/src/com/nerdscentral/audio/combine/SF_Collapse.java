/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.combine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFData;
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
public class SF_Collapse implements SFPL_Operator
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    // Manual reasource manangement - note this is not exception safe
    public Object Interpret(final Object input, final SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> inList = Caster.makeBunch(input);
        HashMap<SFSignal, List<Integer>> atoms = new HashMap<>();
        List<SFSignal> signals = new ArrayList<>(inList.size());
        for (Object each : inList)
        {
            List<Object> dataList = Caster.makeBunch(each);
            double offset = Caster.makeDouble(dataList.get(1)) * SFConstants.SAMPLE_RATE_MS;
            Integer at = (int) SFMaths.floor(offset);
            @SuppressWarnings("resource")
            SFSignal sig = Caster.makeSFSignal(dataList.get(0));
            List<Integer> l = atoms.get(sig);
            if (l == null)
            {
                l = new ArrayList<>();
                atoms.put(sig, l);
            }
            l.add(at);
            signals.add(sig);
        }
        List<List<Object>> ret = new ArrayList<>();
        for (Entry<SFSignal, List<Integer>> sl : atoms.entrySet())
        {
            @SuppressWarnings("resource")
            SFSignal sig = sl.getKey();
            List<Integer> ats = sl.getValue();
            if (ats.size() == 1)
            {
                // Nothing to collapse
                List<Object> pair = new ArrayList<>();
                pair.add(sig);
                pair.add(ats.get(0));
                ret.add(pair);
                sig.__pos__();
            }
            else
            {
                int len = sig.getLength();
                int max = 0;
                int min = Integer.MAX_VALUE;
                for (Integer i : sl.getValue())
                {
                    int ii = i.intValue();
                    if (ii > max) max = ii;
                    if (ii < min) min = ii;
                }
                len += max - min;
                double[] cacheOut = new double[len];
                double[] cacheIn = sig.getDataInternalOnly();
                for (Integer i : sl.getValue())
                {
                    int offSet = i - min;
                    for (int ip = 0; ip < cacheIn.length; ++ip)
                    {
                        cacheOut[ip + offSet] += cacheIn[ip];
                    }
                }
                List<Object> pair = new ArrayList<>();
                pair.add(SFData.build(cacheOut));
                pair.add(min / SFConstants.SAMPLE_RATE_MS);
                ret.add(pair);
            }
        }
        for (SFSignal s : signals)
        {
            s.close();
        }

        return ret;
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_Collapse.0"); //$NON-NLS-1$
    }

}