#include "ma_compressor_node.h"

MA_API ma_compressor_node_config ma_compressor_node_config_init(ma_uint32 channels, ma_uint32 sampleRate)
{
    ma_compressor_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig = ma_node_config_init();
    config.channels   = channels;
    config.sampleRate = sampleRate;
    config.threshold  = -24.0f;
    config.ratio      = 4.0f;
    config.attack     = 5.0f;
    config.release    = 100.0f;
    config.makeupGain = 0.0f;

    return config;
}

static void ma_compressor_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_compressor_node* pCompressor = (ma_compressor_node*)pNode;
    ma_uint32 frameCount = *pFrameCountOut;
    ma_uint32 iFrame, iChannel;

    const float* pMainFrames = ppFramesIn[0];
    /* If the side-chain input (bus 1) is not connected, use the main input (bus 0) as the control signal. */
    const float* pSidechainFrames = (ppFramesIn[1] != NULL) ? ppFramesIn[1] : ppFramesIn[0];

    for (iFrame = 0; iFrame < frameCount; ++iFrame) {
        for (iChannel = 0; iChannel < pCompressor->channels; ++iChannel) {
            float in_main = pMainFrames[iFrame * pCompressor->channels + iChannel];
            float in_sidechain = pSidechainFrames[iFrame * pCompressor->channels + iChannel];

            /* 1. Envelope Detection (RMS detection would be better, but peak is simpler and faster) */
            float rectified_input = fabsf(in_sidechain);

            if (rectified_input > pCompressor->envelope[iChannel]) {
                /* Attack phase */
                pCompressor->envelope[iChannel] = pCompressor->attackCoeff * pCompressor->envelope[iChannel] + (1.0f - pCompressor->attackCoeff) * rectified_input;
            } else {
                /* Release phase */
                pCompressor->envelope[iChannel] = pCompressor->releaseCoeff * pCompressor->envelope[iChannel] + (1.0f - pCompressor->releaseCoeff) * rectified_input;
            }

            /* 2. Convert envelope to dB */
            float envelope_dB = 20.0f * log10f(pCompressor->envelope[iChannel] + 1e-9f); /* Add epsilon to avoid log(0) */

            /* 3. Calculate Gain Reduction */
            float gain_reduction_dB = 0.0f;
            if (envelope_dB > pCompressor->threshold) {
                float overshoot = envelope_dB - pCompressor->threshold;
                gain_reduction_dB = overshoot * (1.0f - 1.0f / pCompressor->ratio);
            }

            /* 4. Convert gain reduction back to a linear multiplier */
            float gain_linear = powf(10.0f, -gain_reduction_dB / 20.0f);

            /* 5. Apply gain to the main signal and add makeup gain */
            float out_sample = in_main * gain_linear * pCompressor->makeupGainLinear;
            ppFramesOut[0][iFrame * pCompressor->channels + iChannel] = out_sample;
        }
    }
}

static ma_node_vtable g_ma_compressor_node_vtable =
{
    ma_compressor_node_process_pcm_frames,
    NULL,
    2,  /* 2 input buses (main and side-chain) */
    1,  /* 1 output bus */
    MA_NODE_FLAG_CONTINUOUS_PROCESSING
};

MA_API ma_result ma_compressor_node_init(ma_node_graph* pNodeGraph, const ma_compressor_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_compressor_node* pCompressorNode)
{
    ma_result result;
    ma_node_config baseConfig;
    ma_uint32 inputChannels[2];

    if (pCompressorNode == NULL || pConfig == NULL || pConfig->sampleRate == 0) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pCompressorNode);

    pCompressorNode->channels   = pConfig->channels;
    pCompressorNode->threshold  = pConfig->threshold;
    pCompressorNode->ratio      = (pConfig->ratio < 1.0f) ? 1.0f : pConfig->ratio;
    pCompressorNode->sampleRate = pConfig->sampleRate;

    /* Pre-calculate coefficients and linear values to save CPU in the process loop */
    double attack_seconds = pConfig->attack / 1000.0;
    double release_seconds = pConfig->release / 1000.0;
    pCompressorNode->attackCoeff  = (float)exp(-1.0 / (attack_seconds * pConfig->sampleRate));
    pCompressorNode->releaseCoeff = (float)exp(-1.0 / (release_seconds * pConfig->sampleRate));
    pCompressorNode->makeupGainLinear = powf(10.0f, pConfig->makeupGain / 20.0f);

    MA_ZERO_MEMORY(pCompressorNode->envelope, sizeof(pCompressorNode->envelope));

    /* This node has two inputs, both with the same channel count. */
    inputChannels[0] = pConfig->channels;
    inputChannels[1] = pConfig->channels;

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable            = &g_ma_compressor_node_vtable;
    baseConfig.inputBusCount     = 2;
    baseConfig.pInputChannels    = inputChannels;
    baseConfig.pOutputChannels   = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pCompressorNode->baseNode);
    if (result != MA_SUCCESS) {
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_compressor_node_uninit(ma_compressor_node* pCompressorNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    if (pCompressorNode == NULL) {
        return;
    }
    ma_node_uninit(&pCompressorNode->baseNode, pAllocationCallbacks);
}