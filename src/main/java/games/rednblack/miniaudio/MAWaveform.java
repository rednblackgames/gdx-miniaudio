package games.rednblack.miniaudio;

/**
 * Wrapper class to MiniAudio's waveform
 *
 * @author fgnm
 */
public class MAWaveform extends MADataSource {

    public MAWaveform(long address, MiniAudio miniAudio) {
        super(address, miniAudio);
        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating Waveform", (int) address);
        }
    }

    /**
     * Change waveform frequency dynamically.
     *
     * @param frequency frequency of the wave
     */
    public void setFrequency(double frequency) {
        miniAudio.setWaveformFrequency(address, frequency);
    }

    /**
     * Change waveform amplitude dynamically.
     *
     * @param amplitude amplitude of the wave
     */
    public void setAmplitude(double amplitude) {
        miniAudio.setWaveformAmplitude(address, amplitude);
    }

    /**
     * Change waveform type dynamically.
     *
     * @param type {@link MAWaveformType} of the wave
     */
    public void setType(MAWaveformType type) {
        miniAudio.setWaveformType(address, type);
    }

    @Override
    public void dispose() {
        miniAudio.disposeWaveform(address);
    }
}
