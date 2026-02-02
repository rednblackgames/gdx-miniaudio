package games.rednblack.miniaudio.gdxaudio;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.LongMap;
import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MASoundPool;

public class GdxMASound implements Sound, GdxEndListener {
    private final LongMap<MASound> activeSoundsMap = new LongMap<>();
    private final MASoundPool soundPool;
    private final MAGroup group;

    public GdxMASound(MASoundPool soundPool, MAGroup group) {
        this.soundPool = soundPool;
        this.group = group;
    }

    @Override
    public long play() {
        MASound sound = soundPool.obtain();
        sound.play();
        activeSoundsMap.put(sound.getAddress(), sound);
        return sound.getAddress();
    }

    @Override
    public long play(float volume) {
        MASound sound = soundPool.obtain();
        sound.play();
        sound.setVolume(volume);
        activeSoundsMap.put(sound.getAddress(), sound);
        return sound.getAddress();
    }

    @Override
    public long play(float volume, float pitch, float pan) {
        MASound sound = soundPool.obtain();
        sound.play();
        sound.setVolume(volume);
        sound.setPitch(pitch);
        sound.setPan(pan);
        activeSoundsMap.put(sound.getAddress(), sound);
        return sound.getAddress();
    }

    @Override
    public long loop() {
        MASound sound = soundPool.obtain();
        sound.loop();
        activeSoundsMap.put(sound.getAddress(), sound);
        return sound.getAddress();
    }

    @Override
    public long loop(float volume) {
        MASound sound = soundPool.obtain();
        sound.loop();
        sound.setVolume(volume);
        activeSoundsMap.put(sound.getAddress(), sound);
        return sound.getAddress();
    }

    @Override
    public long loop(float volume, float pitch, float pan) {
        MASound sound = soundPool.obtain();
        sound.loop();
        sound.setVolume(volume);
        sound.setPitch(pitch);
        sound.setPan(pan);
        activeSoundsMap.put(sound.getAddress(), sound);
        return sound.getAddress();
    }

    @Override
    public void stop() {
        for (MASound sound : activeSoundsMap.values()) {
            soundPool.free(sound);
        }
        activeSoundsMap.clear();
    }

    @Override
    public void pause() {
        group.stop();
    }

    @Override
    public void resume() {
        group.play();
    }

    @Override
    public void dispose() {
        stop();
        soundPool.clear();
        group.dispose();
    }

    @Override
    public void stop(long soundId) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        activeSoundsMap.remove(soundId);
        soundPool.free(sound);
    }

    @Override
    public void pause(long soundId) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        sound.pause();
    }

    @Override
    public void resume(long soundId) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        sound.play();
    }

    @Override
    public void setLooping(long soundId, boolean looping) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        sound.setLooping(looping);
    }

    @Override
    public void setPitch(long soundId, float pitch) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        sound.setPitch(pitch);
    }

    @Override
    public void setVolume(long soundId, float volume) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        sound.setVolume(volume);
    }

    @Override
    public void setPan(long soundId, float pan, float volume) {
        MASound sound = activeSoundsMap.get(soundId);
        if (sound == null) return;
        sound.setPan(pan);
        sound.setVolume(volume);
    }

    @Override
    public void onSoundEnd(long address) {
        MASound sound = activeSoundsMap.get(address);
        if (sound != null) {
            activeSoundsMap.remove(address);
            soundPool.free(sound);
        }
    }
}
