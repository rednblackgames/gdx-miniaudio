package games.rednblack.miniaudio;

/**
 * Indicate the type of the {@link MAWaveform}.
 *
 * @author fgnm
 */
public enum MAWaveformType {
    SINE(0),
    SQUARE(1),
    TRIANGLE(2),
    SAWTOOTH(3);

    public final int code;

    MAWaveformType(int code) {
        this.code = code;
    }
}
