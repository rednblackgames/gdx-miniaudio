package games.rednblack.miniaudio;

/**
 * Wrapper class to MiniAudio's Delay Node
 *
 * @author fgnm
 */
public class MADelayNode extends MANode {
    /*JNI
        #include "miniaudio.h"
     */

    public MADelayNode(MiniAudio miniAudio, float delay, float decay) {
        super(miniAudio);

        address = jniCreateDelayNode(miniAudio.getEngineAddress(), delay, decay);

        if (address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR) {
            throw new IllegalStateException("Error while creating delay node, code " + address);
        }
    }

    @Override
    public int getSupportedOutputs() {
        return 1;
    }

    private native long jniCreateDelayNode(long graphAddress, float delay, float decay);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_delay_node_config delayNodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = ma_engine_get_channels(g_engine);
        sampleRate = ma_engine_get_sample_rate(g_engine);

        delayNodeConfig = ma_delay_node_config_init(channels, sampleRate, (ma_uint32)(sampleRate * delay), decay);

        ma_delay_node* g_delayNode = (ma_delay_node*) ma_malloc(sizeof(ma_delay_node), NULL);
        ma_result result = ma_delay_node_init(ma_engine_get_node_graph(g_engine), &delayNodeConfig, NULL, g_delayNode);
        if (result != MA_SUCCESS) {
            ma_free(g_delayNode, NULL);
            return (jlong) result;
        }
        return (jlong) g_delayNode;
    */

    @Override
    public void attachToNode(MANode previousNode, int outputBusIndex) {
        if (outputBusIndex >= previousNode.getSupportedOutputs())
            throw new IllegalArgumentException("Wrong output bus number, the node support up to " + previousNode.getSupportedOutputs() + " buses.");

        miniAudio.attachOutputBus(previousNode, outputBusIndex, this, 0);
    }

    @Override
    public void dispose() {
        jniDispose(address);
    }

    private native void jniDispose(long nodeAddress); /*
        ma_delay_node* node = (ma_delay_node*) nodeAddress;
        ma_delay_node_uninit(node, NULL);
        ma_free(node, NULL);
    */
}
