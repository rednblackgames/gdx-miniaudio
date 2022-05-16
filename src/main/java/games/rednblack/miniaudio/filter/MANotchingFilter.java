package games.rednblack.miniaudio.filter;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Notching Filter
 *
 * @author fgnm
 */
public class MANotchingFilter extends MANode {

     /*JNI
        #include "miniaudio.h"
     */

    public MANotchingFilter(MiniAudio miniAudio, double q, double frequency) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), q, frequency);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new MiniAudioException("Error while creating notching filter node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, double q, double frequency);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_notch_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = ma_engine_get_channels(g_engine);
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_notch_node_config_init(channels, sampleRate, q, frequency);

        ma_notch_node* g_Node = (ma_notch_node*) ma_malloc(sizeof(ma_notch_node), NULL);
        ma_result result = ma_notch_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_notch_node* node = (ma_notch_node*) nodeAddress;
        ma_notch_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
