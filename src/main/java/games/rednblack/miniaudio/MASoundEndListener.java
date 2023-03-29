package games.rednblack.miniaudio;

/**
 * Listener to get notified when a {@link MASound} ends.
 *
 * NOTE: listener is called on MiniAudio's thread.
 *
 * @author fgnm
 */
public interface MASoundEndListener {

    void onSoundEnd(MASound maSound);
}
