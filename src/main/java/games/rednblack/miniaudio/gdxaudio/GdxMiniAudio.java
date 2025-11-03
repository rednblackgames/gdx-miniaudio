package games.rednblack.miniaudio.gdxaudio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import games.rednblack.miniaudio.*;

public class GdxMiniAudio implements Audio {
    private final MiniAudio miniAudio;
    private final LongMap<GdxEndListener> completionListeners = new LongMap<>();
    private final Array<GdxEndListener> listeners = new Array<>();

    public GdxMiniAudio() {
        this.miniAudio = new MiniAudio();
        MASoundEndListener endListener = maSound -> {
            GdxEndListener music = completionListeners.get(maSound.getAddress());
            if (music != null) music.onSoundEnd(maSound.getAddress());
            for(GdxEndListener listener : listeners) {
                listener.onSoundEnd(maSound.getAddress());
            }
        };
        miniAudio.setEndListener(endListener);
    }

    public MiniAudio getMiniAudio() {
        return miniAudio;
    }

    public void addCompletionListener(long address, GdxEndListener music) {
        completionListeners.put(address, music);
    }

    @Override
    public AudioDevice newAudioDevice(int samplingRate, boolean isMono) {
        throw new UnsupportedOperationException("Use MiniAudio specific API.");
    }

    @Override
    public AudioRecorder newAudioRecorder(int samplingRate, boolean isMono) {
        throw new UnsupportedOperationException("Use MiniAudio specific API.");
    }

    @Override
    public Sound newSound(FileHandle fileHandle) {
        MAGroup group = miniAudio.createGroup();
        boolean external = fileHandle.type() == Files.FileType.Absolute;
        MASoundPool soundPool = new MASoundPool(miniAudio, fileHandle.path(), (short) 0, group, external);
        GdxMASound gdxMASound = new GdxMASound(soundPool, group);
        listeners.add(gdxMASound);
        return gdxMASound;
    }

    @Override
    public Music newMusic(FileHandle file) {
        boolean external = file.type() == Files.FileType.Absolute;
        return new GdxMAMusic(miniAudio.createSound(file.path(), MASound.Flags.MA_SOUND_FLAG_STREAM, null, external), this);
    }

    @Override
    public boolean switchOutputDevice(String deviceIdentifier) {
        return false;
    }

    @Override
    public String[] getAvailableOutputDevices() {
        MADeviceInfo[] devices = miniAudio.enumerateDevices();
        String[] devicesNames = new String[devices.length];
        for (int i = 0; i < devices.length; i++) {
            devicesNames[i] = devices[i].name;
        }
        return devicesNames;
    }
}
