package games.rednblack.miniaudio;

/**
 * Logging level wrapper class.
 *
 * @author fgnm
 */
public enum MALogLevel {
    NONE(0),
    ERROR(1),
    WARNING(2),
    INFO(3),
    DEBUG(4);

    public final int code;

    MALogLevel(int code) {
        this.code = code;
    }
}
