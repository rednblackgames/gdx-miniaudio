package games.rednblack.miniaudio.gdxaudio;

import com.badlogic.gdx.audio.Sound;
import games.rednblack.miniaudio.MASound;

public class GdxMASound implements Sound {

    private final MASound sound;

    public GdxMASound(MASound sound) {
        this.sound = sound;
    }

    @Override
    public long play() {
        sound.play();
        return sound.getAddress();
    }

    @Override
    public long play(float volume) {
        sound.play();
        sound.setVolume(volume);
        return sound.getAddress();
    }

    @Override
    public long play(float volume, float pitch, float pan) {
        sound.play();
        sound.setVolume(volume);
        sound.setPitch(pitch);
        sound.setPan(pan);
        return sound.getAddress();
    }

    @Override
    public long loop() {
        sound.loop();
        return sound.getAddress();
    }

    @Override
    public long loop(float volume) {
        sound.loop();
        sound.setVolume(volume);
        return sound.getAddress();
    }

    @Override
    public long loop(float volume, float pitch, float pan) {
        sound.loop();
        sound.setVolume(volume);
        sound.setPitch(pitch);
        sound.setPan(pan);
        return sound.getAddress();
    }

    @Override
    public void stop() {
        sound.stop();
    }

    @Override
    public void pause() {
        sound.pause();
    }

    @Override
    public void resume() {
        sound.play();
    }

    @Override
    public void dispose() {
        sound.dispose();
    }

    @Override
    public void stop(long soundId) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }

    @Override
    public void pause(long soundId) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }

    @Override
    public void resume(long soundId) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }

    @Override
    public void setLooping(long soundId, boolean looping) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }

    @Override
    public void setPitch(long soundId, float pitch) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }

    @Override
    public void setVolume(long soundId, float volume) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }

    @Override
    public void setPan(long soundId, float pan, float volume) {
        throw new UnsupportedOperationException("soundId is not supported.");
    }
}
