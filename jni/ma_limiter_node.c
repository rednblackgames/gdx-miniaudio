#include "ma_limiter_node.h"

MA_API ma_limiter_node_config ma_limiter_node_config_init(ma_uint32 channels, ma_uint32 sampleRate)
{
    ma_limiter_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig  = ma_node_config_init();
    config.channels    = channels;
    config.sampleRate  = sampleRate;
    config.ceilingDb   = -1.0f;
    config.attackMs    = 1.0f;
    config.releaseMs   = 100.0f;
    config.inputGainDb = 0.0f;

    return config;
}

static void ma_limiter_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_limiter_node* pLimiter = (ma_limiter_node*)pNode;
    ma_uint32 frameCount = *pFrameCountOut;
    ma_uint32 iFrame, iChannel;

    for (iFrame = 0; iFrame < frameCount; ++iFrame) {
        /* 1. Apply input gain and find the absolute peak across all channels for this frame */
        float peak = 0.0f;
        for (iChannel = 0; iChannel < pLimiter->channels; ++iChannel) {
            float in_sample = ppFramesIn[0][iFrame * pLimiter->channels + iChannel];
            float amplified_sample = in_sample * pLimiter->inputGainLinear;

            // Store the amplified sample to be used later
            ppFramesOut[0][iFrame * pLimiter->channels + iChannel] = amplified_sample;

            float abs_sample = fabsf(amplified_sample);
            if (abs_sample > peak) {
                peak = abs_sample;
            }
        }

        /* 2. Update the envelope based on the detected peak */
        if (peak > pLimiter->envelope) {
            /* Attack phase */
            pLimiter->envelope = pLimiter->attackCoeff * pLimiter->envelope + (1.0f - pLimiter->attackCoeff) * peak;
        } else {
            /* Release phase */
            pLimiter->envelope = pLimiter->releaseCoeff * pLimiter->envelope;
        }

        /* 3. Calculate gain reduction if envelope exceeds the ceiling */
        float gain = 1.0f;
        if (pLimiter->envelope > pLimiter->ceilingLinear) {
            gain = pLimiter->ceilingLinear / pLimiter->envelope;
        }

        /* 4. Apply the calculated gain to all channels for this frame */
        for (iChannel = 0; iChannel < pLimiter->channels; ++iChannel) {
            ppFramesOut[0][iFrame * pLimiter->channels + iChannel] *= gain;
        }
    }
}

static ma_node_vtable g_ma_limiter_node_vtable =
{
    ma_limiter_node_process_pcm_frames,
    NULL,
    1,  /* 1 input bus */
    1,  /* 1 output bus */
    MA_NODE_FLAG_CONTINUOUS_PROCESSING
};

MA_API ma_result ma_limiter_node_init(ma_node_graph* pNodeGraph, const ma_limiter_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_limiter_node* pLimiterNode)
{
    ma_result result;
    ma_node_config baseConfig;

    if (pLimiterNode == NULL || pConfig == NULL || pConfig->sampleRate == 0) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pLimiterNode);

    pLimiterNode->channels = pConfig->channels;
    pLimiterNode->sampleRate = pConfig->sampleRate;

    /* Pre-calculate coefficients and linear values to save CPU in the process loop */
    pLimiterNode->ceilingLinear = powf(10.0f, pConfig->ceilingDb / 20.0f);
    pLimiterNode->inputGainLinear = powf(10.0f, pConfig->inputGainDb / 20.0f);

    double attack_seconds = pConfig->attackMs / 1000.0;
    double release_seconds = pConfig->releaseMs / 1000.0;
    pLimiterNode->attackCoeff  = (float)exp(-1.0 / (attack_seconds * pConfig->sampleRate));
    pLimiterNode->releaseCoeff = (float)exp(-1.0 / (release_seconds * pConfig->sampleRate));

    pLimiterNode->envelope = 0.0f;

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable          = &g_ma_limiter_node_vtable;
    baseConfig.pInputChannels  = &pConfig->channels;
    baseConfig.pOutputChannels = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pLimiterNode->baseNode);
    if (result != MA_SUCCESS) {
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_limiter_node_uninit(ma_limiter_node* pLimiterNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    if (pLimiterNode == NULL) {
        return;
    }
    ma_node_uninit(&pLimiterNode->baseNode, pAllocationCallbacks);
}