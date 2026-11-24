package games.rednblack.miniaudio;

/**
 * Wrapper class to MiniAudio's audio buffer. Data is interleaved 32-bit float PCM.
 * <p>
 * Create it with {@link MiniAudio#createAudioBuffer(int, int)} or {@link MiniAudio#decodeBytes(byte[], int)}.
 *
 * @author fgnm
 */
public class MAAudioBuffer extends MADataSource {
    /*JNI
        #include <string.h>
        #include "miniaudio.h"
    */

    private final int bufferSize;
    private final int channels;
    private final int frameCount;

    /**
     * @param address native address of the audio buffer, allocated with a single ma_malloc block that also holds the
     *                PCM data, freed together with the audio buffer
     * @param miniAudio engine instance
     */
    public MAAudioBuffer(long address, MiniAudio miniAudio) {
        super(address, miniAudio);
        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating Audio Buffer", (int) address);
        }
        channels = jniGetChannels(address);
        frameCount = jniGetFrameCount(address);
        bufferSize = frameCount * channels;
    }

    private native int jniGetChannels(long audioBuffer);/*
        return (jint) ((ma_audio_buffer*) audioBuffer)->ref.channels;
    */

    private native int jniGetFrameCount(long audioBuffer);/*
        return (jint) ((ma_audio_buffer*) audioBuffer)->ref.sizeInFrames;
    */

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
        if (size == 0) return;
        jniWrite(address, data, size);
    }

    private native void jniWrite(long audioBuffer, float[] data, int size);/*
        float* buffer = (float*) ((ma_audio_buffer*) audioBuffer)->ref.pData;
        memcpy(buffer, data, (size_t) size * sizeof(float));
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
        jniRead(address, dst, srcOffset, dstOffset, length);
    }

    private native void jniRead(long audioBuffer, float[] dst, int srcOffset, int dstOffset, int length);/*
        const float* buffer = (const float*) ((ma_audio_buffer*) audioBuffer)->ref.pData;
        memcpy(dst + dstOffset, buffer + srcOffset, (size_t) length * sizeof(float));
    */

    /**
     * Dispose the audio buffer and free its data. Calling it more than once has no effect.
     */
    @Override
    public void dispose() {
        if (address == 0) return;
        jniDispose(address);
        address = 0;
    }

    private native void jniDispose(long audioBuffer);/*
        ma_audio_buffer* buffer = (ma_audio_buffer*) audioBuffer;
        ma_audio_buffer_uninit(buffer);
        // Frees the whole block, including the PCM data
        ma_free(buffer, NULL);
    */

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
        return frameCount;
    }
}
