/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.time;

import java.util.List;
import java.util.ArrayList;

import com.nerdscentral.audio.SFConstants;
import com.nerdscentral.audio.SFSignal;
import com.nerdscentral.audio.SFData;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Context;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;
import com.nerdscentral.audio.pitch.algorithm.SFRBJFilter;
import com.nerdscentral.audio.pitch.algorithm.SFRBJFilter.FilterType;

public class SF_AnalogueChorus implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_AnalogueChorus.0"); //$NON-NLS-1$
    }

    private static double sat(double x)
    {
        double y = x >= 0 ? x / (x + 1) : x / (1 - x);
        return y;
    }

    @Override
    public Object Interpret(Object input, SFPL_Context context) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        try (
	     SFSignal inR = Caster.makeSFSignal(lin.get(0));
	     SFSignal mod = Caster.makeSFSignal(lin.get(2))
	){
	    try(SFData in = SFData.realise(inR))
	    {
		double filt = 5000.0;
		if(lin.size()>4)
		{
		    filt=Caster.makeDouble(lin.get(5));
		}			
		if(inR instanceof SFData)
		{
		    in.__pos__();
		}	
		int delay = (int)((Caster.makeDouble(lin.get(1))) * SFConstants.SAMPLE_RATE_MS);
		double feedBack = Caster.makeDouble(lin.get(3));
		double drive    = Caster.makeDouble(lin.get(4));
		double r = in.getLength();
		double feedForward = 1.0 - feedBack;
		FilterType type = FilterType.LOWPASS;
		if(filt>15000)
		{
		    type = FilterType.ALLPASS;
		    filt = 5000.0;
		}
	        SFRBJFilter filter = new SFRBJFilter();
		filter.calc_filter_coeffs(type, filt, 1.0, 0);

		try (
		     SFSignal buff = in.replicateEmpty();
		     SFSignal outL = in.replicateEmpty();
		     SFSignal outR = in.replicateEmpty();
		){
		    for (int n = 0; n < r; ++n)
		    {
			buff.setSample(n,filter.filter(in.getSample(n)));
		    }
		    // reboot the filter
		    filter = new SFRBJFilter();
		    filter.calc_filter_coeffs(type, filt, 1.0, 0);
		    int last = 0;
		    for (int n = 0; n < r; ++n)
		    {
			double mix=0;
			int delayGet=n-delay-((int)(mod.getSample(n)*SFConstants.SAMPLE_RATE_MS));
			if(delayGet<r && delayGet>-1)
			{
			    mix=buff.getSample(delayGet);
			}
			double q=in.getSample(n);
			outL.setSample(n,(q+mix)/2.0);
			outR.setSample(n,(q-mix)/2.0);
			buff.setSample(n,drive*(buff.getSample(n)*feedForward+filter.filter(sat(mix))*feedBack));
		    }
		    Caster.prep4Ret(outL);
		    Caster.prep4Ret(outR);
		    List<SFSignal> ret=new ArrayList<>(2);
		    ret.add(outL);
		    ret.add(outR);
		    return ret;
		}
            }
        }
    }
}
