package games.rednblack.miniaudio.mix;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MAVisualizerListener;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * A passthrough audio node that provides real-time PCM data to a {@link MAVisualizerListener}.
 * Audio passes through unchanged while the listener receives a copy of the PCM data.
 *
 * @author fgnm
 */
public class MAVisualizerNode extends MANode {
    /*JNI
        #include "miniaudio.h"
        #include "ma_visualizer_node.h"
        extern void visualizer_pcm_callback_jni(const float* pFrames, ma_uint32 frameCount, ma_uint32 channels, void* pUserData);
    */

    private MAVisualizerListener listener;
    private boolean disposed = false;

    public MAVisualizerNode(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MAVisualizerNode(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating visualizer node", (int) address);
        }

        miniAudio.registerVisualizerNode(this);
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_uint32 channels;

        channels = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;

        ma_visualizer_node* g_Node = (ma_visualizer_node*) ma_malloc(sizeof(ma_visualizer_node), NULL);
        ma_visualizer_node_config nodeConfig = ma_visualizer_node_config_init(channels, visualizer_pcm_callback_jni, g_Node);

        ma_result result = ma_visualizer_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
        if (result != MA_SUCCESS) {
            ma_free(g_Node, NULL);
            return (jlong) result;
        }
        return (jlong) g_Node;
    */

    /**
     * Set a listener to receive real-time PCM data from this node.
     *
     * @param listener the listener, or null to stop receiving data
     */
    public void setListener(MAVisualizerListener listener) {
        this.listener = listener;
    }

    /**
     * Get the current listener.
     *
     * @return the current listener, or null
     */
    public MAVisualizerListener getListener() {
        return listener;
    }

    /**
     * Called by MiniAudio dispatch when PCM data arrives from the native side.
     */
    public void deliverPcmData(float[] pcmData, int totalSamples, int channels) {
        if (disposed) return;
        MAVisualizerListener l = listener;
        if (l != null) {
            l.onVisualizerData(pcmData, totalSamples, channels);
        }
    }

    @Override
    public int getSupportedOutputs() {
        return 1;
    }

    @Override
    public void dispose() {
        disposed = true;
        miniAudio.unregisterVisualizerNode(this);
        jniDispose(address);
    }

    private native void jniDispose(long nodeAddress); /*
        ma_visualizer_node* node = (ma_visualizer_node*) nodeAddress;
        node->callback = NULL;
        ma_visualizer_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
