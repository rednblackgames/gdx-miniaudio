package games.rednblack.miniaudio.effect;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * A peak limiter acts as a "brick wall", preventing an audio signal from
 * exceeding a set ceiling level. It's used to prevent digital clipping
 * and increase the overall loudness of a signal.
 *
 * @author fgnm
 */
public class MALimiterNode extends MANode {
     /*JNI
        #include "miniaudio.h"
        #include "ma_limiter_node.h"
     */

    public MALimiterNode(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MALimiterNode(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating limiter node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_limiter_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_limiter_node_config_init(channels, sampleRate);

        ma_limiter_node* g_Node = (ma_limiter_node*) ma_malloc(sizeof(ma_limiter_node), NULL);
        ma_result result = ma_limiter_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_limiter_node* node = (ma_limiter_node*) nodeAddress;
        ma_limiter_node_uninit(node, NULL);
        ma_free(node, NULL);
    */

    /**
     * The maximum output level in decibels (dB). Default: -1.0
     *
     * @param ceilingDb in decibels (dB)
     */
    public void setCeilingDb(float ceilingDb) {
        jniSetCeilingDb(address, ceilingDb);
    }

    private native void jniSetCeilingDb(long address, float ceilingDb);/*
        ma_limiter_node* node = (ma_limiter_node*) address;
        node->ceilingLinear = powf(10.0f, ceilingDb / 20.0f);
    */

    /**
     * Gain applied to the input signal in dB. Default: 0.0
     *
     * @param inputGainDb in decibels (dB)
     */
    public void setInputGainDb(float inputGainDb) {
        jniSetInputGainDb(address, inputGainDb);
    }

    private native void jniSetInputGainDb(long address, float inputGainDb);/*
        ma_limiter_node* node = (ma_limiter_node*) address;
        node->inputGainLinear = powf(10.0f, inputGainDb / 20.0f);
    */

    /**
     * Attack time in milliseconds. Default: 1.0
     *
     * @param attack in milliseconds
     */
    public void setAttack(float attack) {
        jniSetAttackMs(address, attack);
    }

    private native void jniSetAttackMs(long address, float attackMs);/*
        ma_limiter_node* node = (ma_limiter_node*) address;
        double attack_seconds = attackMs / 1000.0;
        node->attackCoeff  = (float)exp(-1.0 / (attack_seconds * node->sampleRate));
    */

    /**
     * Release time in milliseconds. Default: 100.0
     *
     * @param release in milliseconds
     */
    public void setRelease(float release) {
        jniSetRelease(address, release);
    }

    private native void jniSetRelease(long address, float releaseMs);/*
        ma_limiter_node* node = (ma_limiter_node*) address;
        double release_seconds = releaseMs / 1000.0;
        node->releaseCoeff  = (float)exp(-1.0 / (release_seconds * node->sampleRate));
    */
}
