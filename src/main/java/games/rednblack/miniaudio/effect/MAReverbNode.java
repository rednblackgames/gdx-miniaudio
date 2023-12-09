package games.rednblack.miniaudio.effect;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Wrapper class to MiniAudio's Reverb Node
 *
 * @author fgnm
 */
public class MAReverbNode extends MANode {

    /*JNI
        #include "miniaudio.h"
        #include "ma_reverb_node.h"
     */

    public MAReverbNode(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MAReverbNode(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating reverb node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_reverb_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_reverb_node_config_init(channels, sampleRate);

        ma_reverb_node* g_Node = (ma_reverb_node*) ma_malloc(sizeof(ma_reverb_node), NULL);
        ma_result result = ma_reverb_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_reverb_node* node = (ma_reverb_node*) nodeAddress;
        ma_reverb_node_uninit(node, NULL);
        ma_free(node, NULL);
    */

    /**
     * Set the size of the room.
     *
     * @param roomSize between 0.0 and 1.0
     */
    public void setRoomSize(float roomSize) {
        jniSetRoomSize(address, roomSize);
    }

    private native void jniSetRoomSize(long address, float roomSize);/*
        ma_reverb_node* node = (ma_reverb_node*) address;
        verblib_set_room_size(&node->reverb, roomSize);
    */

    /**
     * Set the amount of damping.
     *
     * @param dumping between 0.0 and 1.0
     */
    public void setDumping(float dumping) {
        jniSetDumping(address, dumping);
    }

    private native void jniSetDumping(long address, float dumping);/*
        ma_reverb_node* node = (ma_reverb_node*) address;
        verblib_set_damping(&node->reverb, dumping);
    */

    /**
     * Set the stereo width of the reverb.
     *
     * @param width between 0.0 and 1.0
     */
    public void setWidth(float width) {
        jniSetWidth(address, width);
    }

    private native void jniSetWidth(long address, float width);/*
        ma_reverb_node* node = (ma_reverb_node*) address;
        verblib_set_width(&node->reverb, width);
    */

    /**
     * Set the volume of the wet signal.
     *
     * @param wet between 0.0 and 1.0
     */
    public void setWet(float wet) {
        jniSetWet(address, wet);
    }

    private native void jniSetWet(long address, float wet);/*
        ma_reverb_node* node = (ma_reverb_node*) address;
        verblib_set_wet(&node->reverb, wet);
    */

    /**
     * Set the volume of the dry signal.
     *
     * @param dry between 0.0 and 1.0
     */
    public void setDry(float dry) {
        jniDry(address, dry);
    }

    private native void jniDry(long address, float dry);/*
        ma_reverb_node* node = (ma_reverb_node*) address;
        verblib_set_dry(&node->reverb, dry);
    */

    /**
     * Set the mode of the reverb, where values below 0.5 mean normal and values above mean frozen.
     *
     * @param mode major of 0.5 set frozen.
     */
    public void setMode(float mode) {
        jniMode(address, mode);
    }

    private native void jniMode(long address, float mode);/*
        ma_reverb_node* node = (ma_reverb_node*) address;
        verblib_set_mode(&node->reverb, mode);
    */
}
