package games.rednblack.miniaudio.config;

import com.badlogic.gdx.utils.Null;
import games.rednblack.miniaudio.MALogCallback;

/**
 * Configuration class for MiniAudio Context, have platform specific options
 *
 * @author fgnm
 */
public class MAContextConfiguration {
    /** Callback to forward native logs */
    public @Null MALogCallback logCallback = null;
    /** ONLY FOR iOS - The session category to use for the shared AudioSession instance. */
    public MAiOSSessionCategory iOSSessionCategory = MAiOSSessionCategory.AMBIENT;
    /** ONLY FOR iOS - Session category options to use with the shared AudioSession instance. See {@link MAiOSSessionCategoryOptions} */
    public short iOSSessionCategoryOptions = 0;
    /** ONLY FOR Android, Enable or disable AAudio backend, see https://github.com/rednblackgames/gdx-miniaudio/issues/1 */
    public boolean androidUseAAudio = false;
}
