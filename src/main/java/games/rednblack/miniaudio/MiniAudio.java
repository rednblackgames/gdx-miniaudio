package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.SharedLibraryLoader;

/**
 * Main Audio Engine interface that handle calls with native library.
 *
 * @author fgnm
 */
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

    /**
     * Android native implementation needs the AssetManager reference from Java code.
     * Call this only in Android Applications and passing `Context#getAssets()` object.
     *
     * @param assetManager Android's Native AssetManager
     */
    public void setupAndroid(Object assetManager) {
        jniSetupAndroid(assetManager);
    }

    private native void jniSetupAndroid(Object assetManager);/*
        #if defined(MA_ANDROID)
        asset_manager = AAssetManager_fromJava(env, assetManager);
        #endif
    */

    /**
     * Dispose Engine resources
     */
    @Override
    public void dispose() {
        jniDispose();
    }

    private native void jniDispose();/*
        ma_engine_uninit(&engine);
    */

    /**
     * Set engine in play mode. Does nothing if Engine is already started.
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

    /**
     * Stop audio processing and pause the Engine. Does nothing if Engine is already paused.
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

    /**
     * Set master volume of the engine.
     *
     * @param volume float value where 0 is silence and 1 the base volume.
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

    /**
     * Used for 3D Spatialization, set the position of the current listener in world coordinates.
     *
     * @param x position
     * @param y position
     * @param z position
     */
    public void setListenerPosition(float x, float y, float z) {
        jniSetListenerPosition(x, y, z);
    }

    private native void jniSetListenerPosition(float x, float y, float z);/*
        ma_engine_listener_set_position(&engine, 0, x, y, z);
    */

    /**
     * The direction of the listener represents it's forward vector.
     *
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setListenerDirection(float forwardX, float forwardY, float forwardZ) {
        jniSetListenerDirection(forwardX, forwardY, forwardZ);
    }

    private native void jniSetListenerDirection(float forwardX, float forwardY, float forwardZ);/*
        ma_engine_listener_set_direction(&engine, 0, forwardX, forwardY, forwardZ);
    */

    /**
     * The listener's up vector can also be specified and defaults to +1 on the Y axis.
     * Default 0, 1, 0.
     *
     * @param x normal
     * @param y normal
     * @param z normal
     */
    public void setListenerWorldUp(float x, float y, float z) {
        jniSetListenerWorldUp(x, y, z);
    }

    private native void jniSetListenerWorldUp(float x, float y, float z);/*
        ma_engine_listener_set_world_up(&engine, 0, x, y, z);
    */

    /**
     * The engine supports directional attenuation. The listener can have a cone the controls how sound is
     * attenuated based on the listener's direction. When a sound is between the inner and outer cones, it
     * will be attenuated between 1 and the cone's outer gain
     *
     * @param innerAngleInRadians inner angle in radiance
     * @param outerAngleInRadians outer angle in radiance
     * @param outerGain outer gain
     */
    public void setListenerCone(float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        jniSetListenerCone(innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    private native void jniSetListenerCone(float innerAngleInRadians, float outerAngleInRadians, float outerGain);/*
        ma_engine_listener_set_cone(&engine, 0, innerAngleInRadians, outerAngleInRadians, outerGain);
    */

    /**
     * Play sound with "fire and forget"
     *
     * @param fileName path of the file relative to assets folder
     * @return status check {@link Result}
     */
    public int playSound(String fileName) {
        return jniPlaySound(fileName);
    }

    private native int jniPlaySound(String filePath);/*
        return ma_engine_play_sound(&engine, filePath, NULL);
    */

    /**
     * Load a new {@link MASound} object and upload sound data into memory. Each {@link MASound} object
     * represents a single instance of the sound. If you want to play the same sound multiple times at the same time,
     * you need to initialize a separate {@link MASound} object.
     *
     * @param fileName path of the file relative to assets folder
     * @return {@link MASound} object.
     */
    public MASound createSound(String fileName) {
        return new MASound(jniCreateSound(fileName, (short) 0), this);
    }

    /**
     * Load a new {@link MASound} object and upload sound data into memory. Each {@link MASound} object
     * represents a single instance of the sound. If you want to play the same sound multiple times at the same time,
     * you need to initialize a separate {@link MASound} object.
     *
     * {@link MASound.Flags} are useful to customize sound loading and managing
     *
     * @param fileName path of the file relative to assets folder
     * @param flags flags for audio loading
     * @return {@link MASound} object.
     */
    public MASound createSound(String fileName, short flags) {
        return new MASound(jniCreateSound(fileName, flags), this);
    }

    private native long jniCreateSound(String fileName, short flags); /*
        ma_sound* sound = (ma_sound*) ma_malloc(sizeof(ma_sound), NULL);
        ma_result result = ma_sound_init_from_file(&engine, fileName, flags, NULL, NULL, sound);
        if (result != MA_SUCCESS) return (jlong) result;
        return (jlong) sound;
    */

    /**
     * Free sound memory. Use when not needed.
     *
     * @param soundAddress native address to sound object
     */
    public void disposeSound(long soundAddress) {
        jniDisposeSound(soundAddress);
    }

    private native void jniDisposeSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_uninit(sound);
        ma_free(sound, NULL);
    */

    /**
     * Play or resume sound.
     *
     * @param soundAddress native address to sound object
     */
    public void playSound(long soundAddress) {
        jniPlaySound(soundAddress);
    }

    private native void jniPlaySound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_start(sound);
    */

    /**
     * Pause sound.
     *
     * @param soundAddress native address to sound object
     */
    public void pauseSound(long soundAddress) {
        jniPauseSound(soundAddress);
    }

    private native void jniPauseSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_stop(sound);
    */

    /**
     * Pause and rewind sound to beginning.
     *
     * @param soundAddress native address to sound object
     */
    public void stopSound(long soundAddress) {
        jniStopSound(soundAddress);
    }

    private native void jniStopSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_stop(sound);
        ma_sound_seek_to_pcm_frame(sound, 0);
    */

    /**
     * Check if a sound is playing
     *
     * @param soundAddress native address to sound object
     * @return true if sound is playing false otherwise
     */
    public boolean isSoundPlaying(long soundAddress) {
        return jniIsSoundPlaying(soundAddress);
    }

    private native boolean jniIsSoundPlaying(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_is_playing(sound);
    */

    /**
     * Check if a sound is at the end
     *
     * @param soundAddress native address to sound object
     * @return true if sound is at the end false otherwise
     */
    public boolean isSoundEnd(long soundAddress) {
        return jniIsSoundEnd(soundAddress);
    }

    private native boolean jniIsSoundEnd(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_at_end(sound);
    */

    /**
     * Check if a sound is looping
     *
     * @param soundAddress native address to sound object
     * @return true if sound is looping false otherwise
     */
    public boolean isSoundLooping(long soundAddress) {
        return jniIsSoundLooping(soundAddress);
    }

    private native boolean jniIsSoundLooping(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_is_looping(sound);
    */

    /**
     * Set if the sound should loop or not
     *
     * @param soundAddress native address to sound object
     * @param looping true if sound should loop
     */
    public void setSoundLooping(long soundAddress, boolean looping) {
        jniSetSoundLooping(soundAddress, looping);
    }

    private native void jniSetSoundLooping(long soundAddress, boolean looping);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_looping(sound, looping ? MA_TRUE : MA_FALSE);
    */

    /**
     * Set sound volume.
     *
     * @param soundAddress native address to sound object
     * @param volume 0 for silence, 1 for default volume, greater than 1 lauder
     */
    public void setSoundVolume(long soundAddress, float volume) {
        jniSetSoundVolume(soundAddress, volume);
    }

    private native void jniSetSoundVolume(long soundAddress, float volume);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_volume(sound, volume);
    */

    /**
     * Control Sound Pitch A larger value will result in a higher pitch. The pitch must be greater than 0.
     *
     * @param soundAddress native address to sound object
     * @param pitch value, 1 default
     */
    public void setSoundPitch(long soundAddress, float pitch) {
        if (pitch <= 0) throw new IllegalArgumentException("Pitch must be > 0");
        jniSetSoundPitch(soundAddress, pitch);
    }

    private native void jniSetSoundPitch(long soundAddress, float pitch);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pitch(sound, pitch);
    */

    /**
     * Setting the pan to 0 will result in an unpanned sound. Setting it to -1 will shift everything to the left, whereas
     * +1 will shift it to the right.
     *
     * @param soundAddress native address to sound object
     * @param pan value in the range [-1, 1]
     */
    public void setSoundPan(long soundAddress, float pan) {
        jniSetSoundPan(soundAddress, pan);
    }

    private native void jniSetSoundPan(long soundAddress, float pan);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pan(sound, pan);
    */

    /**
     * Enable or disable sound spatialization effects.
     *
     * @param soundAddress native address to sound object
     * @param spatial true by default
     */
    public void setSoundSpatialization(long soundAddress, boolean spatial) {
        jniSetSoundSpatialization(soundAddress, spatial);
    }

    private native void jniSetSoundSpatialization(long soundAddress, boolean spatial);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_spatialization_enabled(sound, spatial ? MA_TRUE : MA_FALSE);
    */

    /**
     * Sounds have a position for 3D Spatialization. By default, the position of a sound is in absolute space.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param x position
     * @param y position
     * @param z position
     */
    public void setSoundPosition(long soundAddress, float x, float y, float z) {
        jniSetSoundPosition(soundAddress, x, y, z);
    }

    private native void jniSetSoundPosition(long soundAddress, float x, float y, float z);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_position(sound, x, y, z);
    */

    /**
     * Sounds have a direction for 3D Spatialization. By default, the position of a sound is in absolute space.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setSoundDirection(long soundAddress, float forwardX, float forwardY, float forwardZ) {
        jniSetSoundDirection(soundAddress, forwardX, forwardY, forwardZ);
    }

    private native void jniSetSoundDirection(long soundAddress, float forwardX, float forwardY, float forwardZ);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_direction(sound, forwardX, forwardY, forwardZ);
    */

    /**
     * Set current audio playing position in seconds.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param seconds to seek the audio track
     */
    public void seekSoundTo(long soundAddress, float seconds) {
        jniSeekSoundTo(soundAddress, seconds);
    }

    private native void jniSeekSoundTo(long soundAddress, float seconds);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_uint64 frameIndex = ma_engine_get_sample_rate(&engine) * seconds;
        ma_sound_seek_to_pcm_frame(sound, frameIndex);
    */

    /**
     * Smoothly fade audio volume between two values.
     *
     * @param soundAddress  soundAddress native address to sound object
     * @param start starting volume (use -1 for current volume)
     * @param end ending volume (use -1 for current volume)
     * @param milliseconds fade duration in milliseconds
     */
    public void soundFade(long soundAddress, float start, float end, float milliseconds) {
        jniSoundFade(soundAddress, start, end, milliseconds);
    }

    private native void jniSoundFade(long soundAddress, float start, float end, float milliseconds);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_fade_in_milliseconds(sound, start, end, milliseconds);
    */
}
