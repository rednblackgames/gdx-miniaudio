package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Disposable;

/**
 * Abstract Data Source class for audio playback.
 *
 * @author fgnm
 */
public abstract class MADataSource implements Disposable {
    protected MiniAudio miniAudio;
    protected long address;

    public MADataSource(long address, MiniAudio miniAudio) {
        this.address = address;
        this.miniAudio = miniAudio;
    }

    void setAddress(long address) {
        this.address = address;
    }
}
