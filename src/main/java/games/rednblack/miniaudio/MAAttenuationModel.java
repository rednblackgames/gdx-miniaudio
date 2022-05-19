package games.rednblack.miniaudio;

/**
 * Indicates the type of the attenuation model in sounds 3D Spatialization. By default the attenuation model is set
 * to {@link #INVERSE} which is the equivalent to OpenAL's `AL_INVERSE_DISTANCE_CLAMPED`.
 *
 * @author fgnm
 */
public enum MAAttenuationModel {
    /** No distance attenuation */
    NONE(0),
    /** Equivalent to `AL_INVERSE_DISTANCE_CLAMPED` */
    INVERSE(1),
    /** Linear attenuation */
    LINEAR(2),
    /** Exponential attenuation */
    EXPONENTIAL(3);

    public final int code;

    MAAttenuationModel(int code) {
        this.code = code;
    }
}
