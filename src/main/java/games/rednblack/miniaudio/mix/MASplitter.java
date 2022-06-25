package games.rednblack.miniaudio.mix;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Splitter Node
 *
 * Make a copy of an audio stream for effect processing.
 * This takes has 1 input bus and splits the stream into 2 output buses.
 *
 * @author fgnm
 */
public class MASplitter extends MANode {
     /*JNI
        #include "miniaudio.h"
     */

    public MASplitter(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MASplitter(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new MiniAudioException("Error while creating splitter node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_splitter_node_config nodeConfig;
        ma_uint32 channels;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;

        nodeConfig = ma_splitter_node_config_init(channels);

        ma_splitter_node* g_Node = (ma_splitter_node*) ma_malloc(sizeof(ma_splitter_node), NULL);
        ma_result result = ma_splitter_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
        if (result != MA_SUCCESS) {
            ma_free(g_Node, NULL);
            return (jlong) result;
        }
        return (jlong) g_Node;
    */

    @Override
    public int getSupportedOutputs() {
        return 2;
    }

    @Override
    public void dispose() {
        jniDispose(address);
    }

    private native void jniDispose(long nodeAddress); /*
        ma_splitter_node* node = (ma_splitter_node*) nodeAddress;
        ma_splitter_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
