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
    public short flags = 0;
    public MAGroup maGroup = null;
    public boolean external = false;
}
