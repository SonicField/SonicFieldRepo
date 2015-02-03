package com.nerdscentral.audio.midi;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.sound.midi.ControllerEventListener;
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
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import com.nerdscentral.sython.SFPL_RuntimeException;

public class MidiPlayer implements MetaEventListener, java.lang.AutoCloseable, ControllerEventListener
{

    // Midi meta event
    public static final int END_OF_TRACK_MESSAGE = 47;

    private Sequencer       sequencer;

    private boolean         loop;

    private boolean         paused;

    private MidiDevice      synth;

    static List<MidiDevice> devices              = new LinkedList<>();

    static
    {
        Thread jvmShutdownHook = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                System.out.println("In hook"); //$NON-NLS-1$
                for (MidiDevice dev : devices)
                {
                    if (dev.isOpen())
                    {
                        System.out.println("Closing: " + dev); //$NON-NLS-1$
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(jvmShutdownHook);
    }

    private void init() throws MidiUnavailableException
    {
        descrDevice();
        sequencer.open();
        sequencer.addMetaEventListener(this);
        int[] conts = { ShortMessage.NOTE_ON, ShortMessage.NOTE_OFF };
        int[] added = sequencer.addControllerEventListener(this, conts);
        System.out.println(Messages.getString("MidiPlayer.8") + added.length + Messages.getString("MidiPlayer.9")); //$NON-NLS-1$ //$NON-NLS-2$
        devices.add(sequencer);
    }

    /**
     * Creates a new MidiPlayer object.
     */
    public MidiPlayer()
    {
        try
        {
            sequencer = MidiSystem.getSequencer();
            init();
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

            synth = dev;
            synth.open();
            Receiver synthReceiver = synth.getReceiver();
            Transmitter seqTransmitter = sequencer.getTransmitter();
            seqTransmitter.setReceiver(synthReceiver);
            init();
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

    public void manual(Sequence sequence) throws IOException
    {
        if (sequencer != null && sequence != null && sequencer.isOpen())
        {
            try
            {
                sequencer.setSequence(sequence);
                System.out.println(Messages.getString("MidiPlayer.6")); //$NON-NLS-1$
                System.in.read();
                System.out.println("Playing"); //$NON-NLS-1$
                sequencer.start();
                System.out.println(Messages.getString("MidiPlayer.7")); //$NON-NLS-1$
                System.in.read();
                if (sequencer.isRunning())
                {
                    sequencer.stop();
                }
                System.out.println("Stopped"); //$NON-NLS-1$
            }
            catch (InvalidMidiDataException ex)
            {
                ex.printStackTrace();
            }
        }
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
            System.out.println("End Of Track"); //$NON-NLS-1$
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

    @Override
    public void controlChange(ShortMessage event)
    {
        System.out.println(Messages.getString("MidiPlayer.10")); //$NON-NLS-1$
        if (event.getCommand() == ShortMessage.NOTE_ON)
        {
            System.out.println("Note On  " + event.getData1()); //$NON-NLS-1$
        }
        else
        {
            System.out.println("Note Off " + event.getData2()); //$NON-NLS-1$

        }

    }

}