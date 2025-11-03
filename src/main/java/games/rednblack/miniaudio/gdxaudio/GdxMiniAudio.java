package games.rednblack.miniaudio.gdxaudio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.LongMap;
import games.rednblack.miniaudio.MADeviceInfo;
import games.rednblack.miniaudio.MASoundEndListener;
import games.rednblack.miniaudio.MiniAudio;

public class GdxMiniAudio implements Audio {
    private final MiniAudio miniAudio;
    private final LongMap<GdxMAMusic> completionListeners = new LongMap<>();

    public GdxMiniAudio() {
        this.miniAudio = new MiniAudio();
        MASoundEndListener endListener = maSound -> {
            GdxMAMusic music = completionListeners.get(maSound.getAddress());
            if (music != null) music.onSoundEnd();
        };
        miniAudio.setEndListener(endListener);
    }

    public MiniAudio getMiniAudio() {
        return miniAudio;
    }

    public void addCompletionListener(long address, GdxMAMusic music) {
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
        if (fileHandle.type() == Files.FileType.Absolute) {
            return new GdxMASound(miniAudio.createSound(fileHandle.path(), (short) 0, null, true));
        }
        return new GdxMASound(miniAudio.createSound(fileHandle.path()));
    }

    @Override
    public Music newMusic(FileHandle file) {
        if (file.type() == Files.FileType.Absolute) {
            return new GdxMAMusic(miniAudio.createSound(file.path(), (short) 0, null, true), this);
        }
        return new GdxMAMusic(miniAudio.createSound(file.path()), this);
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
