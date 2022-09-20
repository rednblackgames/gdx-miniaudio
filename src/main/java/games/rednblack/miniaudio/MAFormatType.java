package games.rednblack.miniaudio;

/**
 * Wrapper enum to map native MiniAudio data format.
 *
 * @author fgnm
 */
public enum MAFormatType {
    /* Mainly used for indicating an error, but also used as the default for the output format for decoders. */
    UNKNOWN(0),
    U8(1),
    /* Seems to be the most widely supported format. */
    S16(2),
    /* Tightly packed. 3 bytes per sample. */
    S24(3),
    S32(4),
    F32(5);

    public final int code;

    MAFormatType(int code) {
        this.code = code;
    }
}
