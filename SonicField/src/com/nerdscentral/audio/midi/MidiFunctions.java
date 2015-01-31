package com.nerdscentral.audio.midi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
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
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

public class MidiFunctions
{

    public static final int                 NOTE_ON  = 0x90;
    public static final int                 NOTE_OFF = 0x80;
    private static HashMap<Integer, String> smLookup = new HashMap<>();
    static
    {
        smLookup.put(ShortMessage.ACTIVE_SENSING, "active_sensing"); //$NON-NLS-1$
        smLookup.put(ShortMessage.CHANNEL_PRESSURE, "channel_pressure"); //$NON-NLS-1$
        smLookup.put(ShortMessage.CONTINUE, "continue"); //$NON-NLS-1$
        smLookup.put(ShortMessage.CONTROL_CHANGE, "control_change"); //$NON-NLS-1$       
        smLookup.put(ShortMessage.END_OF_EXCLUSIVE, "end_of_exclusive"); //$NON-NLS-1$
        smLookup.put(ShortMessage.MIDI_TIME_CODE, "midi_time_code"); //$NON-NLS-1$
        smLookup.put(ShortMessage.NOTE_OFF, "note_off"); //$NON-NLS-1$
        smLookup.put(ShortMessage.NOTE_ON, "note_on"); //$NON-NLS-1$
        smLookup.put(ShortMessage.PITCH_BEND, "pitch_bend"); //$NON-NLS-1$
        smLookup.put(ShortMessage.POLY_PRESSURE, "poly_pressure"); //$NON-NLS-1$
        smLookup.put(ShortMessage.PROGRAM_CHANGE, "program_change"); //$NON-NLS-1$
        smLookup.put(ShortMessage.SONG_POSITION_POINTER, "song_position_pointer"); //$NON-NLS-1$
        smLookup.put(ShortMessage.SONG_SELECT, "song_select"); //$NON-NLS-1$
        smLookup.put(ShortMessage.START, "start"); //$NON-NLS-1$
        smLookup.put(ShortMessage.STOP, "stop"); //$NON-NLS-1$
        smLookup.put(ShortMessage.SYSTEM_RESET, "system_reset"); //$NON-NLS-1$
        smLookup.put(ShortMessage.TIMING_CLOCK, "timing_clock"); //$NON-NLS-1$
        smLookup.put(ShortMessage.TUNE_REQUEST, "tune_request"); //$NON-NLS-1$
    }

    public static Sequence preProcessChannels(Sequence seqIn) throws InvalidMidiDataException
    {
        // If there are multiple channels per track we move the channels into separate
        // tracks so the output numer of tracks might be bigger than the input so first
        // work out how many tracks we need.
        int nTracks = 0;
        // Create the output sequence
        Sequence sqlOut = new Sequence(seqIn.getDivisionType(), seqIn.getResolution(), nTracks);
        return sqlOut;
    }

    public static Sequence readMidiFile(String fileName) throws InvalidMidiDataException, IOException
    {
        return MidiSystem.getSequence(new File(fileName));
    }

    public static void setChannel(ShortMessage sm, int channel) throws InvalidMidiDataException
    {
        sm.setMessage(sm.getCommand(), channel, sm.getData1(), sm.getData2());
    }

    public static void setChannel(MidiEvent ev, int channel) throws InvalidMidiDataException
    {
        ShortMessage sm = (ShortMessage) ev.getMessage();
        sm.setMessage(sm.getCommand(), channel, sm.getData1(), sm.getData2());
    }

