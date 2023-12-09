package games.rednblack.miniaudio.mix;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Encoder Node. Write WAV file with encoded PCM data
 *
 * @author fgnm
 */
public class MAEncoder extends MANode {
     /*JNI
        #include "miniaudio.h"
        #include "ma_encoder_node.h"
     */

    public MAEncoder(MiniAudio miniAudio, String filePath) {
        this(miniAudio, filePath, -1);
    }

    public MAEncoder(MiniAudio miniAudio, String filePath, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), filePath, customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating encoder node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, String filePath, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_encoder_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_encoder_node_config_init(ma_format_f32, channels, sampleRate, filePath);

        ma_encoder_node* g_Node = (ma_encoder_node*) ma_malloc(sizeof(ma_encoder_node), NULL);
        ma_result result = ma_encoder_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_encoder_node* node = (ma_encoder_node*) nodeAddress;
        ma_encoder_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
