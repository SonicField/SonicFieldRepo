/* For license see LICENSE.txt and COPYING.txt in the root directory */
package com.nerdscentral.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.nerdscentral.audio.io.SFWavFileException;
import com.nerdscentral.audio.io.SFWavSubsystem;
import com.nerdscentral.sython.Caster;
import com.nerdscentral.sython.SFMaths;
import com.nerdscentral.sython.SFPL_RuntimeException;

/**
 * A place to collection functions for SonicField.
 * 
 * @author a1t
 * 
 */
public final class SF2JavaSound
{
    private final static int WRITE_BUFFER_SIZE = 8096;

    public final static List<String> getMixers()
    {
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < aInfos.length; i++)
        {
            ret.add(aInfos[i].getName());
        }
        return ret;
    }

    public final static Mixer getMixer(String name)
    {
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        Mixer ret = null;
        for (int i = 0; i < aInfos.length; i++)
        {
            if (aInfos[i].getName().equals(name))
            {
                ret = AudioSystem.getMixer(aInfos[i]);
            }
        }
        return ret;
    }

    public final static SFLineListener playFile(String fileName, Mixer mixer) throws UnsupportedAudioFileException,
                    IOException, LineUnavailableException
    {
        InputStream in = new FileInputStream(fileName);
        BufferedInputStream bin = new BufferedInputStream(in);
        AudioInputStream ain = AudioSystem.getAudioInputStream(bin);
        try (Clip clip = AudioSystem.getClip(mixer.getMixerInfo());)
        {
            clip.open(ain);
            SFLineListener ret;

            clip.addLineListener(ret = new SFLineListener()
            {

                private volatile boolean stopped = false;

                @Override
                public boolean hasStopped()
                {
                    return this.stopped;
                }

                @Override
                public void update(LineEvent arg0)
                {
                    if (arg0.getType() == LineEvent.Type.STOP) this.stopped = true;
                }

            });
            clip.start();
            return ret;
        }
    }

    public final static List<SFSignal> readFile(String fileName) throws SFPL_RuntimeException, UnsupportedAudioFileException,
                    IOException
    {
        InputStream in = new FileInputStream(fileName);
        BufferedInputStream bin = new BufferedInputStream(in);
        try (AudioInputStream ain = AudioSystem.getAudioInputStream(bin);)
        {
            return getChannels(ain);
        }
    }

    public final static List<SFSignal> getChannels(AudioInputStream ain) throws SFPL_RuntimeException, IOException
    {
        AudioFormat af = ain.getFormat();
        int nChannels = af.getChannels();

        AudioFormat outDataFormat = new AudioFormat((float) SFConstants.SAMPLE_RATE, 32, nChannels, true, true);
        if (AudioSystem.isConversionSupported(outDataFormat, ain.getFormat()))
        {
            try (AudioInputStream interalAs = AudioSystem.getAudioInputStream(outDataFormat, ain);)
            {
                double frameCount = ain.getFrameLength();
                frameCount = frameCount * SFConstants.SAMPLE_RATE / ain.getFormat().getFrameRate();
                float[][] data = new float[nChannels][(int) SFMaths.ceil(frameCount)];
                int expectedBuffSize = 4 * nChannels;
                byte[] buff = new byte[expectedBuffSize];
                for (int pos = 0; pos < frameCount; ++pos)
                {
                    if (expectedBuffSize != interalAs.read(buff))
                    {
                        throw new SFPL_RuntimeException(Messages.getString("SF2JavaSound.1")); //$NON-NLS-1$
                    }

                    int bufferPointer = 0;
                    for (int channel = 0; channel < nChannels; ++channel)
                    {
                        int sample = 0;
                        sample |= buff[bufferPointer++] & 255;
                        sample <<= 8;
                        sample |= buff[bufferPointer++] & 255;
                        sample <<= 8;
                        sample |= buff[bufferPointer++] & 255;
                        sample <<= 8;
                        sample |= buff[bufferPointer++] & 255;
                        data[channel][pos] = (((float) sample) / ((float) Integer.MAX_VALUE));
                    }
                }
                List<SFSignal> ret = new ArrayList<>(nChannels);
                for (int c = 0; c < nChannels; ++c)
                {
                    ret.add(SFData.build(data[c]));
                }
                return ret;
            }
        }
        throw new SFPL_RuntimeException(Messages.getString("SF2JavaSound.0")); //$NON-NLS-1$
    }

    public static void WriteWav(String fileName, List<Object> channels, boolean hiRes) throws IOException, SFWavFileException,
                    SFPL_RuntimeException
    {
        int sampleRate = (int) SFConstants.SAMPLE_RATE; // Samples per second
        int nChannels = channels.size();
        if (nChannels == 0) throw new SFPL_RuntimeException(Messages.getString("SF2JavaSound.2")); //$NON-NLS-1$

        // Calculate the number of frames required for specified duration, if
        // channels are not
        // the same length then the short channels will be zero'ed at the ends
        int numFrames = 0;
        for (int channelRR = 0; channelRR < nChannels; ++channelRR)
        {
            @SuppressWarnings("resource")
            SFSignal data = Caster.makeSFSignal(channels.get(0));
            int l = data.getLength();
            if (l > numFrames) numFrames = l;
        }
        // Create a wav file with the name specified as the first argument
        SFWavSubsystem wavFile = SFWavSubsystem.newWavFile(new File(fileName), nChannels, numFrames, hiRes ? 32 : 16,
                        sampleRate);

        // Create a buffer of 100 frames
        double[][] buffer = new double[nChannels][WRITE_BUFFER_SIZE];

        // Initialise a local frame counter
        int frameCounter = 0;

        List<SFSignal> datas = new ArrayList<>();
        for (int c = 0; c < channels.size(); ++c)
        {
            datas.add(Caster.makeSFSignal(channels.get(c)));
        }
        // Loop until all frames written
        while (frameCounter < numFrames)
        {
            // Determine how many frames to write, up to a maximum of the buffer
            // size
            long remaining = wavFile.getFramesRemaining();
            int toWrite = (remaining > WRITE_BUFFER_SIZE) ? WRITE_BUFFER_SIZE : (int) remaining;

            // Fill the buffer, one tone per channel
            for (int s = 0; s < toWrite; s++, frameCounter++)
            {
                for (int channelRR = 0; channelRR < nChannels; ++channelRR)
                {
                    @SuppressWarnings("resource")
                    SFSignal d = datas.get(channelRR);
                    double dd = d.getLength() > frameCounter ? d.getSample(frameCounter) : 0;
                    buffer[channelRR][s] = dd;
                }
            }

            // Write the buffer
            wavFile.writeFrames(buffer, toWrite);
        }

        for (int channelRR = 0; channelRR < nChannels; ++channelRR)
        {
            SFSignal d = datas.get(channelRR);
            // Manually clean resournce
            d.close();
        }
        // Close the wavFile
        wavFile.close();
    }
}
