package games.rednblack.miniaudio.config;

/**
 * Options for iOS Audio Session.
 * Reference to <a href="https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions-swift.struct">...</a>
 *
 * @author fgnm
 */
public class MAiOSSessionCategoryOptions {
    public short MIX_WITH_OTHERS = 0x01;
    public short DUCK_OTHERS = 0x02;
    public short ALLOW_BLUETOOTH = 0x04;
    public short DEFAULT_TO_SPEAKER = 0x08;
    public short INTERRUPT_SPOKEN_AUDIO_AND_MIX_WITH_OTHERS = 0x11;
    public short ALLOW_BLUETOOTH_A2DP = 0x20;
    public short ALLOW_AIR_PLAY = 0x40;
}