    public static List<Object> processSequence(Sequence sequence)
    {
        List<Object> table = new ArrayList<>();
        int trackNumber = -1;
        for (Track track : sequence.getTracks())
        {
            ++trackNumber;
            List<Object> column = new ArrayList<>();
            table.add(column);
            HashMap<Integer, Stack<Map<String, Object>>> onMap = new HashMap<>();
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage)
                {
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == NOTE_ON)
                    {
                        int key = sm.getData1();
                        int velocity = sm.getData2();
                        if (velocity > 0)
                        {
                            Map<String, Object> row = new ConcurrentHashMap<>();
                            row.put("command", "note");  //$NON-NLS-1$//$NON-NLS-2$
                            row.put("tick", (double) event.getTick()); //$NON-NLS-1$
                            row.put("key", (double) key); //$NON-NLS-1$
                            row.put("velocity", (double) velocity); //$NON-NLS-1$
                            row.put("track", (double) trackNumber); //$NON-NLS-1$
                            row.put("channel", (double) sm.getChannel()); //$NON-NLS-1$
                            row.put("event", event); //$NON-NLS-1$
                            if (!onMap.containsKey(key))
                            {
                                onMap.put(key, new Stack<Map<String, Object>>());
                            }
                            Stack<Map<String, Object>> st = onMap.get(key);
                            if (st.size() > 0)
                            {
                                StringBuilder out = new StringBuilder();
                                out.append(String.format("Warning: Overlapping Notes Detected:%n")); //$NON-NLS-1$
                                for (Entry<String, Object> kv : row.entrySet())
                                {
                                    out.append(kv.getKey());
                                    out.append("="); //$NON-NLS-1$
                                    out.append(kv.getValue());
                                    out.append(" "); //$NON-NLS-1$
                                }
                                System.err.println(out);
                            }
                            st.push(row);
                        }
                        else
                        {
                            Map<String, Object> row = onMap.get(key).pop();
                            row.put("tick-off", (double) event.getTick()); //$NON-NLS-1$
                            row.put("event-off", event); //$NON-NLS-1$
                            column.add(row);
                        }
                    }
                    else if (sm.getCommand() == NOTE_OFF)
                    {
                        int key = sm.getData1();
                        Map<String, Object> row = onMap.get(key).pop();
                        row.put("tick-off", (double) event.getTick()); //$NON-NLS-1$
                        row.put("event-off", event); //$NON-NLS-1$
                        column.add(row);
                    }
                    else
                    {
                        Map<String, Object> row = new ConcurrentHashMap<>();
                        String command = smLookup.get(sm.getCommand());
                        if (command == null) command = "small-unknown"; //$NON-NLS-1$
                        row.put("type", command); //$NON-NLS-1$
                        row.put("track", (double) trackNumber); //$NON-NLS-1$
                        row.put("channel", (double) sm.getChannel()); //$NON-NLS-1$
                        row.put("data1", (double) sm.getData1()); //$NON-NLS-1$
                        row.put("data2", (double) sm.getData2()); //$NON-NLS-1$
                        row.put("tick", (double) event.getTick()); //$NON-NLS-1$
                        row.put("event", event); //$NON-NLS-1$
                        column.add(row);
                    }
                }
                else if (message instanceof SysexMessage)
                {
                    SysexMessage sxm = (SysexMessage) message;
                    Map<String, Object> row = new ConcurrentHashMap<>();
                    row.put("command", "sysex"); //$NON-NLS-1$ //$NON-NLS-2$
                    row.put("data", sxm.getData()); //$NON-NLS-1$
                    row.put("length", sxm.getLength()); //$NON-NLS-1$
                    row.put("status", sxm.getStatus()); //$NON-NLS-1$
                    row.put("track", (double) trackNumber); //$NON-NLS-1$
                    row.put("tick", (double) event.getTick()); //$NON-NLS-1$
                    row.put("event", event); //$NON-NLS-1$
                    column.add(row);
                }
                else if (message instanceof MetaMessage)
                {
                    MetaMessage mm = (MetaMessage) message;
                    Map<String, Object> row = new ConcurrentHashMap<>();
                    row.put("command", "meta"); //$NON-NLS-1$ //$NON-NLS-2$
                    row.put("type", mm.getType());//$NON-NLS-1$
                    row.put("data", mm.getData()); //$NON-NLS-1$
                    row.put("length", mm.getLength()); //$NON-NLS-1$
                    row.put("status", mm.getStatus()); //$NON-NLS-1$
                    row.put("tick", (double) event.getTick()); //$NON-NLS-1$
                    row.put("event", event); //$NON-NLS-1$
                    column.add(row);
                }
                else
                {
                    System.out.println("Other message: " + message.getClass()); //$NON-NLS-1$
                }
            }
        }
        return table;
    }

    /**
     * Makes a new sequence with just the headers copied over
     * 
     * @throws InvalidMidiDataException
     * 
     */
    public static Sequence blankSequence(Sequence in) throws InvalidMidiDataException
    {
        return new Sequence(in.getDivisionType(), in.getResolution());
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
    public static void writeMidiFile(String fileName, Sequence sequence) throws IOException
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

    public static List<Map<String, Object>> getMidiDeviceNames() throws MidiUnavailableException
    {
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        List<Map<String, Object>> ret = new ArrayList<>();
        int i = 0;
        for (Info info : infos)
        {
            Map<String, Object> l = new HashMap<>();
            l.put("name", info.getName()); //$NON-NLS-1$
            l.put("number", i++); //$NON-NLS-1$
            l.put("vendor", info.getVendor()); //$NON-NLS-1$
            l.put("version", info.getVersion()); //$NON-NLS-1$
            try (MidiDevice dev = MidiSystem.getMidiDevice(info);)
            {
                l.put("max-reveivers", dev.getMaxReceivers()); //$NON-NLS-1$
                l.put("max-transmitters", dev.getMaxTransmitters()); //$NON-NLS-1$
                ret.add(l);
            }
        }
        return ret;
    }

    public static MidiPlayer getPlayer()
    {
        return new MidiPlayer();
    }

}
