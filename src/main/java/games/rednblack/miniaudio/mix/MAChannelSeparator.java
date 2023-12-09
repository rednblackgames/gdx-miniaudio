package games.rednblack.miniaudio.mix;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Channel Separator Node
 *
 * @author fgnm
 */
public class MAChannelSeparator extends MANode {
     /*JNI
        #include "miniaudio.h"
        #include "ma_channel_separator_node.h"
     */

    private final int outputChannels;

    public MAChannelSeparator(MiniAudio miniAudio, int channels) {
        super(miniAudio);

        outputChannels = channels;
        address = jniCreateNode(miniAudio.getEngineAddress(), channels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating channel separator node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int channels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_channel_separator_node_config nodeConfig;

        nodeConfig = ma_channel_separator_node_config_init(channels);

        ma_channel_separator_node* g_Node = (ma_channel_separator_node*) ma_malloc(sizeof(ma_channel_separator_node), NULL);
        ma_result result = ma_channel_separator_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
        if (result != MA_SUCCESS) {
            ma_free(g_Node, NULL);
            return (jlong) result;
        }
        return (jlong) g_Node;
    */

    @Override
    public int getSupportedOutputs() {
        return outputChannels;
    }

    @Override
    public void dispose() {
        jniDispose(address);
    }

    private native void jniDispose(long nodeAddress); /*
        ma_channel_separator_node* node = (ma_channel_separator_node*) nodeAddress;
        ma_channel_separator_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
