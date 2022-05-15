package games.rednblack.miniaudio.filter;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;

/**
 * Wrapper class to MiniAudio's High Pass Filter
 *
 * @author fgnm
 */
public class MAHighPassFilter extends MANode {

     /*JNI
        #include "miniaudio.h"
     */

    public MAHighPassFilter(MiniAudio miniAudio, double cutoffFrequency, int order) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), cutoffFrequency, order);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new IllegalStateException("Error while creating biquad node, code " + address);
        }
    }

    private native long jniCreateNode(long graphAddress, double cutoffFrequency, int order);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_hpf_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = ma_engine_get_channels(g_engine);
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_hpf_node_config_init(channels, sampleRate, cutoffFrequency, order);

        ma_hpf_node* g_Node = (ma_hpf_node*) ma_malloc(sizeof(ma_hpf_node), NULL);
        ma_result result = ma_hpf_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_hpf_node* node = (ma_hpf_node*) nodeAddress;
        ma_hpf_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}

