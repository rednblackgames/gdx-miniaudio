package games.rednblack.miniaudio.effect;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Represents a chorus audio effect node.
 * <p>
 * This effect creates a thicker, shimmering sound by mixing the original audio
 * with one or more delayed and pitch-modulated copies. It simulates the sound
 * of multiple voices or instruments playing the same part, adding richness
 * and texture to the signal.
 *
 * Key Parameters:
 * <ul>
 * <li><b>Rate:</b> The speed (in Hz) of the LFO that modulates the delay time.</li>
 * <li><b>Depth:</b> The intensity of the pitch/delay modulation.</li>
 * <li><b>Delay:</b> The base delay time (in ms) for the copied signals.</li>
 * </ul>
 *
 * @author fgnm
 */
public class MAChorusNode extends MANode {
    /*JNI
        #include "miniaudio.h"
        #include "ma_chorus_node.h"
     */

    public MAChorusNode(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MAChorusNode(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating chorus node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_chorus_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_chorus_node_config_init(channels, sampleRate);

        ma_chorus_node* g_Node = (ma_chorus_node*) ma_malloc(sizeof(ma_chorus_node), NULL);
        ma_result result = ma_chorus_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
        if (result != MA_SUCCESS) {
            ma_free(g_Node, NULL);
            return (jlong) result;
        }
        return (jlong) g_Node;
    */

    @Override
    public int getSupportedOutputs() {
        return 1;
    }

    @Override
    public void dispose() {
        jniDispose(address);
    }

    private native void jniDispose(long nodeAddress); /*
        ma_chorus_node* node = (ma_chorus_node*) nodeAddress;
        ma_chorus_node_uninit(node, NULL);
        ma_free(node, NULL);
    */

    /**
     * LFO rate in Hz. (Default: 0.5)
     *
     * @param rate in Hz
     */
    public void setRate(float rate) {
        jniSetRate(address, rate);
    }

    private native void jniSetRate(long address, float rate);/*
        ma_chorus_node* node = (ma_chorus_node*) address;
        node->rate = rate;
    */

    /**
     * Modulation depth in milliseconds. (Default: 2.0).
     *
     * @param depth in milliseconds
     */
    public void setDepth(float depth) {
        jniSetDepth(address, depth);
    }

    private native void jniSetDepth(long address, float depth);/*
        ma_chorus_node* node = (ma_chorus_node*) address;
        node->depth = depth;
    */

    /**
     * Wet signal mix level. (Default: 0.5).
     *
     * @param wet from 0.0 to 1.0
     */
    public void setWet(float wet) {
        jniSetWet(address, wet);
    }

    private native void jniSetWet(long address, float wet);/*
        ma_chorus_node* node = (ma_chorus_node*) address;
        node->wet = wet;
    */

    /**
     * Dry signal mix level. (Default: 0.5).
     *
     * @param dry from 0.0 to 1.0
     */
    public void setDry(float dry) {
        jniSetDry(address, dry);
    }

    private native void jniSetDry(long address, float dry);/*
        ma_chorus_node* node = (ma_chorus_node*) address;
        node->dry = dry;
    */

    /**
     * Set Base delay of the chorus. (Default: 25.0).
     *
     * @param delay in milliseconds
     */
    public void setDelay(float delay) {
        jniSetDelay(address, delay);
    }

    private native void jniSetDelay(long address, float delay);/*
        ma_chorus_node* node = (ma_chorus_node*) address;
        node->delay = delay;
    */
}
