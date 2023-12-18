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

    public static MALogLevel decode (int code) {
        switch (code) {
            case 1:
                return MALogLevel.ERROR;
            case 2:
                return MALogLevel.WARNING;
            case 3:
                return MALogLevel.INFO;
            case 4:
                return MALogLevel.DEBUG;
            default:
                return MALogLevel.NONE;
        }
    }
}
