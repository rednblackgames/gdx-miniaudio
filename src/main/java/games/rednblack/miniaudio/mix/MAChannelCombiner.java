package games.rednblack.miniaudio.mix;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Channel Combiner Node
 *
 * @author fgnm
 */
public class MAChannelCombiner extends MANode {
     /*JNI
        #include "miniaudio.h"
        #include "ma_channel_combiner_node.h"
     */

    public MAChannelCombiner(MiniAudio miniAudio, int channels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), channels);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new MiniAudioException("Error while creating channel combiner node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int channels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_channel_combiner_node_config nodeConfig;

        nodeConfig = ma_channel_combiner_node_config_init(channels);

        ma_channel_combiner_node* g_Node = (ma_channel_combiner_node*) ma_malloc(sizeof(ma_channel_combiner_node), NULL);
        ma_result result = ma_channel_combiner_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_channel_combiner_node* node = (ma_channel_combiner_node*) nodeAddress;
        ma_channel_combiner_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
