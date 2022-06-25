package games.rednblack.miniaudio.filter;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Peaking Filter
 *
 * @author fgnm
 */
public class MAPeakingFilter extends MANode {

     /*JNI
        #include "miniaudio.h"
     */

    public MAPeakingFilter(MiniAudio miniAudio, double gainDB, double q, double frequency) {
        this(miniAudio, gainDB, q, frequency, -1);
    }

    public MAPeakingFilter(MiniAudio miniAudio, double gainDB, double q, double frequency, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), gainDB, q, frequency, customChannels);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new MiniAudioException("Error while creating peaking filter node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, double gainDB, double q, double frequency, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_peak_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_peak_node_config_init(channels, sampleRate, gainDB, q, frequency);

        ma_peak_node* g_Node = (ma_peak_node*) ma_malloc(sizeof(ma_peak_node), NULL);
        ma_result result = ma_peak_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_peak_node* node = (ma_peak_node*) nodeAddress;
        ma_peak_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}

