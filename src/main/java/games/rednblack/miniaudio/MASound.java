package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Disposable;

public class MASound implements Disposable {
    protected long address;
    protected MiniAudio miniAudio;

    public MASound(long address, MiniAudio miniAudio) {
        this.address = address;
        this.miniAudio = miniAudio;
    }

    public void play() {
        miniAudio.playSound(address);
    }

    public void loop() {
        setLooping(true);
        play();
    }

    public void pause() {
        miniAudio.pauseSound(address);
    }

    public void stop() {
        miniAudio.stopSound(address);
    }

    public boolean isPlaying() {
        return miniAudio.isSoundPlaying(address);
    }

    public boolean isEnd() {
        return miniAudio.isSoundEnd(address);
    }

    public boolean isLooping() {
        return miniAudio.isSoundLooping(address);
    }

    public void setLooping(boolean looping) {
        miniAudio.setSoundLooping(address, looping);
    }

    public void setVolume(float volume) {
        miniAudio.setSoundVolume(address, volume);
    }

    public void setPitch(float pitch) {
        miniAudio.setSoundPitch(address, pitch);
    }

    public void setPan(float pan) {
        miniAudio.setSoundPan(address, pan);
    }

    public void setSpatialization(boolean spatialization) {
        miniAudio.setSoundSpatialization(address, spatialization);
    }

    public void setPosition(float x, float y, float z) {
        miniAudio.setSoundPosition(address, x, y, z);
    }

    public void setDirection(float forwardX, float forwardY, float forwardZ) {
        miniAudio.setSoundDirection(address, forwardX, forwardY, forwardZ);
    }

    public void seekTo(float seconds) {
        miniAudio.seekSoundTo(address, seconds);
    }

    public void fadeIn(float milliseconds) {
        fadeIn(milliseconds, 1);
    }

    public void fadeIn(float milliseconds, float targetVolume) {
        miniAudio.soundFade(address, 0, targetVolume, milliseconds);
    }

    public void fadeOut(float milliseconds, float targetVolume) {
        miniAudio.soundFade(address, -1, targetVolume, milliseconds);
    }

    public void fadeOut(float milliseconds) {
        fadeOut(milliseconds, 0);
    }

    @Override
    public void dispose() {
        miniAudio.disposeSound(address);
    }

    public static class Flags {
        public static final short MA_SOUND_FLAG_STREAM                = 0x00000001,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_STREAM */
        MA_SOUND_FLAG_DECODE                = 0x00000002,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_DECODE */
        MA_SOUND_FLAG_ASYNC                 = 0x00000004,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_ASYNC */
        MA_SOUND_FLAG_WAIT_INIT             = 0x00000008,   /* MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_WAIT_INIT */
        MA_SOUND_FLAG_NO_DEFAULT_ATTACHMENT = 0x00000010,   /* Do not attach to the endpoint by default. Useful for when setting up nodes in a complex graph system. */
        MA_SOUND_FLAG_NO_PITCH              = 0x00000020,   /* Disable pitch shifting with ma_sound_set_pitch() and ma_sound_group_set_pitch(). This is an optimization. */
        MA_SOUND_FLAG_NO_SPATIALIZATION     = 0x00000040;    /* Disable spatialization. */
    }
}
