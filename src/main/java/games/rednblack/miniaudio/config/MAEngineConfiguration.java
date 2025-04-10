package games.rednblack.miniaudio.config;

import games.rednblack.miniaudio.MAFormatType;

/**
 * Configuration class for MiniAudio Engine
 *
 * @author fgnm
 */
public class MAEngineConfiguration {
    /** Number of listeners in 3D Spatialization. */
    public int listenerCount = 1;
    /** Native address of playback device (use -1 for default). */
    public long playbackId = -1;
    /** Native address of capture device (use -1 for default). */
    public long captureId = -1;
    /** The number of channels to use when mixing and spatializing.
     * When set to 0, will use the native channel count of the device. */
    public int channels = 0;
    /** The size of input buffer in millis. Set 0 for default. */
    public int bufferPeriodMillis = 0;
    /** The size of input buffer in PCM frames, use it to change latency (bufferSize / sampleRate).  Set 0 for default. */
    public int bufferPeriodFrames = 0;
    /** How many samples your audio interface will capture every second. Set 0 for default. */
    public int sampleRate = 0;
    /** Devices data format, see {@link MAFormatType} */
    public MAFormatType formatType = MAFormatType.F32;
    /** Enable/disable full duplex engine (require microphone permission) */
    public boolean fullDuplex = false;
    /** Enable/disable capture exclusive device mode */
    public boolean exclusive = false;
    /** Enable/disable low latency profile */
    public boolean lowLatency = true;
}
