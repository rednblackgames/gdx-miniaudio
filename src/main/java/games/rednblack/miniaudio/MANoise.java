package games.rednblack.miniaudio;

/**
 * Wrapper class to MiniAudio's noise
 *
 * @author fgnm
 */
public class MANoise extends MADataSource {

    public MANoise(long address, MiniAudio miniAudio) {
        super(address, miniAudio);
        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating Noise", (int) address);
        }
    }

    @Override
    public void dispose() {
        miniAudio.disposeNoise(address);
    }
}
