package games.rednblack.miniaudio;

/**
 * Wrapper class for mange native sound objects.
 *
 * @author fgnm
 */
public class MASound extends MANode {
    public MASound(long address, MiniAudio miniAudio) {
        super(miniAudio);
        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new IllegalStateException("Error while loading Sound, code " + address);
        }
        this.address = address;
    }

    /**
     * Play audio.
     */
    public void play() {
        miniAudio.playSound(address);
    }

    /**
     * Play audio in loop mode.
     */
    public void loop() {
        setLooping(true);
        play();
    }

    /**
     * Pause audio.
     */
    public void pause() {
        miniAudio.pauseSound(address);
    }

    /**
     * Stop audio.
     */
    public void stop() {
        miniAudio.stopSound(address);
    }

    /**
     * Check if audio is currently playing.
     *
     * @return true if audio is playing
     */
    public boolean isPlaying() {
        return miniAudio.isSoundPlaying(address);
    }

    /**
     * Check if audio is ad the end.
     *
     * @return true if is at the end
     */
    public boolean isEnd() {
        return miniAudio.isSoundEnd(address);
    }

    /**
     * Check if audio is looping.
     *
     * @return true if looping
     */
    public boolean isLooping() {
        return miniAudio.isSoundLooping(address);
    }

    /**
     * Set audio in loop mode
     * @param looping true to loop audio
     */
    public void setLooping(boolean looping) {
        miniAudio.setSoundLooping(address, looping);
    }

    /**
     * Set sound volume
     * @param volume 0 for silence, 1 for default volume, greater than 1 lauder
     */
    public void setVolume(float volume) {
        miniAudio.setSoundVolume(address, volume);
    }

    /**
     * Control Sound Pitch A larger value will result in a higher pitch. The pitch must be greater than 0.
     *
     * @param pitch value, 1 default
     */
    public void setPitch(float pitch) {
        miniAudio.setSoundPitch(address, pitch);
    }

    /**
     * Setting the pan to 0 will result in an unpanned sound. Setting it to -1 will shift everything to the left, whereas
     * +1 will shift it to the right.
     *
     * @param pan value in the range [-1, 1]
     */
    public void setPan(float pan) {
        miniAudio.setSoundPan(address, pan);
    }

    /**
     * Enable or disable sound spatialization effects.
     *
     * @param spatialization true by default
     */
    public void setSpatialization(boolean spatialization) {
        miniAudio.setSoundSpatialization(address, spatialization);
    }

    /**
     * Sounds have a position for 3D Spatialization. By default, the position of a sound is in absolute space.
     *
     * @param x position
     * @param y position
     * @param z position
     */
    public void setPosition(float x, float y, float z) {
        miniAudio.setSoundPosition(address, x, y, z);
    }

    /**
     * Sounds have a direction for 3D Spatialization. By default, the position of a sound is in absolute space.
     *
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setDirection(float forwardX, float forwardY, float forwardZ) {
        miniAudio.setSoundDirection(address, forwardX, forwardY, forwardZ);
    }

    /**
     * Set current audio playing position in seconds.
     *
     * @param seconds to seek the audio track
     */
    public void seekTo(float seconds) {
        miniAudio.seekSoundTo(address, seconds);
    }

    /**
     * Smoothly fade in audio.
     *
     * @param milliseconds fade duration in milliseconds
     */
    public void fadeIn(float milliseconds) {
        fadeIn(milliseconds, 1);
    }

    /**
     * Smoothly fade in audio to a target volume.
     *
     * @param milliseconds fade duration in milliseconds
     * @param targetVolume target fade volume
     */
    public void fadeIn(float milliseconds, float targetVolume) {
        miniAudio.soundFade(address, 0, targetVolume, milliseconds);
    }

    /**
     * Smoothly fade out audio to a target volume.
     *
     * @param milliseconds fade duration in milliseconds
     * @param targetVolume target fade volume
     */
    public void fadeOut(float milliseconds, float targetVolume) {
        miniAudio.soundFade(address, -1, targetVolume, milliseconds);
    }

    /**
     * Smoothly fade out audio.
     *
     * @param milliseconds fade duration in milliseconds
     */
    public void fadeOut(float milliseconds) {
        fadeOut(milliseconds, 0);
    }

    /**
     * Get current sound cursor position.
     *
     * @return cursor position in seconds
     */
    public float getCursorPosition() {
        return miniAudio.getSoundCursorPosition(address);
    }

    /**
     * Get total length of the sound.
     *
     * @return sound length in seconds
     */
    public float getLength() {
        return miniAudio.getSoundLength(address);
    }

    /**
     * Release and dispose sound objects
     */
    @Override
    public void dispose() {
        miniAudio.disposeSound(address);
    }

    @Override
    public void attachToNode(MANode previousNode, int outputBus) {
        throw new RuntimeException("Sounds doesn't have input bus");
    }

    @Override
    public int getSupportedOutputs() {
        return 1;
    }

    /**
     * Flags to customize sound loading and management {@link MiniAudio#createSound(String, short)}
     *
     * @author fgnm
     */
    public static class Flags {
        /**
         * If loading the entire sound into memory is prohibitive, you can also configure the engine to stream the audio data
         * */
        public static final short MA_SOUND_FLAG_STREAM                = 0x00000001;

        /**
         * By default, the sound is synchronously loaded fully into memory straight from the file system without
         * any kind of decoding. If you want to decode the sound before storing it in memory add this flag
         */
        public static final short MA_SOUND_FLAG_DECODE                = 0x00000002;

        /** If you want to load the sound asynchronously */
        public static final short MA_SOUND_FLAG_ASYNC                 = 0x00000004;

        /** MA_RESOURCE_MANAGER_DATA_SOURCE_FLAG_WAIT_INIT */
        public static final short MA_SOUND_FLAG_WAIT_INIT             = 0x00000008;

        /** Do not attach to the endpoint by default. Useful for when setting up nodes in a complex graph system. */
        public static final short MA_SOUND_FLAG_NO_DEFAULT_ATTACHMENT = 0x00000010;

        /** Disable pitch shifting with ma_sound_set_pitch() and ma_sound_group_set_pitch(). This is an optimization. */
        public static final short MA_SOUND_FLAG_NO_PITCH              = 0x00000020;

        /* Disable spatialization. */
        public static final short MA_SOUND_FLAG_NO_SPATIALIZATION     = 0x00000040;
    }
}
