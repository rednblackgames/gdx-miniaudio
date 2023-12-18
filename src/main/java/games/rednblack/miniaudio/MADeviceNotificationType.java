package games.rednblack.miniaudio;

public enum MADeviceNotificationType {
    STARTED(0),
    STOPPED(1),
    REROUTED(2),
    INTERRUPTION_BEGAN(3),
    INTERRUPTION_ENDED(4),
    UNLOCKED(5);

    public final int code;

    MADeviceNotificationType(int code) {
        this.code = code;
    }

    public static MADeviceNotificationType decode (int code) {
        switch (code) {
            case 0:
                return MADeviceNotificationType.STARTED;
            case 1:
                return MADeviceNotificationType.STOPPED;
            case 2:
                return MADeviceNotificationType.REROUTED;
            case 3:
                return MADeviceNotificationType.INTERRUPTION_BEGAN;
            case 4:
                return MADeviceNotificationType.INTERRUPTION_ENDED;
            case 5:
                return MADeviceNotificationType.UNLOCKED;
            default:
                return null;
        }
    }
}
