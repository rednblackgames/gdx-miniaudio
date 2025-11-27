#include "ma_phaser_node.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

MA_API ma_phaser_node_config ma_phaser_node_config_init(ma_uint32 channels, ma_uint32 sampleRate)
{
    ma_phaser_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig       = ma_node_config_init();
    config.channels         = channels;
    config.sampleRate       = sampleRate;
    config.stages           = 4;
    config.rate             = 0.5f;
    config.depth            = 1.0f;
    config.feedback         = 0.5f;
    config.wet              = 0.5f;
    config.dry              = 0.5f;
    config.frequencyRangeMin = 440.0f;
    config.frequencyRangeMax = 1600.0f;

    return config;
}

static void ma_phaser_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_phaser_node* pPhaserNode = (ma_phaser_node*)pNode;
    ma_uint32 frameCountOut = *pFrameCountOut;
    ma_uint32 frameCountIn  = (pFrameCountIn != NULL) ? *pFrameCountIn : 0;
    const float* pFramesIn  = (ppFramesIn != NULL) ? ppFramesIn[0] : NULL;

    ma_uint32 iFrame, iChannel, iStage;

    const float lfo_increment = 2.0f * (float)M_PI * pPhaserNode->rate / (float)pPhaserNode->sampleRate;

    for (iFrame = 0; iFrame < frameCountOut; ++iFrame) {
        float lfo_raw = sinf(pPhaserNode->lfoPhase);
        float lfo_val = (lfo_raw + 1.0f) * 0.5f * pPhaserNode->depth;

        pPhaserNode->lfoPhase += lfo_increment;
        if (pPhaserNode->lfoPhase >= 2.0f * (float)M_PI) {
            pPhaserNode->lfoPhase -= 2.0f * (float)M_PI;
        }

        float center_freq = pPhaserNode->rangeMin + (pPhaserNode->rangeMax - pPhaserNode->rangeMin) * lfo_val;

        /* Safety clamp for frequency to prevent instability */
        if (center_freq > pPhaserNode->sampleRate * 0.49f) {
            center_freq = pPhaserNode->sampleRate * 0.49f;
        }

        float tan_val = tanf((float)M_PI * center_freq / (float)pPhaserNode->sampleRate);
        float alpha = (tan_val - 1.0f) / (tan_val + 1.0f);

        for (iChannel = 0; iChannel < pPhaserNode->channels; ++iChannel) {
            float in_sample = 0.0f;
            if (pFramesIn != NULL && iFrame < frameCountIn) {
                in_sample = pFramesIn[iFrame * pPhaserNode->channels + iChannel];
            }

            float filtered_input = in_sample + pPhaserNode->feedbackSample[iChannel] * pPhaserNode->feedback;

            for (iStage = 0; iStage < pPhaserNode->numStages; ++iStage) {
                ma_uint32 state_idx = iStage * pPhaserNode->channels + iChannel;

                float y = alpha * filtered_input + pPhaserNode->pFilterState[state_idx];
                pPhaserNode->pFilterState[state_idx] = filtered_input - alpha * y;
                filtered_input = y;
            }

            pPhaserNode->feedbackSample[iChannel] = filtered_input;

            ppFramesOut[0][iFrame * pPhaserNode->channels + iChannel] = in_sample * pPhaserNode->dry + filtered_input * pPhaserNode->wet;
        }
    }
}

static ma_node_vtable g_ma_phaser_node_vtable =
{
    ma_phaser_node_process_pcm_frames,
    NULL,
    1,  /* 1 input bus. */
    1,  /* 1 output bus. */
    MA_NODE_FLAG_CONTINUOUS_PROCESSING
};

MA_API ma_result ma_phaser_node_init(ma_node_graph* pNodeGraph, const ma_phaser_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_phaser_node* pPhaserNode)
{
    ma_result result;
    ma_node_config baseConfig;
    ma_uint32 filter_state_size;

    if (pPhaserNode == NULL || pConfig == NULL || pConfig->stages == 0) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pPhaserNode);

    pPhaserNode->channels   = pConfig->channels;
    pPhaserNode->sampleRate = pConfig->sampleRate;
    pPhaserNode->numStages  = pConfig->stages;
    pPhaserNode->rate       = pConfig->rate;
    pPhaserNode->depth      = pConfig->depth;
    pPhaserNode->feedback   = pConfig->feedback;
    pPhaserNode->wet        = pConfig->wet;
    pPhaserNode->dry        = pConfig->dry;
    pPhaserNode->rangeMin   = pConfig->frequencyRangeMin;
    pPhaserNode->rangeMax   = pConfig->frequencyRangeMax;
    pPhaserNode->lfoPhase   = 0.0f;

    filter_state_size = pPhaserNode->numStages * pPhaserNode->channels * sizeof(float);
    pPhaserNode->pFilterState = (float*)ma_malloc(filter_state_size, pAllocationCallbacks);
    if (pPhaserNode->pFilterState == NULL) {
        return MA_OUT_OF_MEMORY;
    }
    MA_ZERO_MEMORY(pPhaserNode->pFilterState, filter_state_size);
    MA_ZERO_MEMORY(pPhaserNode->feedbackSample, sizeof(pPhaserNode->feedbackSample));

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable          = &g_ma_phaser_node_vtable;
    baseConfig.pInputChannels  = &pConfig->channels;
    baseConfig.pOutputChannels = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pPhaserNode->baseNode);
    if (result != MA_SUCCESS) {
        ma_free(pPhaserNode->pFilterState, pAllocationCallbacks);
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_phaser_node_uninit(ma_phaser_node* pPhaserNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    if (pPhaserNode == NULL) {
        return;
    }

    ma_node_uninit(&pPhaserNode->baseNode, pAllocationCallbacks);
    ma_free(pPhaserNode->pFilterState, pAllocationCallbacks);
}