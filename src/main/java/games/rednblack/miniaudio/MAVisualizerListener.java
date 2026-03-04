package games.rednblack.miniaudio;

/**
 * Listener to receive real-time PCM audio data from a {@link games.rednblack.miniaudio.mix.MAVisualizerNode}.
 *
 * NOTE: listener is called on MiniAudio's callback thread.
 *
 * @author fgnm
 */
public interface MAVisualizerListener {

    /**
     * Called when new PCM data is available from the visualizer node.
     * The pcmData array is reused across calls — copy it if you need to retain it.
     *
     * @param pcmData interleaved float PCM samples (buffer may be larger than totalSamples)
     * @param totalSamples number of valid samples in pcmData (frameCount * channels)
     * @param channels number of audio channels
     */
    void onVisualizerData(float[] pcmData, int totalSamples, int channels);
}
