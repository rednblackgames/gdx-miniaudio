package games.rednblack.miniaudio.effect;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Delay Node
 *
 * @author fgnm
 */
public class MADelayNode extends MANode {
    /*JNI
        #include "miniaudio.h"
     */

    public MADelayNode(MiniAudio miniAudio, float delay, float decay) {
        this(miniAudio, delay, decay, -1);
    }

    public MADelayNode(MiniAudio miniAudio, float delay, float decay, int customChannels) {
        super(miniAudio);

        address = jniCreateDelayNode(miniAudio.getEngineAddress(), delay, decay, customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating delay node", (int) address);
        }
    }

    @Override
    public int getSupportedOutputs() {
        return 1;
    }

    private native long jniCreateDelayNode(long graphAddress, float delay, float decay, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_delay_node_config delayNodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        delayNodeConfig = ma_delay_node_config_init(channels, sampleRate, (ma_uint32)(sampleRate * delay), decay);

        ma_delay_node* g_delayNode = (ma_delay_node*) ma_malloc(sizeof(ma_delay_node), NULL);
        ma_result result = ma_delay_node_init(ma_engine_get_node_graph(g_engine), &delayNodeConfig, NULL, g_delayNode);
        if (result != MA_SUCCESS) {
            ma_free(g_delayNode, NULL);
            return (jlong) result;
        }
        return (jlong) g_delayNode;
    */

    @Override
    public void dispose() {
        jniDispose(address);
    }

    private native void jniDispose(long nodeAddress); /*
        ma_delay_node* node = (ma_delay_node*) nodeAddress;
        ma_delay_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
