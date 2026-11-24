package games.rednblack.miniaudio;

/**
 * Wrapper class to MiniAudio's audio buffer
 *
 * @author fgnm
 */
public class MAAudioBuffer extends MADataSource {
    /*JNI
        #include <string.h>
    */

    private final int bufferSize;
    private final int channels;
    protected long dataBufferAddress;

    /**
     * @param audioBuffer native address of the audio buffer
     * @param dataBuffer native address of the interleaved float data buffer
     * @param frameCount buffer length in PCM frames
     * @param channels how many channels
     * @param miniAudio engine instance
     */
    public MAAudioBuffer(long audioBuffer, long dataBuffer, int frameCount, int channels, MiniAudio miniAudio) {
        super(audioBuffer, miniAudio);
        if (MAResult.checkErrors(audioBuffer)) {
            throw new MiniAudioException("Error while creating Audio Buffer", (int) audioBuffer);
        }
        this.channels = channels;
        bufferSize = frameCount * channels;
        dataBufferAddress = dataBuffer;
    }

    /**
     * Write to data buffer.
     *
     * @param data array of raw PCM data.
     */
    public void write(float[] data) {
        write(data, data.length);
    }

    /**
     * Write to data buffer.
     *
     * @param data array of raw PCM data.
     * @param size size of data to be written
     */
    public void write(float[] data, int size) {
        if (size < 0 || size > data.length)
            throw new IllegalArgumentException("Invalid size: " + size + " (data length " + data.length + ").");
        if (size > bufferSize)
            throw new IllegalArgumentException("Buffer size exceeded.");
        jniWrite(dataBufferAddress, data, size);
    }

    private native void jniWrite(long dataBufferAddress, float[] data, int size);/*
        float* buffer = (float*) dataBufferAddress;
        for (int i = 0; i < size; i++)
            buffer[i] = data[i];
    */

    /**
     * Read from the start of the data buffer, as many float samples as fit in the destination array.
     * Samples are interleaved, see {@link #getChannels()}.
     *
     * @param dst destination array
     * @return how many float samples have been read
     */
    public int read(float[] dst) {
        int length = Math.min(dst.length, bufferSize);
        read(dst, 0, 0, length);
        return length;
    }

    /**
     * Read from data buffer. Offsets and length are in float samples (PCM frames multiplied by the channel count),
     * samples are interleaved, see {@link #getChannels()}.
     *
     * @param dst destination array
     * @param srcOffset first sample to read from the data buffer
     * @param dstOffset first index to write in the destination array
     * @param length how many float samples to read
     */
    public void read(float[] dst, int srcOffset, int dstOffset, int length) {
        if (srcOffset < 0 || dstOffset < 0 || length < 0)
            throw new IllegalArgumentException("Invalid read: srcOffset " + srcOffset + ", dstOffset " + dstOffset + ", length " + length + ".");
        if (srcOffset > bufferSize - length)
            throw new IllegalArgumentException("Buffer size exceeded.");
        if (dstOffset > dst.length - length)
            throw new IllegalArgumentException("Destination array too small: length " + dst.length + ", required " + ((long) dstOffset + length) + ".");
        if (length == 0) return;
        jniRead(dataBufferAddress, dst, srcOffset, dstOffset, length);
    }

    private native void jniRead(long dataBufferAddress, float[] dst, int srcOffset, int dstOffset, int length);/*
        const float* buffer = (const float*) dataBufferAddress;
        memcpy(dst + dstOffset, buffer + srcOffset, (size_t) length * sizeof(float));
    */

    @Override
    public void dispose() {
        miniAudio.disposeAudioBuffer(address, dataBufferAddress);
    }

    /**
     * Get the length of the buffer in float samples (PCM frames multiplied by the channel count).
     *
     * @return max length of the buffer
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Get the number of channels.
     *
     * @return channel count
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Get the length of the buffer in PCM frames.
     *
     * @return length in PCM frames
     */
    public int getFrameCount() {
        return bufferSize / channels;
    }
}
