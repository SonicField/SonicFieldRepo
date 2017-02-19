/* For Copyright and License see LICENSE.txt and COPYING.txt in the root directory */
/**
 * 
 */
package com.nerdscentral.audio.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class SFWavSubsystem
{
    private enum IOState
    {
        READING, WRITING, CLOSED
    }

    private final static int BUFFER_SIZE   = 4096;

    private final static int FMT_CHUNK_ID  = 0x20746D66;
    private final static int DATA_CHUNK_ID = 0x61746164;
    private final static int RIFF_CHUNK_ID = 0x46464952;
    private final static int RIFF_TYPE_ID  = 0x45564157;

    private File             file;                      // File that will be
                                                         // read from or written
                                                         // to
    private IOState          ioState;                   // Specifies the IO
                                                         // State of the Wav
                                                         // File (used for
                                                         // snaity checking)
    private int              bytesPerSample;            // Number of bytes
                                                         // required to store a
                                                         // single sample
    private long             numFrames;                 // Number of frames
                                                         // within the data
                                                         // section
    private FileOutputStream oStream;                   // Output stream used
                                                         // for writting data
    private FileInputStream  iStream;                   // Input stream used
                                                         // for reading data
    private double           doubleScale;               // Scaling factor used
                                                         // for int <-> double
                                                         // conversion
    private double           doubleOffset;              // Offset factor used
                                                         // for int <-> double
                                                         // conversion
    private boolean          wordAlignAdjust;           // Specify if an extra
                                                         // byte at the end of
                                                         // the data chunk is
                                                         // required for word
                                                         // alignment

    // Wav Header
    private int              numChannels;               // 2 bytes unsigned,
                                                         // 0x0001 (1) to 0xFFFF
                                                         // (65,535)
    private long             sampleRate;                // 4 bytes unsigned,
                                                         // 0x00000001 (1) to
                                                         // 0xFFFFFFFF
                                                         // (4,294,967,295)
                                                         // Although a java int
                                                         // is 4 bytes, it is
                                                         // signed, so need to
                                                         // use a long
    private int              blockAlign;                // 2 bytes unsigned,
                                                         // 0x0001 (1) to 0xFFFF
                                                         // (65,535)
    private int              validBits;                 // 2 bytes unsigned,
                                                         // 0x0002 (2) to 0xFFFF
                                                         // (65,535)

    // Buffering
    private final byte[]     buffer;                    // Local buffer used
    // for IO
    private int              bufferPointer;             // Points to the
                                                         // current position in
                                                         // local buffer
    private int              bytesRead;                 // Bytes read after
                                                         // last read into local
                                                         // buffer
    private long             frameCounter;              // Current number of
                                                         // frames read or
                                                         // written

    // Cannot instantiate WavFile directly, must either use newWavFile() or
    // openWavFile()
    private SFWavSubsystem()
    {
        this.buffer = new byte[BUFFER_SIZE];
    }

    public int getNumChannels()
    {
        return this.numChannels;
    }

    public long getNumFrames()
    {
        return this.numFrames;
    }

    public long getFramesRemaining()
    {
        return this.numFrames - this.frameCounter;
    }

    public long getSampleRate()
    {
        return this.sampleRate;
    }

    public int getValidBits()
    {
        return this.validBits;
    }

    public static SFWavSubsystem newWavFile(File file, int numChannels, long numFrames, int validBits, long sampleRate)
                    throws IOException, SFWavFileException
    {
        // Instantiate new Wavfile and initialise
        SFWavSubsystem wavFile = new SFWavSubsystem();
        wavFile.file = file;
        wavFile.numChannels = numChannels;
        wavFile.numFrames = numFrames;
        wavFile.sampleRate = sampleRate;
        wavFile.bytesPerSample = (validBits + 7) / 8;
        wavFile.blockAlign = wavFile.bytesPerSample * numChannels;
        wavFile.validBits = validBits;

        // Sanity check arguments
        if (numChannels < 1 || numChannels > 65535) throw new SFWavFileException(
                        "Illegal number of channels, valid range 1 to 65536"); //$NON-NLS-1$
        if (numFrames < 0) throw new SFWavFileException("Number of frames must be positive"); //$NON-NLS-1$
        if (validBits < 2 || validBits > 65535) throw new SFWavFileException(
                        "Illegal number of valid bits, valid range 2 to 65536"); //$NON-NLS-1$
        if (sampleRate < 0) throw new SFWavFileException("Sample rate must be positive"); //$NON-NLS-1$

        // Create output stream for writing data
        wavFile.oStream = new FileOutputStream(file);

        // Calculate the chunk sizes
        long dataChunkSize = wavFile.blockAlign * numFrames;
        long mainChunkSize = 4 + // Riff Type
                        8 + // Format ID and size
                        16 + // Format data
                        8 + // Data ID and size
                        dataChunkSize;

        // Chunks must be word aligned, so if odd number of audio data bytes
        // adjust the main chunk size
        if (dataChunkSize % 2 == 1)
        {
            mainChunkSize += 1;
            wavFile.wordAlignAdjust = true;
        }
        else
        {
            wavFile.wordAlignAdjust = false;
        }

        // Set the main chunk size
        putLE(RIFF_CHUNK_ID, wavFile.buffer, 0, 4);
        putLE(mainChunkSize, wavFile.buffer, 4, 4);
        putLE(RIFF_TYPE_ID, wavFile.buffer, 8, 4);

        // Write out the header
        wavFile.oStream.write(wavFile.buffer, 0, 12);

        // Put format data in buffer
        long averageBytesPerSecond = sampleRate * wavFile.blockAlign;

        putLE(FMT_CHUNK_ID, wavFile.buffer, 0, 4); // Chunk ID
        putLE(16, wavFile.buffer, 4, 4); // Chunk Data Size
        putLE(1, wavFile.buffer, 8, 2); // Compression Code (Uncompressed)
        putLE(numChannels, wavFile.buffer, 10, 2); // Number of channels
        putLE(sampleRate, wavFile.buffer, 12, 4); // Sample Rate
        putLE(averageBytesPerSecond, wavFile.buffer, 16, 4); // Average Bytes
                                                             // Per Second
        putLE(wavFile.blockAlign, wavFile.buffer, 20, 2); // Block Align
        putLE(validBits, wavFile.buffer, 22, 2); // Valid Bits

        // Write Format Chunk
        wavFile.oStream.write(wavFile.buffer, 0, 24);

        // Start Data Chunk
        putLE(DATA_CHUNK_ID, wavFile.buffer, 0, 4); // Chunk ID
        putLE(dataChunkSize, wavFile.buffer, 4, 4); // Chunk Data Size

        // Write Format Chunk
        wavFile.oStream.write(wavFile.buffer, 0, 8);

        // Calculate the scaling factor for converting to a normalised double
        if (wavFile.validBits > 8)
        {
            // If more than 8 validBits, data is signed
            // Conversion required multiplying by magnitude of max positive
            // value
            wavFile.doubleOffset = 0;
            wavFile.doubleScale = Long.MAX_VALUE >> (64 - wavFile.validBits);
        }
        else
        {
            // Else if 8 or less validBits, data is unsigned
            // Conversion required dividing by max positive value
            wavFile.doubleOffset = 1;
            wavFile.doubleScale = 0.5 * ((1 << wavFile.validBits) - 1);
        }

        // Finally, set the IO State
        wavFile.bufferPointer = 0;
        wavFile.bytesRead = 0;
        wavFile.frameCounter = 0;
        wavFile.ioState = IOState.WRITING;

        return wavFile;
    }

    public static SFWavSubsystem openWavFile(File file) throws IOException, SFWavFileException
    {
        // Instantiate new Wavfile and store the file reference
        SFWavSubsystem wavFile = new SFWavSubsystem();
        wavFile.file = file;

        // Create a new file input stream for reading file data
        wavFile.iStream = new FileInputStream(file);

        // Read the first 12 bytes of the file
        int bytesRead = wavFile.iStream.read(wavFile.buffer, 0, 12);
        if (bytesRead != 12) throw new SFWavFileException("Not enough wav file bytes for header"); //$NON-NLS-1$

        // Extract parts from the header
        long riffChunkID = getLE(wavFile.buffer, 0, 4);
        long chunkSize = getLE(wavFile.buffer, 4, 4);
        long riffTypeID = getLE(wavFile.buffer, 8, 4);

        // Check the header bytes contains the correct signature
        if (riffChunkID != RIFF_CHUNK_ID) throw new SFWavFileException("Invalid Wav Header data, incorrect riff chunk ID"); //$NON-NLS-1$
        if (riffTypeID != RIFF_TYPE_ID) throw new SFWavFileException("Invalid Wav Header data, incorrect riff type ID"); //$NON-NLS-1$

        // Check that the file size matches the number of bytes listed in header
        if (file.length() != chunkSize + 8)
        {
            throw new SFWavFileException("Header chunk size (" + chunkSize //$NON-NLS-1$
                            + ") does not match file size (" + file.length() + ")");} //$NON-NLS-1$ //$NON-NLS-2$

        boolean foundFormat = false;
        boolean foundData = false;

        // Search for the Format and Data Chunks
        while (true)
        {
            // Read the first 8 bytes of the chunk (ID and chunk size)
            bytesRead = wavFile.iStream.read(wavFile.buffer, 0, 8);
            if (bytesRead == -1) throw new SFWavFileException("Reached end of file without finding format chunk"); //$NON-NLS-1$
            if (bytesRead != 8) throw new SFWavFileException("Could not read chunk header"); //$NON-NLS-1$

            // Extract the chunk ID and Size
            long chunkID = getLE(wavFile.buffer, 0, 4);
            chunkSize = getLE(wavFile.buffer, 4, 4);

            // Word align the chunk size
            // chunkSize specifies the number of bytes holding data. However,
            // the data should be word aligned (2 bytes) so we need to calculate
            // the actual number of bytes in the chunk
            long numChunkBytes = (chunkSize % 2 == 1) ? chunkSize + 1 : chunkSize;

            if (chunkID == FMT_CHUNK_ID)
            {
                // Flag that the format chunk has been found
                foundFormat = true;

                // Read in the header info
                bytesRead = wavFile.iStream.read(wavFile.buffer, 0, 16);

                // Check this is uncompressed data
                int compressionCode = (int) getLE(wavFile.buffer, 0, 2);
                if (compressionCode != 1) throw new SFWavFileException("Compression Code " + compressionCode + " not supported"); //$NON-NLS-1$ //$NON-NLS-2$

                // Extract the format information
                wavFile.numChannels = (int) getLE(wavFile.buffer, 2, 2);
                wavFile.sampleRate = getLE(wavFile.buffer, 4, 4);
                wavFile.blockAlign = (int) getLE(wavFile.buffer, 12, 2);
                wavFile.validBits = (int) getLE(wavFile.buffer, 14, 2);

                if (wavFile.numChannels == 0) throw new SFWavFileException(
                                "Number of channels specified in header is equal to zero"); //$NON-NLS-1$
                if (wavFile.blockAlign == 0) throw new SFWavFileException("Block Align specified in header is equal to zero"); //$NON-NLS-1$
                if (wavFile.validBits < 2) throw new SFWavFileException("Valid Bits specified in header is less than 2"); //$NON-NLS-1$
                if (wavFile.validBits > 64) throw new SFWavFileException(
                                "Valid Bits specified in header is greater than 64, this is greater than a long can hold"); //$NON-NLS-1$

                // Calculate the number of bytes required to hold 1 sample
                wavFile.bytesPerSample = (wavFile.validBits + 7) / 8;
                if (wavFile.bytesPerSample * wavFile.numChannels != wavFile.blockAlign) throw new SFWavFileException(
                                "Block Align does not agree with bytes required for validBits and number of channels"); //$NON-NLS-1$

                // Account for number of format bytes and then skip over
                // any extra format bytes
                numChunkBytes -= 16;
                if (numChunkBytes > 0) wavFile.iStream.skip(numChunkBytes);
            }
            else if (chunkID == DATA_CHUNK_ID)
            {
                // Check if we've found the format chunk,
                // If not, throw an exception as we need the format information
                // before we can read the data chunk
                if (foundFormat == false) throw new SFWavFileException("Data chunk found before Format chunk"); //$NON-NLS-1$

                // Check that the chunkSize (wav data length) is a multiple of
                // the
                // block align (bytes per frame)
                if (chunkSize % wavFile.blockAlign != 0) throw new SFWavFileException(
                                "Data Chunk size is not multiple of Block Align"); //$NON-NLS-1$

                // Calculate the number of frames
                wavFile.numFrames = chunkSize / wavFile.blockAlign;

                // Flag that we've found the wave data chunk
                foundData = true;

                break;
            }
            else
            {
                // If an unknown chunk ID is found, just skip over the chunk
                // data
                wavFile.iStream.skip(numChunkBytes);
            }
        }

        // Throw an exception if no data chunk has been found
        if (foundData == false) throw new SFWavFileException("Did not find a data chunk"); //$NON-NLS-1$

        // Calculate the scaling factor for converting to a normalised double
        if (wavFile.validBits > 8)
        {
            // If more than 8 validBits, data is signed
            // Conversion required dividing by magnitude of max negative value
            wavFile.doubleOffset = 0;
            wavFile.doubleScale = 1 << (wavFile.validBits - 1);
        }
        else
        {
            // Else if 8 or less validBits, data is unsigned
            // Conversion required dividing by max positive value
            wavFile.doubleOffset = -1;
            wavFile.doubleScale = 0.5 * ((1 << wavFile.validBits) - 1);
        }

        wavFile.bufferPointer = 0;
        wavFile.bytesRead = 0;
        wavFile.frameCounter = 0;
        wavFile.ioState = IOState.READING;

        return wavFile;
    }

    // Get and Put little endian data from local buffer
    // ------------------------------------------------
    private static long getLE(byte[] buffer, final int posIn, final int numBytesIn)
    {
        int numBytes = numBytesIn - 1;
        int pos = posIn + numBytes;

        long val = buffer[pos] & 0xFF;
        for (int b = 0; b < numBytes; b++)
            val = (val << 8) + (buffer[--pos] & 0xFF);

        return val;
    }

    private static void putLE(final long valIn, byte[] buffer, final int posIn, final int numBytes)
    {
        long val = valIn;
        int pos = posIn;
        for (int b = 0; b < numBytes; b++)
        {
            buffer[pos] = (byte) (val & 0xFF);
            val >>= 8;
            pos++;
        }
    }

    // Sample Writing and Reading
    // --------------------------
    private void writeSample(final long valIn) throws IOException
    {
        long val = valIn;
        for (int b = 0; b < this.bytesPerSample; b++)
        {
            if (this.bufferPointer == BUFFER_SIZE)
            {
                this.oStream.write(this.buffer, 0, BUFFER_SIZE);
                this.bufferPointer = 0;
            }

            this.buffer[this.bufferPointer] = (byte) (val & 0xFF);
            val >>= 8;
            this.bufferPointer++;
        }
    }

    private long readSample() throws IOException, SFWavFileException
    {
        long val = 0;

        for (int b = 0; b < this.bytesPerSample; b++)
        {
            if (this.bufferPointer == this.bytesRead)
            {
                int read = this.iStream.read(this.buffer, 0, BUFFER_SIZE);
                if (read == -1) throw new SFWavFileException("Not enough data available"); //$NON-NLS-1$
                this.bytesRead = read;
                this.bufferPointer = 0;
            }

            int v = this.buffer[this.bufferPointer];
            if (b < this.bytesPerSample - 1 || this.bytesPerSample == 1) v &= 0xFF;
            val += v << (b * 8);

            this.bufferPointer++;
        }

        return val;
    }

    // Integer
    // -------
    public int readFrames(int[] sampleBuffer, int numFramesToRead) throws IOException, SFWavFileException
    {
        return readFrames(sampleBuffer, 0, numFramesToRead);
    }

    public int readFrames(final int[] sampleBuffer, final int offsetIn, final int numFramesToRead) throws IOException,
                    SFWavFileException
    {
        if (this.ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance"); //$NON-NLS-1$
        int offSet = offsetIn;
        for (int f = 0; f < numFramesToRead; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
            {
                sampleBuffer[offSet] = (int) readSample();
                offSet++;
            }

            this.frameCounter++;
        }

        return numFramesToRead;
    }

    public int readFrames(int[][] sampleBuffer, int numFramesToRead) throws IOException, SFWavFileException
    {
        return readFrames(sampleBuffer, 0, numFramesToRead);
    }

    public int readFrames(int[][] sampleBuffer, final int offSetIn, final int numFramesToRead) throws IOException,
                    SFWavFileException
    {
        if (this.ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToRead; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
                sampleBuffer[c][offSet] = (int) readSample();

            offSet++;
            this.frameCounter++;
        }

        return numFramesToRead;
    }

    public int writeFrames(int[] sampleBuffer, int numFramesToWrite) throws IOException
    {
        return writeFrames(sampleBuffer, 0, numFramesToWrite);
    }

    public int writeFrames(int[] sampleBuffer, final int offSetIn, int numFramesToWrite) throws IOException
    {
        if (this.ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToWrite; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
            {
                writeSample(sampleBuffer[offSet]);
                offSet++;
            }

            this.frameCounter++;
        }

        return numFramesToWrite;
    }

    public int writeFrames(int[][] sampleBuffer, int numFramesToWrite) throws IOException
    {
        return writeFrames(sampleBuffer, 0, numFramesToWrite);
    }

    public int writeFrames(int[][] sampleBuffer, final int offSetIn, final int numFramesToWrite) throws IOException
    {
        if (this.ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToWrite; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
                writeSample(sampleBuffer[c][offSet]);

            offSet++;
            this.frameCounter++;
        }

        return numFramesToWrite;
    }

    // Long
    // ----
    public int readFrames(long[] sampleBuffer, int numFramesToRead) throws IOException, SFWavFileException
    {
        return readFrames(sampleBuffer, 0, numFramesToRead);
    }

    public int readFrames(long[] sampleBuffer, final int offSetIn, final int numFramesToRead) throws IOException,
                    SFWavFileException
    {
        if (this.ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToRead; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
            {
                sampleBuffer[offSet] = readSample();
                offSet++;
            }

            this.frameCounter++;
        }

        return numFramesToRead;
    }

    public int readFrames(long[][] sampleBuffer, int numFramesToRead) throws IOException, SFWavFileException
    {
        return readFrames(sampleBuffer, 0, numFramesToRead);
    }

    public int readFrames(long[][] sampleBuffer, final int offSetIn, final int numFramesToRead) throws IOException,
                    SFWavFileException
    {
        if (this.ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToRead; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
                sampleBuffer[c][offSet] = readSample();

            offSet++;
            this.frameCounter++;
        }

        return numFramesToRead;
    }

    public int writeFrames(long[] sampleBuffer, int numFramesToWrite) throws IOException
    {
        return writeFrames(sampleBuffer, 0, numFramesToWrite);
    }

    public int writeFrames(long[] sampleBuffer, final int offSetIn, final int numFramesToWrite) throws IOException
    {
        if (this.ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToWrite; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
            {
                writeSample(sampleBuffer[offSet]);
                offSet++;
            }

            this.frameCounter++;
        }

        return numFramesToWrite;
    }

    public int writeFrames(long[][] sampleBuffer, int numFramesToWrite) throws IOException
    {
        return writeFrames(sampleBuffer, 0, numFramesToWrite);
    }

    public int writeFrames(long[][] sampleBuffer, final int offSetIn, final int numFramesToWrite) throws IOException
    {
        if (this.ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToWrite; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
                writeSample(sampleBuffer[c][offSet]);

            offSet++;
            this.frameCounter++;
        }

        return numFramesToWrite;
    }

    // Double
    // ------
    public int readFrames(double[] sampleBuffer, int numFramesToRead) throws IOException, SFWavFileException
    {
        return readFrames(sampleBuffer, 0, numFramesToRead);
    }

    public int readFrames(double[] sampleBuffer, final int offSetIn, final int numFramesToRead) throws IOException,
                    SFWavFileException
    {
        if (this.ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToRead; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
            {
                sampleBuffer[offSet] = this.doubleOffset + readSample() / this.doubleScale;
                offSet++;
            }

            this.frameCounter++;
        }

        return numFramesToRead;
    }

    public int readFrames(double[][] sampleBuffer, int numFramesToRead) throws IOException, SFWavFileException
    {
        return readFrames(sampleBuffer, 0, numFramesToRead);
    }

    public int readFrames(double[][] sampleBuffer, final int offSetIn, int numFramesToRead) throws IOException,
                    SFWavFileException
    {
        if (this.ioState != IOState.READING) throw new IOException("Cannot read from WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToRead; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
                sampleBuffer[c][offSet] = this.doubleOffset + readSample() / this.doubleScale;

            offSet++;
            this.frameCounter++;
        }

        return numFramesToRead;
    }

    public int writeFrames(double[] sampleBuffer, int numFramesToWrite) throws IOException
    {
        return writeFrames(sampleBuffer, 0, numFramesToWrite);
    }

    public int writeFrames(double[] sampleBuffer, final int offSetIn, final int numFramesToWrite) throws IOException
    {
        if (this.ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;
        for (int f = 0; f < numFramesToWrite; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
            {
                writeSample((long) (this.doubleScale * (this.doubleOffset + sampleBuffer[offSet])));
                offSet++;
            }

            this.frameCounter++;
        }

        return numFramesToWrite;
    }

    public int writeFrames(double[][] sampleBuffer, int numFramesToWrite) throws IOException
    {
        return writeFrames(sampleBuffer, 0, numFramesToWrite);
    }

    public int writeFrames(double[][] sampleBuffer, final int offSetIn, final int numFramesToWrite) throws IOException
    {
        if (this.ioState != IOState.WRITING) throw new IOException("Cannot write to WavFile instance"); //$NON-NLS-1$
        int offSet = offSetIn;

        for (int f = 0; f < numFramesToWrite; f++)
        {
            if (this.frameCounter == this.numFrames) return f;

            for (int c = 0; c < this.numChannels; c++)
                writeSample((long) (this.doubleScale * (this.doubleOffset + sampleBuffer[c][offSet])));
            offSet++;
            this.frameCounter++;
        }

        return numFramesToWrite;
    }

    public void close() throws IOException
    {
        // Close the input stream and set to null
        if (this.iStream != null)
        {
            this.iStream.close();
            this.iStream = null;
        }

        if (this.oStream != null)
        {
            // Write out anything still in the local buffer
            if (this.bufferPointer > 0) this.oStream.write(this.buffer, 0, this.bufferPointer);

            // If an extra byte is required for word alignment, add it to the
            // end
            if (this.wordAlignAdjust) this.oStream.write(0);

            // Close the stream and set to null
            this.oStream.close();
            this.oStream = null;
        }

        // Flag that the stream is closed
        this.ioState = IOState.CLOSED;
    }

    public void display()
    {
        display(System.out);
    }

    public void display(PrintStream out)
    {
        out.printf("File: %s\n", this.file); //$NON-NLS-1$
        out.printf("Channels: %d, Frames: %d\n", this.numChannels, this.numFrames); //$NON-NLS-1$
        out.printf("IO State: %s\n", this.ioState); //$NON-NLS-1$
        out.printf("Sample Rate: %d, Block Align: %d\n", this.sampleRate, this.blockAlign); //$NON-NLS-1$
        out.printf("Valid Bits: %d, Bytes per sample: %d\n", this.validBits, this.bytesPerSample); //$NON-NLS-1$
    }

}
