package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Disposable;

/**
 * Wrapper class to MiniAudio's decoder. Read PCM frames from an audio file without playing it. Frames are read in
 * chunks, so the whole decoded file is never kept in memory. Output is always interleaved 32-bit float.
 * <p>
 * Create it with {@link MiniAudio#createDecoder(String)} or {@link MiniAudio#createDecoder(byte[])}.
 * <p>
 * A decoder does not depend on the playback engine, so it can be used from a background thread, but each instance
 * must be used by one thread at a time. Dispose it before disposing {@link MiniAudio}.
 *
 * @author fgnm
 */
public class MADecoder implements Disposable {
    /*JNI
        #include "miniaudio.h"
    */

    private long address;
    private final int channels;
    private final int sampleRate;
    private long lengthInFrames = -1;

    /**
     * @param address native address of the initialized decoder, allocated with a single ma_malloc block (when
     *                decoding from memory the block also holds the encoded data, freed together with the decoder)
     */
    public MADecoder(long address) {
        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating decoder", (int) address);
        }
        this.address = address;
        channels = jniGetChannels(address);
        sampleRate = jniGetSampleRate(address);
    }

    private native int jniGetChannels(long decoderAddress);/*
        ma_uint32 channels = 0;
        ma_decoder_get_data_format((ma_decoder*) decoderAddress, NULL, &channels, NULL, NULL, 0);
        return (jint) channels;
    */

    private native int jniGetSampleRate(long decoderAddress);/*
        ma_uint32 sampleRate = 0;
        ma_decoder_get_data_format((ma_decoder*) decoderAddress, NULL, NULL, &sampleRate, NULL, 0);
        return (jint) sampleRate;
    */

    /**
     * Read as many PCM frames as fit in the output array, starting from the current cursor.
     *
     * @param out interleaved output array
     * @return how many PCM frames have been read, 0 when the end of the file is reached
     */
    public int readFrames(float[] out) {
        return readFrames(out, out.length / channels);
    }

    /**
     * Read PCM frames starting from the current cursor.
     *
     * @param out interleaved output array, must hold at least frameCount * {@link #getChannels()} floats
     * @param frameCount how many PCM frames to read
     * @return how many PCM frames have been read, 0 when the end of the file is reached
     */
    public int readFrames(float[] out, int frameCount) {
        if (frameCount < 0)
            throw new IllegalArgumentException("Invalid frame count: " + frameCount + ".");
        if ((long) frameCount * channels > out.length)
            throw new IllegalArgumentException("Output array too small: length " + out.length + ", required " + ((long) frameCount * channels) + ".");
        if (frameCount == 0) return 0;

        int result = jniReadFrames(address, out, frameCount);
        if (result < 0) {
            throw new MiniAudioException("Error while reading PCM frames", result);
        }
        return result;
    }

    private native int jniReadFrames(long decoderAddress, float[] out, int frameCount);/*
        ma_uint64 framesRead = 0;
        ma_result result = ma_decoder_read_pcm_frames((ma_decoder*) decoderAddress, out, (ma_uint64) frameCount, &framesRead);
        if (framesRead > 0 || result == MA_SUCCESS || result == MA_AT_END) return (jint) framesRead;
        return (jint) result;
    */

    /**
     * Move the cursor to a PCM frame.
     *
     * @param frame PCM frame index, 0 is the start of the file
     */
    public void seekToFrame(long frame) {
        if (frame < 0)
            throw new IllegalArgumentException("Invalid frame: " + frame + ".");
        int result = jniSeekToFrame(address, frame);
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Error while seeking decoder", result);
        }
    }

    private native int jniSeekToFrame(long decoderAddress, long frame);/*
        return (jint) ma_decoder_seek_to_pcm_frame((ma_decoder*) decoderAddress, (ma_uint64) frame);
    */

    /**
     * Get the current cursor position.
     *
     * @return cursor position in PCM frames
     */
    public long getCursorInFrames() {
        long cursor = jniGetCursorInFrames(address);
        if (MAResult.checkErrors(cursor)) {
            throw new MiniAudioException("Error while getting decoder cursor", (int) cursor);
        }
        return cursor;
    }

    private native long jniGetCursorInFrames(long decoderAddress);/*
        ma_uint64 cursor = 0;
        ma_result result = ma_decoder_get_cursor_in_pcm_frames((ma_decoder*) decoderAddress, &cursor);
        if (result != MA_SUCCESS) return (jlong) result;
        return (jlong) cursor;
    */

    /**
     * Get the total length of the file. The value is computed on the first call and then cached: for some formats
     * (e.g. MP3) the first call has to scan the whole file. When the output sample rate differs from the file's
     * native one, the length is an estimate.
     *
     * @return length in PCM frames, 0 if it can't be determined
     */
    public long getLengthInFrames() {
        if (lengthInFrames < 0) {
            long length = jniGetLengthInFrames(address);
            if (MAResult.checkErrors(length)) {
                throw new MiniAudioException("Error while getting decoder length", (int) length);
            }
            lengthInFrames = length;
        }
        return lengthInFrames;
    }

    private native long jniGetLengthInFrames(long decoderAddress);/*
        ma_uint64 length = 0;
        ma_result result = ma_decoder_get_length_in_pcm_frames((ma_decoder*) decoderAddress, &length);
        if (result != MA_SUCCESS) return (jlong) result;
        return (jlong) length;
    */

    /**
     * Get the number of interleaved output channels.
     *
     * @return channel count
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Get the output sample rate.
     *
     * @return sample rate in Hz
     */
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void dispose() {
        if (address == 0) return;
        jniDispose(address);
        address = 0;
    }

    private native void jniDispose(long decoderAddress);/*
        ma_decoder* decoder = (ma_decoder*) decoderAddress;
        ma_decoder_uninit(decoder);
        // Frees the whole block, including the encoded data when decoding from memory
        ma_free(decoder, NULL);
    */
}
