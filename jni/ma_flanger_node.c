#include "ma_flanger_node.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

MA_API ma_flanger_node_config ma_flanger_node_config_init(ma_uint32 channels, ma_uint32 sampleRate)
{
    ma_flanger_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig = ma_node_config_init();
    config.channels   = channels;
    config.sampleRate = sampleRate;
    config.delay      = 5.0f;
    config.rate       = 0.2f;
    config.depth      = 2.0f;
    config.feedback   = 0.5f;
    config.wet        = 0.5f;
    config.dry        = 0.5f;

    return config;
}

static void ma_flanger_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_flanger_node* pFlangerNode = (ma_flanger_node*)pNode;
    ma_uint32 frameCountOut = *pFrameCountOut;
    ma_uint32 frameCountIn  = (pFrameCountIn != NULL) ? *pFrameCountIn : 0;
    const float* pFramesIn  = (ppFramesIn != NULL) ? ppFramesIn[0] : NULL;

    ma_uint32 iFrame, iChannel;

    const float lfo_increment = 2.0f * (float)M_PI * pFlangerNode->rate / (float)pFlangerNode->sampleRate;

    float delay_samples = pFlangerNode->delay * pFlangerNode->sampleRate / 1000.0f;
    float depth_samples = pFlangerNode->depth * pFlangerNode->sampleRate / 1000.0f;

    for (iFrame = 0; iFrame < frameCountOut; ++iFrame) {
        float lfo_val = sinf(pFlangerNode->lfoPhase);
        pFlangerNode->lfoPhase += lfo_increment;
        if (pFlangerNode->lfoPhase >= 2.0f * (float)M_PI) {
            pFlangerNode->lfoPhase -= 2.0f * (float)M_PI;
        }

        float modulated_delay = delay_samples + lfo_val * depth_samples;
        float read_pos_float = (float)pFlangerNode->writeIndex - modulated_delay;

        while (read_pos_float < 0) {
            read_pos_float += pFlangerNode->delayBufferSizeInFrames;
        }

        ma_uint32 read_pos_0 = (ma_uint32)read_pos_float;
        ma_uint32 read_pos_1 = (read_pos_0 + 1) % pFlangerNode->delayBufferSizeInFrames;
        float frac = read_pos_float - read_pos_0;

        for (iChannel = 0; iChannel < pFlangerNode->channels; ++iChannel) {
            float in_sample = 0.0f;
            if (pFramesIn != NULL && iFrame < frameCountIn) {
                in_sample = pFramesIn[iFrame * pFlangerNode->channels + iChannel];
            }

            /* Read and interpolate from delay buffer to get the delayed signal */
            float delayed_sample_0 = pFlangerNode->pDelayBuffer[read_pos_0 * pFlangerNode->channels + iChannel];
            float delayed_sample_1 = pFlangerNode->pDelayBuffer[read_pos_1 * pFlangerNode->channels + iChannel];
            float wet_sample = delayed_sample_0 * (1.0f - frac) + delayed_sample_1 * frac;

            /* Create the feedback loop: input + feedback * delayed_signal */
            float feedback_sample = in_sample + wet_sample * pFlangerNode->feedback;

            /* Write the result back into the delay buffer */
            pFlangerNode->pDelayBuffer[pFlangerNode->writeIndex * pFlangerNode->channels + iChannel] = feedback_sample;

            /* Mix the final output */
            ppFramesOut[0][iFrame * pFlangerNode->channels + iChannel] = in_sample * pFlangerNode->dry + wet_sample * pFlangerNode->wet;
        }

        pFlangerNode->writeIndex = (pFlangerNode->writeIndex + 1) % pFlangerNode->delayBufferSizeInFrames;
    }
}

static ma_node_vtable g_ma_flanger_node_vtable =
{
    ma_flanger_node_process_pcm_frames,
    NULL,
    1,  /* 1 input bus. */
    1,  /* 1 output bus. */
    MA_NODE_FLAG_CONTINUOUS_PROCESSING
};

MA_API ma_result ma_flanger_node_init(ma_node_graph* pNodeGraph, const ma_flanger_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_flanger_node* pFlangerNode)
{
    ma_result result;
    ma_node_config baseConfig;
    float max_delay_ms;
    ma_uint32 delay_buffer_size_in_frames;
    ma_uint32 delay_buffer_size_in_bytes;

    if (pFlangerNode == NULL || pConfig == NULL) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pFlangerNode);

    pFlangerNode->channels   = pConfig->channels;
    pFlangerNode->sampleRate = pConfig->sampleRate;
    pFlangerNode->delay      = pConfig->delay;
    pFlangerNode->rate       = pConfig->rate;
    pFlangerNode->depth      = pConfig->depth;
    pFlangerNode->wet        = pConfig->wet;
    pFlangerNode->dry        = pConfig->dry;
    pFlangerNode->writeIndex = 0;
    pFlangerNode->lfoPhase   = 0.0f;

    pFlangerNode->feedback = pConfig->feedback;
    if (pFlangerNode->feedback > 0.99f) pFlangerNode->feedback = 0.99f;
    if (pFlangerNode->feedback < -0.99f) pFlangerNode->feedback = -0.99f;
    /* Calculate the max delay buffer size, adding a margin for safety */
    max_delay_ms = pConfig->delay + pConfig->depth + 1.0f;
    delay_buffer_size_in_frames = (ma_uint32)((max_delay_ms / 1000.0f) * pConfig->sampleRate);
    if (delay_buffer_size_in_frames < 2) { /* Need at least 2 for interpolation */
        delay_buffer_size_in_frames = 2;
    }

    pFlangerNode->delayBufferSizeInFrames = delay_buffer_size_in_frames;
    delay_buffer_size_in_bytes = pFlangerNode->delayBufferSizeInFrames * pConfig->channels * sizeof(float);

    pFlangerNode->pDelayBuffer = (float*)ma_malloc(delay_buffer_size_in_bytes, pAllocationCallbacks);
    if (pFlangerNode->pDelayBuffer == NULL) {
        return MA_OUT_OF_MEMORY;
    }
    /* Correctly zeroing the memory for the buffer. Thanks for the tip! */
    MA_ZERO_MEMORY(pFlangerNode->pDelayBuffer, delay_buffer_size_in_bytes);

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable          = &g_ma_flanger_node_vtable;
    baseConfig.pInputChannels  = &pConfig->channels;
    baseConfig.pOutputChannels = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pFlangerNode->baseNode);
    if (result != MA_SUCCESS) {
        ma_free(pFlangerNode->pDelayBuffer, pAllocationCallbacks);
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_flanger_node_uninit(ma_flanger_node* pFlangerNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    if (pFlangerNode == NULL) {
        return;
    }

    ma_node_uninit(&pFlangerNode->baseNode, pAllocationCallbacks);
    ma_free(pFlangerNode->pDelayBuffer, pAllocationCallbacks);
}