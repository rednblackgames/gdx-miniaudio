package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Pool;

/**
 * libGDX pool implementation to handle concurrent instances of {@link MASound}
 *
 * @author fgnm
 */
public class MASoundPool extends Pool<MASound> {

    private final MiniAudio miniAudio;
    private final String fileName;
    private final short flags;
    private final MAGroup group;
    private final boolean external;

    public MASoundPool(MiniAudio miniAudio, String fileName) {
        this(miniAudio, fileName, (short) 0, null, false,16, Integer.MAX_VALUE);
    }

    public MASoundPool(MiniAudio miniAudio, String fileName, short flags) {
        this(miniAudio, fileName, flags, null, false,16, Integer.MAX_VALUE);
    }

    public MASoundPool(MiniAudio miniAudio, String fileName, short flags, MAGroup group) {
        this(miniAudio, fileName, flags, group, false,16, Integer.MAX_VALUE);
    }

    public MASoundPool(MiniAudio miniAudio, String fileName, short flags, MAGroup group, boolean external) {
        this(miniAudio, fileName, flags, group, external,16, Integer.MAX_VALUE);
    }

    public MASoundPool(MiniAudio miniAudio, String fileName, short flags, MAGroup group, boolean external, int initialCapacity, int max) {
        super(initialCapacity, max);
        this.miniAudio = miniAudio;
        this.fileName = fileName;
        this.flags = flags;
        this.group = group;
        this.external = external;
    }

    @Override
    protected MASound newObject() {
        return miniAudio.createSound(fileName, flags, group, external);
    }

    @Override
    protected void discard(MASound object) {
        object.dispose();
        super.discard(object);
    }
}
