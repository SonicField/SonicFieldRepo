package com.nerdscentral.audio.pitch;

import java.util.List;

import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * This filter is designed to be very stable indeed at low frequencies and never produce infinities, it is also asymetric
 * (filtering falling singals differently from rising). It is not supposed to be a nice audio filter but used for special
 * effects. See SF_Follow - the envelope follower.
 * 
 * @author alexanderturner
 * 
 */
public class SF_FollowingLowPass implements SFPL_Operator
{

    private static final long serialVersionUID = 1L;

    class Follower
    {
        private final double attack_coef;
        private final double release_coef;
        private double       envelope;

        Follower(double attack, double release)
        {
            attack_coef = Math.exp(Math.log(0.01) / (attack * SFConstants.SAMPLE_RATE * 0.001));
            release_coef = Math.exp(Math.log(0.01) / (release * SFConstants.SAMPLE_RATE * 0.001));
            envelope = 0.0;
        }

        double step(double in)
        {
            if (in > envelope) envelope = attack_coef * (envelope - in) + in;
            else
                envelope = release_coef * (envelope - in) + in;
            return envelope;
        }
    }

    @Override
    public String Word()
    {
        return Messages.getString("SF_FollowingLowPass.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        double attack = Caster.makeDouble(lin.get(1));
        double release = Caster.makeDouble(lin.get(2));
        SFSignal signal = Caster.makeSFSignal(lin.get(0));
        Follower fol = new Follower(attack, release);
        SFSignal ret = signal.replicateEmpty();
        for (int index = 0; index < signal.getLength(); ++index)
        {
            ret.setSample(index, fol.step(signal.getSample(index)));
        }
        return ret;
    }
}