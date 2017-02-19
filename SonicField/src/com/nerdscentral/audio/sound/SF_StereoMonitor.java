/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio.sound;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import com.nerdscentral.audio.Messages;
import com.nerdscentral.audio.core.SFConstants;
import com.nerdscentral.audio.core.SFPL_RefPassThrough;
import com.nerdscentral.audio.core.SFSignal;
import com.nerdscentral.audio.volume.SF_Normalise;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFPL_Operator;
import com.nerdscentral.sython.SFPL_RuntimeException;

public class SF_StereoMonitor implements SFPL_Operator, SFPL_RefPassThrough
{

    private static final long serialVersionUID = 1L;

    @Override
    public String Word()
    {
        return Messages.getString("SF_StereoMonitor.0"); //$NON-NLS-1$
    }

    @Override
    public Object Interpret(Object input) throws SFPL_RuntimeException
    {
        List<Object> lin = Caster.makeBunch(input);
        if (lin.size() != 2) throw new SFPL_RuntimeException(Messages.getString("SF_StereoMonitor.1")); //$NON-NLS-1$
        try
        {
            SFSignal dataIn1a = Caster.makeSFSignal(lin.get(0));
            SFSignal dataIn2a = Caster.makeSFSignal(lin.get(1));
            SFSignal dataIn1 = SF_Normalise.doNormalisation(dataIn1a);
            SFSignal dataIn2 = SF_Normalise.doNormalisation(dataIn2a);

            AudioFormat af = new AudioFormat((float) SFConstants.SAMPLE_RATE, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            SourceDataLine source = (SourceDataLine) AudioSystem.getLine(info);
            source.open(af);
            source.start();
            byte[] buf = new byte[dataIn1.getLength() * 4];
            for (int i = 0; i < buf.length; ++i)
            {
                short sample = 0;
                if (i / 4 < dataIn1.getLength())
                {
                    sample = (short) (dataIn1.getSample(i / 4) * 32767.0);
                }
                buf[i] = (byte) (sample >> 8);
                buf[++i] = (byte) (sample & 0xFF);
                sample = 0;
                if (i / 4 < dataIn2.getLength())
                {
                    sample = (short) (dataIn2.getSample(i / 4) * 32767.0);
                }
                buf[++i] = (byte) (sample >> 8);
                buf[++i] = (byte) (sample & 0xFF);
            }
            source.write(buf, 0, buf.length);
            source.drain();
            source.stop();
            source.close();
            List<SFSignal> ret = new ArrayList<>();
            ret.add(dataIn1a);
            ret.add(dataIn2a);
            return ret;
        }
        catch (Exception e)
        {
            throw new SFPL_RuntimeException(Messages.getString("SF_Monitor.1"), e); //$NON-NLS-1$
        }

    }
}
