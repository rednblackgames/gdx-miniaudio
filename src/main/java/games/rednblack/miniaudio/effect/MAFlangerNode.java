package games.rednblack.miniaudio.effect;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

public class MAFlangerNode extends MANode {
    /*JNI
        #include "miniaudio.h"
        #include "ma_flanger_node.h"
     */

    public MAFlangerNode(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MAFlangerNode(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating flanger node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_flanger_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_flanger_node_config_init(channels, sampleRate);

        ma_flanger_node* g_Node = (ma_flanger_node*) ma_malloc(sizeof(ma_flanger_node), NULL);
        ma_result result = ma_flanger_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_flanger_node* node = (ma_flanger_node*) nodeAddress;
        ma_flanger_node_uninit(node, NULL);
        ma_free(node, NULL);
    */

    /**
     * LFO rate in Hz. (Default: 0.2)
     *
     * @param rate in Hz
     */
    public void setRate(float rate) {
        jniSetRate(address, rate);
    }

    private native void jniSetRate(long address, float rate);/*
        ma_flanger_node* node = (ma_flanger_node*) address;
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
        ma_flanger_node* node = (ma_flanger_node*) address;
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
        ma_flanger_node* node = (ma_flanger_node*) address;
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
        ma_flanger_node* node = (ma_flanger_node*) address;
        node->dry = dry;
    */

    /**
     * Set Base delay of the flanger (typically 1-10ms). (Default: 5.0).
     *
     * @param delay in milliseconds
     */
    public void setDelay(float delay) {
        jniSetDelay(address, delay);
    }

    private native void jniSetDelay(long address, float delay);/*
        ma_flanger_node* node = (ma_flanger_node*) address;
        node->delay = delay;
    */

    /**
     * Feedback level. Default: 0.5
     *
     * @param feedback from -1.0 to 1.0
     */
    public void setFeedback(float feedback) {
        jniSetFeedback(address, feedback);
    }

    private native void jniSetFeedback(long address, float feedback);/*
        ma_flanger_node* node = (ma_flanger_node*) address;
        node->feedback = feedback;
    */
}
