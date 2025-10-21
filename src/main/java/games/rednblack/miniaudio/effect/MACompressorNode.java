package games.rednblack.miniaudio.effect;

import games.rednblack.miniaudio.MANode;
import games.rednblack.miniaudio.MAResult;
import games.rednblack.miniaudio.MiniAudio;
import games.rednblack.miniaudio.MiniAudioException;

/**
 * Represents a dynamic range compressor node within the audio graph.
 * <p>
 * A compressor reduces the volume of sounds that are louder than a certain
 * threshold, effectively reducing the dynamic range of a signal. This node
 * is particularly powerful because it supports both standard and side-chain
 * compression through its two-input bus system.
 *
 * How the Two Inputs Work
 * The compressor's behavior is determined by which input buses are connected:
 * <ul>
 * <li><strong>Input Bus 0 (Main Signal):</strong> This is the primary audio signal
 * that will actually be compressed (i.e., have its volume reduced).
 * Examples: background music, ambient sound effects.</li>
 * <li><strong>Input Bus 1 (Side-chain / Control Signal):</strong> This is the
 * "trigger" signal. The compressor <em>listens</em> to the volume of this
 * input to decide when and how much to compress the signal on Input Bus 0.
 * Examples: dialogue, narration, a kick drum beat.</li>
 * </ul>
 *
 * Modes of Operation
 * <ol>
 * <li><strong>Standard Compression:</strong> If you only connect a node to
 * <strong>Input Bus 0</strong>, the compressor works in a standard mode.
 * It listens to its own input signal and reduces its volume whenever it
 * gets too loud.</li>
 * <li><strong>Side-chain Compression (Ducking):</strong> When you connect nodes
 * to <strong>both Input Bus 0 and Input Bus 1</strong>, you activate
 * side-chaining. The compressor will listen to the volume of the signal on
 * Bus 1, but apply the volume reduction to the signal on Bus 0. This is
 * perfect for automatically lowering music volume when someone speaks
 * (a technique called "ducking").</li>
 * </ol>
 *
 * Side-chain Ducking Example
 * The following example demonstrates how to make background music "duck"
 * (lower in volume) whenever a dialogue sound is playing.
 *
 * <pre>{@code
 * // --- Setup Nodes ---
 * // The sound that will be compressed (the music)
 * MASound backgroundMusic = new MASound("path/to/music.wav");
 *
 * // The sound that will trigger the compression (the dialogue)
 * MASound dialogueSound = new MASound("path/to/dialogue.wav");
 *
 * // The compressor node that will apply the effect
 * MACompressorNode compressorNode = new MACompressorNode(miniAudio);
 *
 * // A splitter is needed to send the dialogue to two places:
 * // 1. To the compressor's side-chain input (to trigger it)
 * // 2. To the final output (so we can actually hear it)
 * MASplitter splitter = new MASplitter(miniAudio);
 *
 * // --- Routing the Audio Graph ---
 * // 1. Route the dialogue into the splitter
 * splitter.attachToThisNode(dialogueSound, 0);
 *
 * // 2. Route the music to the compressor's MAIN input (Bus 0)
 * compressorNode.attachToThisNode(backgroundMusic, 0, 0);
 *
 * // 3. Route the dialogue from the splitter to the compressor's SIDE-CHAIN input (Bus 1)
 * compressorNode.attachToThisNode(splitter, 0, 1);
 *
 * // --- Final Output ---
 * // 4. The compressed music is sent to the engine output so we can hear it
 * miniAudio.attachToEngineOutput(compressorNode, 0);
 *
 * // 5. The original, untouched dialogue is also sent to the engine output
 * miniAudio.attachToEngineOutput(splitter, 1);
 * }</pre>
 *
 * @author fgnm
 */
public class MACompressorNode extends MANode {
     /*JNI
        #include "miniaudio.h"
        #include "ma_compressor_node.h"
     */

    public MACompressorNode(MiniAudio miniAudio) {
        this(miniAudio, -1);
    }

    public MACompressorNode(MiniAudio miniAudio, int customChannels) {
        super(miniAudio);

        address = jniCreateNode(miniAudio.getEngineAddress(), customChannels);

        if (MAResult.checkErrors(address)) {
            throw new MiniAudioException("Error while creating compressor node", (int) address);
        }
    }

    private native long jniCreateNode(long graphAddress, int customChannels);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        ma_compressor_node_config nodeConfig;
        ma_uint32 channels;
        ma_uint32 sampleRate;

        channels   = customChannels == -1 ? ma_engine_get_channels(g_engine) : customChannels;
        sampleRate = ma_engine_get_sample_rate(g_engine);

        nodeConfig = ma_compressor_node_config_init(channels, sampleRate);

        ma_compressor_node* g_Node = (ma_compressor_node*) ma_malloc(sizeof(ma_compressor_node), NULL);
        ma_result result = ma_compressor_node_init(ma_engine_get_node_graph(g_engine), &nodeConfig, NULL, g_Node);
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
        ma_compressor_node* node = (ma_compressor_node*) nodeAddress;
        ma_compressor_node_uninit(node, NULL);
        ma_free(node, NULL);
    */

    /**
     * Threshold in decibels (dB). Default: -24.0
     *
     * @param threshold in decibels (dB)
     */
    public void setThreshold(float threshold) {
        jniSetThreshold(address, threshold);
    }

    private native void jniSetThreshold(long address, float threshold);/*
        ma_compressor_node* node = (ma_compressor_node*) address;
        node->threshold = threshold;
    */

    /**
     * Compression ratio. Default: 4.0 (for 4:1)
     *
     * @param ratio 4.0 (for 4:1)
     */
    public void setRatio(float ratio) {
        jniSetRatio(address, Math.max(ratio, 1.0f));
    }

    private native void jniSetRatio(long address, float ratio);/*
        ma_compressor_node* node = (ma_compressor_node*) address;
        node->ratio = ratio;
    */

    /**
     * Attack time in milliseconds. Default: 5.0
     *
     * @param attack in milliseconds
     */
    public void setAttack(float attack) {
        jniSetAttack(address, attack);
    }

    private native void jniSetAttack(long address, float attack);/*
        ma_compressor_node* node = (ma_compressor_node*) address;

        double attack_seconds = attack / 1000.0;
        node->attackCoeff  = (float)exp(-1.0 / (attack_seconds * node->sampleRate));
    */

    /**
     * Release time in milliseconds. Default: 100.0
     *
     * @param release in milliseconds
     */
    public void setRelease(float release) {
        jniSetRelease(address, release);
    }

    private native void jniSetRelease(long address, float release);/*
        ma_compressor_node* node = (ma_compressor_node*) address;
        double release_seconds = release / 1000.0;
        node->releaseCoeff = (float)exp(-1.0 / (release_seconds * node->sampleRate));
    */

    /**
     * Makeup gain in decibels (dB). Default: 0.0
     *
     * @param makeupGain in decibels (dB)
     */
    public void setMakeupGain(float makeupGain) {
        jniSetMakeupGain(address, makeupGain);
    }

    private native void jniSetMakeupGain(long address, float makeupGain);/*
        ma_compressor_node* node = (ma_compressor_node*) address;
        node->makeupGainLinear = powf(10.0f, makeupGain / 20.0f);
    */
}
