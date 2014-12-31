package com.nerdscentral.audio.midi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import com.nerdscentral.sython.SFPL_RuntimeException;

public class MidiFunctions
{

    public static final int      NOTE_ON    = 0x90;
    public static final int      NOTE_OFF   = 0x80;
    public static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$

    public Sequence preProcessChannels(Sequence seqIn)
    {
        return null;
    }

    public static List<Object> readMidiFile(String fileName) throws SFPL_RuntimeException
    {
        Sequence sequence = null;
        try
        {
            sequence = MidiSystem.getSequence(new File(fileName));
        }
        catch (InvalidMidiDataException | IOException e)
        {
            throw new SFPL_RuntimeException(e);
        }

        List<Object> table = new ArrayList<>();
        int trackNumber = 0;
        for (Track track : sequence.getTracks())
        {
            trackNumber++;
            List<Object> column = new ArrayList<>();
            table.add(column);
            System.out.println("Track " + trackNumber + ": size = " + track.size()); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println();
            HashMap<Integer, Stack<ArrayList<Object>>> onMap = new HashMap<>();
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                System.out.print("@" + event.getTick() + " "); //$NON-NLS-1$ //$NON-NLS-2$
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage)
                {
                    ShortMessage sm = (ShortMessage) message;
                    System.out.print("Channel: " + sm.getChannel() + " "); //$NON-NLS-1$ //$NON-NLS-2$
                    if (sm.getCommand() == NOTE_ON)
                    {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        if (velocity > 0)
                        {
                            ArrayList<Object> row = new ArrayList<>();
                            row.add((double) event.getTick());
                            row.add((double) 0);
                            row.add((double) note);
                            row.add((double) key);
                            row.add((double) velocity);
                            if (!onMap.containsKey(key))
                            {
                                onMap.put(key, new Stack<ArrayList<Object>>());
                            }
                            Stack<ArrayList<Object>> st = onMap.get(key);
                            if (st.size() > 0)
                            {
                                System.err.println("Warning: Overlapping Notes Detected \r\n    Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            }
                            else
                            {
                                System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            }
                            st.push(row);
                            // System.out.println(onMap);
                        }
                        else
                        {
                            System.out.println("Note Zero, " + noteName + octave + " key=" + key + " velocity: " + velocity); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            ArrayList<Object> row = onMap.get(key).pop();
                            row.set(1, (double) event.getTick());
                            column.add(row);
                            // onMap.remove(noteName);
                        }
                    }
                    else if (sm.getCommand() == NOTE_OFF)
                    {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ArrayList<Object> row = onMap.get(key).pop();
                        // ArrayList<Object> row = onMap.get(key);
                        row.set(1, (double) event.getTick());
                        column.add(row);
                        // onMap.remove(noteName);
                    }
                    else
                    {
                        System.out.println("Command:" + sm.getCommand() + "," + sm.getData1() + "," + sm.getData2()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                }
                else
                {
                    System.out.println("Other message: " + message.getClass()); //$NON-NLS-1$
                }
            }

            System.out.println();
        }
        return table;
    }

    /**
     * Generates a midi sequence with the tick at 1 millisecond to fit with the SFPL.
     * 
     * @return the sequence
     * @throws InvalidMidiDataException
     */
    public static Sequence createSequence() throws InvalidMidiDataException
    {
        Sequence ret = new Sequence(Sequence.PPQ, 500);
        return ret;
    }

    public static Track createTrack(Sequence sequence)
    {
        return sequence.createTrack();
    }

    public static Track getSequenceTrack(Sequence sequence, int trackNo)
    {
        return sequence.getTracks()[trackNo];
    }

    /**
     * Creates a SFPL style note
     * 
     * @param track
     *            the track to put it on
     * @param time
     *            the time in milliseconds since start of track to place the note
     * @param length
     *            the length of the note in milliseconds
     * @param channel
     *            the channel
     * @param note
     * @param velocity
     * @throws InvalidMidiDataException
     */
    public static void addNote(Track track, int time, int length, int channel, int note, int velocity)
                    throws InvalidMidiDataException
    {
        ShortMessage mess = new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity);
        MidiEvent event = new MidiEvent(mess, time);
        track.add(event);
        mess = new ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0);
        event = new MidiEvent(mess, time + length);
        track.add(event);
    }

    /** Saves a sequence as a type one file */
    public static void saveSequence(Sequence sequence, String fileName) throws IOException
    {
        try (FileOutputStream fs = new FileOutputStream(fileName);)
        {
            MidiSystem.write(sequence, 1, fs);
        }
    }

    /**
     * Returns a sequencer attached to the device with the passed device name.
     * 
     * @throws MidiUnavailableException
     * @throws InvalidMidiDataException
     */
    @SuppressWarnings("resource")
    public static Sequencer getPlayableSequencer(Sequence sequence, int deviceNo) throws MidiUnavailableException,
                    InvalidMidiDataException
    {
        Info deviceInfo = MidiSystem.getMidiDeviceInfo()[deviceNo];
        Sequencer sequencer = MidiSystem.getSequencer(false);
        sequencer.setSequence(sequence);
        MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
        Receiver receiver = device.getReceiver();
        sequencer.getTransmitter().setReceiver(receiver);
        return sequencer;
    }

    /** Closes all receivers attached to this sequencer */
    public static void closeSequencer(Sequencer sequencer)
    {
        for (Transmitter transmitter : sequencer.getTransmitters())
        {
            transmitter.getReceiver().close();
            transmitter.close();
        }
    }

    public static List<Object> getMidiDeviceNames() throws MidiUnavailableException
    {
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        List<Object> ret = new ArrayList<>();
        int i = 0;
        for (Info info : infos)
        {
            List<Object> l = new ArrayList<>();
            l.add(info.getName());
            l.add(i++);
            try (MidiDevice dev = MidiSystem.getMidiDevice(info);)
            {
                l.add(dev.getMaxReceivers());
                l.add(dev.getMaxTransmitters());
                ret.add(l);
            }
        }
        return ret;
    }

    public static void openSequencer(Sequencer sequencer) throws MidiUnavailableException
    {
        if (!sequencer.isOpen())
        {
            sequencer.open();
        }
    }

    public static void startSequencer(Sequencer sequencer)
    {
        sequencer.start();
    }

    public static void stopSequencer(Sequencer sequencer)
    {
        sequencer.stop();
    }
}
