package games.rednblack.miniaudio.loader;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;

/**
 *  Parameters class for {@link MASoundLoader}
 *
 * @author fgnm
 */
public class MASoundLoaderParameters extends AssetLoaderParameters<MASound> {
    /** Custom flags to customize sound {@link MASound.Flags} */
    public short flags = 0;

    /** Attach the sound to a group */
    public MAGroup maGroup = null;

    /** Do not load file from the internal asset manager but looks for an absolute path */
    public boolean external = false;

    /** Do not read file from native code but fully decode data in memory, sound length and streaming are not available */
    public boolean loadFromMemory = false;
}
