package games.rednblack.miniaudio;

/**
 * Wrapper class for mange native sound group objects.
 *
 * @author fgnm
 */
public class MAGroup extends MANode {

    public MAGroup(long address, MiniAudio miniAudio) {
        super(miniAudio);
        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while loading group", (int) address);
        }
        this.address = address;
    }

    /**
     * Play group.
     */
    public void play() {
        miniAudio.playGroup(address);
    }

    /**
     * Pause group.
     */
    public void pause() {
        miniAudio.pauseGroup(address);
    }

    /**
     * Set sound group volume.
     *
     * @param volume 0 for silence, 1 for default volume, greater than 1 lauder
     */
    public void setVolume(float volume) {
        miniAudio.setGroupVolume(address, volume);
    }

    /**
     * Control Sound Group Pitch A larger value will result in a higher pitch. The pitch must be greater than 0.
     *
     * @param pitch value, 1 default
     */
    public void setPitch(float pitch) {
        miniAudio.setGroupPitch(address, pitch);
    }

    /**
     * Setting the pan to 0 will result in an unpanned sound. Setting it to -1 will shift everything to the left, whereas
     * +1 will shift it to the right.
     *
     * @param pan value in the range [-1, 1]
     */
    public void setPan(float pan) {
        miniAudio.setGroupPan(address, pan);
    }

    /**
     * Enable or disable sound spatialization effects.
     *
     * @param spatialization true by default
     */
    public void setSpatialization(boolean spatialization) {
        miniAudio.setGroupSpatialization(address, spatialization);
    }

    /**
     * By default, groups will be spatialized based on the closest listener. If a group should always be spatialized
     * relative to a specific listener it can be pinned to one
     *
     * @param listenerIndex index of the pinned listener
     */
    public void setPinnedListenerIndex(int listenerIndex) {
        miniAudio.setGroupPinnedListenerIndex(address, listenerIndex);
    }

    /**
     * Groups have a position for 3D Spatialization. By default, the position of a group is in absolute space or
     * relative to its listener see {@link #setPositioning(MAPositioning)}.
     *
     * @param x position
     * @param y position
     * @param z position
     */
    public void setPosition(float x, float y, float z) {
        miniAudio.setGroupPosition(address, x, y, z);
    }

    /**
     * Groups have a direction for 3D Spatialization.
     *
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setDirection(float forwardX, float forwardY, float forwardZ) {
        miniAudio.setGroupDirection(address, forwardX, forwardY, forwardZ);
    }

    /**
     * Groups also have a cone for controlling directional attenuation. This works exactly the same as
     * listeners
     *
     * @param innerAngleInRadians inner angle in radiance
     * @param outerAngleInRadians outer angle in radiance
     * @param outerGain outer gain
     */
    public void setCone(float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        miniAudio.setGroupCone(address, innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    /**
     * The velocity of a group is used for doppler effect.
     *
     * @param velocityX doppler velocity on x-axis
     * @param velocityY doppler velocity on y-axis
     * @param velocityZ doppler velocity on z-axis
     */
    public void setVelocity(float velocityX, float velocityY, float velocityZ) {
        miniAudio.setGroupVelocity(address, velocityX, velocityY, velocityZ);
    }

    /**
     * The engine supports different attenuation models which can be configured on a per-sound basis.
     *
     * @param attenuationModel set pre defined attenuation model
     */
    public void setAttenuationModel(MAAttenuationModel attenuationModel) {
        miniAudio.setGroupAttenuationModel(address, attenuationModel);
    }

    /**
     * Groups have a position. By default, the position of a sound is in absolute space,
     * but it can be changed to be relative to a listener.
     *
     * @param positioning type of coordinates position
     */
    public void setPositioning(MAPositioning positioning) {
        miniAudio.setGroupPositioning(address, positioning);
    }

    /**
     * To control how quickly a group rolls off as it moves away from the listener, rolloff needs to be configured
     *
     * @param rolloff value of the rolloff effect
     */
    public void setRolloff(float rolloff) {
        miniAudio.setGroupRolloff(address, rolloff);
    }

    /**
     * Set the minimum and maximum gain to apply from spatialization.
     *
     * @param minGain minimum gain to apply
     * @param maxGain maximum gain to apply
     */
    public void setGainRange(float minGain, float maxGain) {
        miniAudio.setGroupGainRange(address, minGain, maxGain);
    }

    /**
     * Likewise, in the calculation of attenuation, you can control the minimum and maximum distances for
     * the attenuation calculation. This is useful if you want to ensure sounds don't drop below a certain
     * volume after the listener moves further away and to have sounds play a maximum volume when the
     * listener is within a certain distance.
     *
     * @param minDistance minimum distance
     * @param maxDistance maximum distance
     */
    public void setDistanceRange(float minDistance, float maxDistance) {
        miniAudio.setGroupDistanceRange(address, minDistance, maxDistance);
    }

    /**
     * The engine's spatialization system supports doppler effect. The doppler factor can be configured on
     * a per-sound basis
     *
     * @param dopplerFactor doppler factor
     */
    public void setDopplerFactor(float dopplerFactor) {
        miniAudio.setGroupDopplerFactor(address, dopplerFactor);
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
        miniAudio.groupFade(address, 0, targetVolume, milliseconds);
    }

    /**
     * Smoothly fade out audio to a target volume.
     *
     * @param milliseconds fade duration in milliseconds
     * @param targetVolume target fade volume
     */
    public void fadeOut(float milliseconds, float targetVolume) {
        miniAudio.groupFade(address, -1, targetVolume, milliseconds);
    }

    /**
     * Smoothly fade out audio.
     *
     * @param milliseconds fade duration in milliseconds
     */
    public void fadeOut(float milliseconds) {
        fadeOut(milliseconds, 0);
    }

    @Override
    public int getSupportedOutputs() {
        return 1;
    }

    @Override
    public void dispose() {
        miniAudio.disposeGroup(address);
    }
}
