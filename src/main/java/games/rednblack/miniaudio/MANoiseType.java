package games.rednblack.miniaudio;

/**
 * Indicate the type of the {@link MANoise}.
 *
 * @author fgnm
 */
public enum MANoiseType {
    WHITE(0),
    PINK(1),
    BROWNIAN(2);

    public final int code;

    MANoiseType(int code) {
        this.code = code;
    }
}
