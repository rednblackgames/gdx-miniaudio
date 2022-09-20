package games.rednblack.miniaudio;

/**
 * Utility class for collect hardware device info.
 *
 * @author fgnm
 */
public class MADeviceInfo {
    /** Name of the device */
    public String name;
    /** If this is a capture device or not*/
    public boolean isCapture;
    /** Device native id address */
    public long idAddress;
    /** If this is a default system device*/
    public boolean isDefault;
    /** List of supported data formats for this device*/
    public MADeviceNativeDataFormat[] nativeDataFormats;

    public static class MADeviceNativeDataFormat {
        public MAFormatType format; /* Sample format. If set to ma_format_unknown, all sample formats are supported. */
        public int channels;        /* If set to 0, all channels are supported. */
        public int sampleRate;      /* If set to 0, all sample rates are supported. */
        public int flags;           /* A combination of MA_DATA_FORMAT_FLAG_* flags. */
    }
}
