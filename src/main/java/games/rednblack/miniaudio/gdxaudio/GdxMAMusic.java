package games.rednblack.miniaudio.gdxaudio;

import com.badlogic.gdx.audio.Music;
import games.rednblack.miniaudio.MASound;

public class GdxMAMusic implements Music, GdxEndListener {
    private final GdxMiniAudio gdxMiniAudio;
    private final MASound sound;

    private OnCompletionListener listener;

    public GdxMAMusic(MASound sound, GdxMiniAudio gdxMiniAudio) {
        this.sound = sound;
        this.gdxMiniAudio = gdxMiniAudio;
    }

    @Override
    public void play() {
        sound.play();
    }

    @Override
    public void pause() {
        sound.pause();
    }

    @Override
    public void stop() {
        sound.stop();
    }

    @Override
    public boolean isPlaying() {
        return sound.isPlaying();
    }

    @Override
    public void setLooping(boolean isLooping) {
        sound.setLooping(isLooping);
    }

    @Override
    public boolean isLooping() {
        return sound.isLooping();
    }

    @Override
    public void setVolume(float volume) {
        sound.setVolume(volume);
    }

    @Override
    public float getVolume() {
        return sound.getVolume();
    }

    @Override
    public void setPan(float pan, float volume) {
        sound.setPan(pan);
        sound.setVolume(volume);
    }

    @Override
    public void setPosition(float position) {
        sound.seekTo(position);
    }

    @Override
    public float getPosition() {
        return sound.getCursorPosition();
    }

    @Override
    public void dispose() {
        sound.dispose();
        listener = null;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        this.listener = listener;
        gdxMiniAudio.addCompletionListener(sound.getAddress(), this);
    }

    @Override
    public void onSoundEnd(long address) {
        if (listener != null) listener.onCompletion(this);
    }
}
