package com.nerdscentral.audio.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

import com.nerdscentral.sython.SFPL_RuntimeException;

public class MidiPlayer implements MetaEventListener, java.lang.AutoCloseable
{

    // Midi meta event
    public static final int END_OF_TRACK_MESSAGE = 47;

    private Sequencer       sequencer;

    private boolean         loop;

    private boolean         paused;

    /**
     * Creates a new MidiPlayer object.
     */
    public MidiPlayer()
    {
        try
        {
            sequencer = MidiSystem.getSequencer();
            descrDevice();
            sequencer.open();
            sequencer.addMetaEventListener(this);
        }
        catch (MidiUnavailableException ex)
        {
            sequencer = null;
        }
    }

    private void descrDevice()
    {
        Info deviceInfo = sequencer.getDeviceInfo();
        System.out.println(Messages.getString("MidiPlayer.2") + deviceInfo.getName() + Messages.getString("MidiPlayer.3") + deviceInfo.getDescription());  //$NON-NLS-1$ //$NON-NLS-2$
        for (Receiver r : sequencer.getReceivers())
        {
            System.out.println(Messages.getString("MidiPlayer.4") + r); //$NON-NLS-1$
        }
        for (Transmitter t : sequencer.getTransmitters())
        {
            System.out.println(Messages.getString("MidiPlayer.5") + t); //$NON-NLS-1$
        }
    }

    /**
     * Creates a new MidiPlayer object.
     * 
     * @throws SFPL_RuntimeException
     */
    @SuppressWarnings("resource")
    public MidiPlayer(int sequencerNo, int synthNo) throws SFPL_RuntimeException
    {
        try
        {
            Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice dev = MidiSystem.getMidiDevice(infos[sequencerNo]);
            if (dev instanceof Sequencer)
            {
                sequencer = (Sequencer) dev;
            }
            else
            {
                throw new SFPL_RuntimeException(
                                Messages.getString("MidiPlayer.0") + sequencerNo + Messages.getString("MidiPlayer.1")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            dev = MidiSystem.getMidiDevice(infos[synthNo]);

            Synthesizer synth = null;
            if (dev instanceof Synthesizer)
            {
                synth = (Synthesizer) dev;
            }
            else
            {
                throw new SFPL_RuntimeException("Device " + synthNo + " not a synthesiser"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            synth.open();
            Receiver synthReceiver = synth.getReceiver();
            Transmitter seqTransmitter = sequencer.getTransmitter();
            seqTransmitter.setReceiver(synthReceiver);
            descrDevice();
            sequencer.open();
            sequencer.addMetaEventListener(this);
        }
        catch (MidiUnavailableException ex)
        {
            sequencer = null;
        }
    }

    /**
     * Plays a sequence, optionally looping. This method returns immediately. The sequence is not played if it is invalid.
     */
    public void play(Sequence sequence, boolean loopIn)
    {
        if (sequencer != null && sequence != null && sequencer.isOpen())
        {
            try
            {
                sequencer.setSequence(sequence);
                sequencer.start();
                this.loop = loopIn;
            }
            catch (InvalidMidiDataException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public void play(Sequence sequence)
    {
        play(sequence, false);
    }

    /**
     * This method is called by the sound system when a meta event occurs. In this case, when the end-of-track meta event is
     * received, the sequence is restarted if looping is on.
     */
    @Override
    public void meta(MetaMessage event)
    {
        if (event.getType() == END_OF_TRACK_MESSAGE)
        {
            if (sequencer != null && sequencer.isOpen() && loop)
            {
                sequencer.start();
            }
        }
    }

    public void waitFor() throws InterruptedException
    {
        while (sequencer.isRunning())
        {
            Thread.sleep(1000);
            System.out.print('.');
        }
    }

    /**
     * Stops the sequencer and resets its position to 0.
     */
    public void stop()
    {
        if (sequencer != null && sequencer.isOpen())
        {
            sequencer.stop();
            sequencer.setMicrosecondPosition(0);
        }
    }

    /**
     * Closes the sequencer.
     */
    @Override
    public void close()
    {
        if (sequencer != null && sequencer.isOpen())
        {
            sequencer.close();
        }
    }

    /**
     * Sets the paused state. Music may not immediately pause.
     */
    public void setPaused(boolean pausedIn)
    {
        if (this.paused != pausedIn && sequencer != null && sequencer.isOpen())
        {
            this.paused = pausedIn;
            if (pausedIn)
            {
                sequencer.stop();
            }
            else
            {
                sequencer.start();
            }
        }
    }

    /**
     * Returns the paused state.
     */
    public boolean isPaused()
    {
        return paused;
    }
}