package games.rednblack.miniaudio.config;

/**
 * An audio session category defines a set of audio behaviors.
 * Reference to <a href="https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct">AVAudioSession.Category</a>
 *
 * @author fgnm
 */
public enum MAiOSSessionCategory {
    /** Silenced by the Ring/Silent switch and by screen locking. Output only */
    AMBIENT (2),
    /** Silenced by the Ring/Silent switch and by screen locking. Interrupts nonmixable app’s audio. Output only */
    SOLO_AMBIENT (3),
    /** Interrupts nonmixable app’s audio. Output only */
    PLAYBACK (4),
    /** Interrupts nonmixable app’s audio. Input only */
    RECORD (5),
    /** Interrupts nonmixable app’s audio. Input and Output */
    PLAY_AND_RECORD (6),
    /** Interrupts nonmixable app’s audio. Input and output */
    MULTI_ROUTE (7);

    public final int code;

    MAiOSSessionCategory(int code) {
        this.code = code;
    }
}
