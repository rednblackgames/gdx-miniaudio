package games.rednblack.miniaudio;

/**
 * Wrapper class to MiniAudio's audio buffer
 *
 * @author fgnm
 */
public class MAAudioBuffer extends MADataSource {
    private final int bufferSize;
    protected long dataBufferAddress;

    public MAAudioBuffer(long audioBuffer, long dataBuffer, int size, MiniAudio miniAudio) {
        super(audioBuffer, miniAudio);
        if (MAResult.checkErrors(audioBuffer)) {
            throw new MiniAudioException("Error while creating Audio Buffer", (int) audioBuffer);
        }
        bufferSize = size;
        dataBufferAddress = dataBuffer;
    }

    /**
     * Write to data buffer.
     *
     * @param data array of raw PCM data.
     * @param size size of data to be written
     */
    public void write(float[] data, int size) {
        if (size > bufferSize)
            throw new IllegalArgumentException("Buffer size exceeded.");
        jniWrite(dataBufferAddress, data, size);
    }

    private native void jniWrite(long dataBufferAddress, float[] data, int size);/*
        float* buffer = (float*) dataBufferAddress;
        for (int i = 0; i < size; i++)
            buffer[i] = data[i];
    */

    @Override
    public void dispose() {
        miniAudio.disposeAudioBuffer(address, dataBufferAddress);
    }
}
