package games.rednblack.miniaudio.mix;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's ltrim Node. Trims any leading silence with a threshold
 *
 * @author fgnm
 */
public class MALTrim extends MANode {
     /*JNI
        #include "miniaudio.h"
        #include "ma_ltrim_node.h"
     */

    public MALTrim(MiniAudio miniAudio, float threshold) {
        this(miniAudio, threshold, -1);
    }

    public MALTrim(MiniAudio miniAudio, float threshold, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), threshold, customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating ltrim node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, float threshold, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_ltrim_node_config nodeConfig;
        ma_uint32 channels;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;

        nodeConfig = ma_ltrim_node_config_init(channels, threshold);

        ma_ltrim_node* g_Node = (ma_ltrim_node*) ma_malloc(sizeof(ma_ltrim_node), NULL);
        ma_result result = ma_ltrim_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_ltrim_node* node = (ma_ltrim_node*) nodeAddress;
        ma_ltrim_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
