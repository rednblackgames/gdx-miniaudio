package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class MiniAudio implements Disposable {
    static {
        new SharedLibraryLoader().load("gdx-miniaudio");
    }

    /*JNI
        #define STB_VORBIS_HEADER_ONLY
        #include "stb_vorbis.c"

        #define MINIAUDIO_IMPLEMENTATION
        #include "miniaudio.h"

        #include <stdio.h>

        #ifdef MA_ANDROID
        #include <android/asset_manager_jni.h>
        AAssetManager* asset_manager = NULL;
        #include "miniaudio_android_assets.h"
        #endif

        ma_engine engine;
        ma_engine_config engineConfig;

        #ifdef MA_ANDROID
        ma_android_vfs androidVFS;
        #endif
     */

    public MiniAudio() {
        int result = init_engine();
        if (result != Result.MA_SUCCESS) {
            throw new RuntimeException("Unable to init MiniAudio Engine, error " + result);
        }
    }

    private native int init_engine();/*
        engineConfig = ma_engine_config_init();
        #if defined(MA_ANDROID)
        ma_result res = ma_android_vfs_init(&androidVFS, NULL);
        if (res != MA_SUCCESS) return res;
        engineConfig.pResourceManagerVFS = &androidVFS;
        #endif
		return ma_engine_init(&engineConfig, &engine);
	*/

    public void setupAndroid(Object assetManager) {
        jniSetupAndroid(assetManager);
    }

    private native void jniSetupAndroid(Object assetManager);/*
        #if defined(MA_ANDROID)
        asset_manager = AAssetManager_fromJava(env, assetManager);
        #endif
    */

    @Override
    public void dispose() {
        jniDispose();
    }

    private native void jniDispose();/*
        ma_engine_uninit(&engine);
    */

    public void startEngine() {
        int result = jniStartEngine();
        if (result != Result.MA_SUCCESS) {
            throw new RuntimeException("Unable to start MiniAudio Engine, error " + result);
        }
    }

    private native int jniStartEngine();/*
        ma_result res = ma_engine_start(&engine);
        if (res != MA_SUCCESS) return res;
        return MA_SUCCESS;
    */

    public void stopEngine() {
        int result = jniStopEngine();
        if (result != Result.MA_SUCCESS) {
            throw new RuntimeException("Unable to stop MiniAudio Engine, error " + result);
        }
    }

    private native int jniStopEngine();/*
        ma_result res = ma_engine_stop(&engine);
        if (res != MA_SUCCESS) return res;
        return MA_SUCCESS;
    */

    public void setMasterVolume(float volume) {
        int result = jniSetMasterVolume(volume);
        if (result != Result.MA_SUCCESS) {
            throw new RuntimeException("Unable to set MiniAudio master volume, error " + result);
        }
    }

    private native int jniSetMasterVolume(float volume);/*
        ma_result res = ma_engine_set_volume(&engine, volume);
        if (res != MA_SUCCESS) return res;
        return MA_SUCCESS;
    */

    public void setListenerPosition(float x, float y, float z) {
        jniSetListenerPosition(x, y, z);
    }

    private native void jniSetListenerPosition(float x, float y, float z);/*
        ma_engine_listener_set_position(&engine, 0, x, y, z);
    */

    public void setListenerDirection(float forwardX, float forwardY, float forwardZ) {
        jniSetListenerDirection(forwardX, forwardY, forwardZ);
    }

    private native void jniSetListenerDirection(float forwardX, float forwardY, float forwardZ);/*
        ma_engine_listener_set_direction(&engine, 0, forwardX, forwardY, forwardZ);
    */

    public void setListenerWorldUp(float x, float y, float z) {
        jniSetListenerWorldUp(x, y, z);
    }

    private native void jniSetListenerWorldUp(float x, float y, float z);/*
        ma_engine_listener_set_world_up(&engine, 0, x, y, z);
    */

    public void setListenerCone(float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        jniSetListenerCone(innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    private native void jniSetListenerCone(float innerAngleInRadians, float outerAngleInRadians, float outerGain);/*
        ma_engine_listener_set_cone(&engine, 0, innerAngleInRadians, outerAngleInRadians, outerGain);
    */

    public int playSound(String fileName) {
        return jniPlaySound(fileName);
    }

    private native int jniPlaySound(String filePath);/*
        return ma_engine_play_sound(&engine, filePath, NULL);
    */

    public MASound createSound(String fileName) {
        return new MASound(jniCreateSound(fileName, (short) 0), this);
    }

    public MASound createSound(String fileName, short flags) {
        return new MASound(jniCreateSound(fileName, flags), this);
    }

    private native long jniCreateSound(String fileName, short flags); /*
        ma_sound* sound = (ma_sound*) ma_malloc(sizeof(ma_sound), NULL);
        ma_result result = ma_sound_init_from_file(&engine, fileName, flags, NULL, NULL, sound);
        if (result != MA_SUCCESS) return (jlong) result;
        return (jlong) sound;
    */

    public void disposeSound(long soundAddress) {
        jniDisposeSound(soundAddress);
    }

    private native void jniDisposeSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_uninit(sound);
        ma_free(sound, NULL);
    */

    public void playSound(long soundAddress) {
        jniPlaySound(soundAddress);
    }

    private native void jniPlaySound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_start(sound);
    */

    public void pauseSound(long soundAddress) {
        jniPauseSound(soundAddress);
    }

    private native void jniPauseSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_stop(sound);
    */

    public void stopSound(long soundAddress) {
        jniStopSound(soundAddress);
    }

    private native void jniStopSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_stop(sound);
        ma_sound_seek_to_pcm_frame(sound, 0);
    */

    public boolean isSoundPlaying(long soundAddress) {
        return jniIsSoundPlaying(soundAddress);
    }

    private native boolean jniIsSoundPlaying(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_is_playing(sound);
    */

    public boolean isSoundEnd(long soundAddress) {
        return jniIsSoundEnd(soundAddress);
    }

    private native boolean jniIsSoundEnd(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_at_end(sound);
    */

    public boolean isSoundLooping(long soundAddress) {
        return jniIsSoundLooping(soundAddress);
    }

    private native boolean jniIsSoundLooping(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_is_looping(sound);
    */

    public void setSoundLooping(long soundAddress, boolean looping) {
        jniSetSoundLooping(soundAddress, looping);
    }

    private native void jniSetSoundLooping(long soundAddress, boolean looping);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_looping(sound, looping ? MA_TRUE : MA_FALSE);
    */

    public void setSoundVolume(long soundAddress, float volume) {
        jniSetSoundVolume(soundAddress, volume);
    }

    private native void jniSetSoundVolume(long soundAddress, float volume);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_volume(sound, volume);
    */

    public void setSoundPitch(long soundAddress, float pitch) {
        jniSetSoundPitch(soundAddress, pitch);
    }

    private native void jniSetSoundPitch(long soundAddress, float pitch);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pitch(sound, pitch);
    */

    public void setSoundPan(long soundAddress, float pan) {
        jniSetSoundPan(soundAddress, pan);
    }

    private native void jniSetSoundPan(long soundAddress, float pan);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pan(sound, pan);
    */

    public void setSoundSpatialization(long soundAddress, boolean spatial) {
        jniSetSoundSpatialization(soundAddress, spatial);
    }

    private native void jniSetSoundSpatialization(long soundAddress, boolean spatial);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_spatialization_enabled(sound, spatial ? MA_TRUE : MA_FALSE);
    */

    public void setSoundPosition(long soundAddress, float x, float y, float z) {
        jniSetSoundPosition(soundAddress, x, y, z);
    }

    private native void jniSetSoundPosition(long soundAddress, float x, float y, float z);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_position(sound, x, y, z);
    */

    public void setSoundDirection(long soundAddress, float forwardX, float forwardY, float forwardZ) {
        jniSetSoundDirection(soundAddress, forwardX, forwardY, forwardZ);
    }

    private native void jniSetSoundDirection(long soundAddress, float forwardX, float forwardY, float forwardZ);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_direction(sound, forwardX, forwardY, forwardZ);
    */

    public void seekSoundTo(long soundAddress, float seconds) {
        jniSeekSoundTo(soundAddress, seconds);
    }

    private native void jniSeekSoundTo(long soundAddress, float seconds);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_uint64 frameIndex = ma_engine_get_sample_rate(&engine) * seconds;
        ma_sound_seek_to_pcm_frame(sound, frameIndex);
    */

    public void soundFade(long soundAddress, float start, float end, float milliseconds) {
        jniSoundFade(soundAddress, start, end, milliseconds);
    }

    private native void jniSoundFade(long soundAddress, float start, float end, float milliseconds);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_fade_in_milliseconds(sound, start, end, milliseconds);
    */
}
