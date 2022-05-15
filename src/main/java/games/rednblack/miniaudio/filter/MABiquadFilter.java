package games.rednblack.miniaudio.filter;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;

/**
 * Wrapper class to MiniAudio's Biquad Filter
 *
 * @author fgnm
 */
public class MABiquadFilter extends MANode {

     /*JNI
        #include "miniaudio.h"
     */

    public MABiquadFilter(MiniAudio miniAudio, float b0, float b1, float b2, float a0, float a1, float a2) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), b0, b1, b2, a0, a1, a2);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new IllegalStateException("Error while creating biquad node, code " + address);
        }
    }

    private native long jniCreateNode(long graphAddress, float b0, float b1, float b2, float a0, float a1, float a2);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_biquad_node_config nodeConfig;
        ma_uint32 channels;

        channels   = ma_engine_get_channels(g_engine);

        nodeConfig = ma_biquad_node_config_init(channels, b0, b1, b2, a0, a1, a2);

        ma_biquad_node* g_Node = (ma_biquad_node*) ma_malloc(sizeof(ma_biquad_node), NULL);
        ma_result result = ma_biquad_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_biquad_node* node = (ma_biquad_node*) nodeAddress;
        ma_biquad_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
