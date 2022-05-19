package games.rednblack.miniaudio;

/**
 * Indicates the type of the positioning model in sounds 3D Spatialization. By default, the position of a sound is in
 * absolute space, but it can be changed to be relative to a listener.
 *
 * @author fgnm
 */
public enum MAPositioning {
    /** Coordinates are in absolute space */
    ABSOLUTE(0),
    /** Coordinates are relative to the listener */
    RELATIVE(1);

    public final int code;

    MAPositioning(int code) {
        this.code = code;
    }
}
