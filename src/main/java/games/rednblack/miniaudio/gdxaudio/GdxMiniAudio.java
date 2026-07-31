package games.rednblack.miniaudio.gdxaudio;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import games.rednblack.miniaudio.*;
import games.rednblack.miniaudio.MASound.Flags;

public class GdxMiniAudio implements Audio {
    private final MiniAudio miniAudio;
    private final LongMap<GdxEndListener> completionListeners = new LongMap<>();
    private final Array<GdxEndListener> listeners = new Array<>();

    public GdxMiniAudio() {
        this(new MiniAudio());
    }

    public GdxMiniAudio(MiniAudio miniAudio) {
        this.miniAudio = miniAudio;
        // End events are fired from the native dispatch thread, post them to the
        // render thread to serialize access with play()/stop()/newSound()
        MASoundEndListener endListener = maSound -> {
            long address = maSound.getAddress();
            Gdx.app.postRunnable(() -> {
                GdxEndListener music = completionListeners.get(address);
                if (music != null) music.onSoundEnd(address);
                for(GdxEndListener listener : listeners) {
                    listener.onSoundEnd(address);
                }
            });
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
        boolean external = isExternal(fileHandle);
        String path = external ? fileHandle.file().getAbsolutePath() : fileHandle.path();
        MASoundPool soundPool = new MASoundPool(miniAudio, path, (short) 0, group, external);
        GdxMASound gdxMASound = new GdxMASound(soundPool, group);
        listeners.add(gdxMASound);
        return gdxMASound;
    }

    @Override
    public Music newMusic(FileHandle file) {
        boolean external = isExternal(file);
        String path = external ? file.file().getAbsolutePath() : file.path();
        MASound sound = miniAudio.createSound(path, Flags.MA_SOUND_FLAG_STREAM, null, external);
        return new GdxMAMusic(sound, this);
    }

    @Override
    public boolean switchOutputDevice(String deviceIdentifier) {
        MADeviceInfo[] devices = miniAudio.getAvailableDevices();
        MADeviceInfo targetDevice = null;
        for (MADeviceInfo device : devices) {
            if (!device.isCapture && device.name.equals(deviceIdentifier)) {
                targetDevice = device;
                break;
            }
        }

        if (targetDevice == null) return false;

        try {
            miniAudio.changeDevice(targetDevice, null);
        } catch (Exception ignore) {
            return false;
        }

        return true;
    }

    @Override
    public String[] getAvailableOutputDevices() {
        miniAudio.refreshAvailableDevices();
        Array<String> outputDevices = new Array<>();
        MADeviceInfo[] devices = miniAudio.getAvailableDevices();
        for (MADeviceInfo device : devices) {
            if (!device.isCapture) {
                outputDevices.add(device.name);
            }
        }
        return outputDevices.toArray();
    }

    private static boolean isExternal(FileHandle file) {
        return file.type() == FileType.Absolute
            || file.type() == FileType.External
            || file.type() == FileType.Local;
    }

    @Override
    public void dispose() {
        miniAudio.dispose();
    }
}
