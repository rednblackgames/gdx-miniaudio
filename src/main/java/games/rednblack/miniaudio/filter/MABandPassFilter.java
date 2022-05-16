package games.rednblack.miniaudio.filter;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Band Pass Filter
 *
 * @author fgnm
 */
public class MABandPassFilter extends MANode {

     /*JNI
        #include "miniaudio.h"
     */

    public MABandPassFilter(MiniAudio miniAudio, double cutoffFrequency, int order) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), cutoffFrequency, order);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new MiniAudioException("Error while creating band pass filter node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, double cutoffFrequency, int order);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_bpf_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = ma_engine_get_channels(g_engine);
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_bpf_node_config_init(channels, sampleRate, cutoffFrequency, order);

        ma_bpf_node* g_Node = (ma_bpf_node*) ma_malloc(sizeof(ma_bpf_node), NULL);
        ma_result result = ma_bpf_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_bpf_node* node = (ma_bpf_node*) nodeAddress;
        ma_bpf_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}

