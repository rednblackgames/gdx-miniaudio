#include "ma_chorus_node.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

MA_API ma_chorus_node_config ma_chorus_node_config_init(ma_uint32 channels, ma_uint32 sampleRate)
{
    ma_chorus_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig = ma_node_config_init();
    config.channels   = channels;
    config.sampleRate = sampleRate;
    config.delay      = 25.0f;
    config.rate       = 0.5f;
    config.depth      = 2.0f;
    config.wet        = 0.5f;
    config.dry        = 0.5f;

    return config;
}

static void ma_chorus_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_chorus_node* pChorusNode = (ma_chorus_node*)pNode;
    ma_uint32 frameCountOut = *pFrameCountOut;
    ma_uint32 frameCountIn  = (pFrameCountIn != NULL) ? *pFrameCountIn : 0;
    const float* pFramesIn  = (ppFramesIn != NULL) ? ppFramesIn[0] : NULL;

    ma_uint32 iFrame;
    ma_uint32 iChannel;

    const float lfo_increment = 2.0f * (float)M_PI * pChorusNode->rate / (float)pChorusNode->sampleRate;

    /* Calculate delay and depth in samples */
    float delay_samples = pChorusNode->delay * pChorusNode->sampleRate / 1000.0f;
    float depth_samples = pChorusNode->depth * pChorusNode->sampleRate / 1000.0f;

    for (iFrame = 0; iFrame < frameCountOut; ++iFrame) {
        /* Calculate LFO value */
        float lfo_val = sinf(pChorusNode->lfoPhase);
        pChorusNode->lfoPhase += lfo_increment;
        if (pChorusNode->lfoPhase >= 2.0f * (float)M_PI) {
            pChorusNode->lfoPhase -= 2.0f * (float)M_PI;
        }

        /* Calculate the modulated delay and read position */
        float modulated_delay = delay_samples + lfo_val * depth_samples;
        float read_pos_float = (float)pChorusNode->writeIndex - modulated_delay;

        while (read_pos_float < 0) {
            read_pos_float += pChorusNode->delayBufferSizeInFrames;
        }

        /* Linear interpolation for fractional delay */
        ma_uint32 read_pos_0 = (ma_uint32)read_pos_float;
        ma_uint32 read_pos_1 = (read_pos_0 + 1) % pChorusNode->delayBufferSizeInFrames;
        float frac = read_pos_float - read_pos_0;

        for (iChannel = 0; iChannel < pChorusNode->channels; ++iChannel) {
            float in_sample = 0.0f;
            if (pFramesIn != NULL && iFrame < frameCountIn) {
                in_sample = pFramesIn[iFrame * pChorusNode->channels + iChannel];
            }

            /* Write current input to delay buffer */
            pChorusNode->pDelayBuffer[pChorusNode->writeIndex * pChorusNode->channels + iChannel] = in_sample;

            /* Read and interpolate from delay buffer */
            float delayed_sample_0 = pChorusNode->pDelayBuffer[read_pos_0 * pChorusNode->channels + iChannel];
            float delayed_sample_1 = pChorusNode->pDelayBuffer[read_pos_1 * pChorusNode->channels + iChannel];
            float wet_sample = delayed_sample_0 * (1.0f - frac) + delayed_sample_1 * frac;

            /* Mix dry and wet signals */
            ppFramesOut[0][iFrame * pChorusNode->channels + iChannel] = in_sample * pChorusNode->dry + wet_sample * pChorusNode->wet;
        }

        pChorusNode->writeIndex = (pChorusNode->writeIndex + 1) % pChorusNode->delayBufferSizeInFrames;
    }
}

static ma_node_vtable g_ma_chorus_node_vtable =
{
    ma_chorus_node_process_pcm_frames,
    NULL,
    1,  /* 1 input bus. */
    1,  /* 1 output bus. */
    MA_NODE_FLAG_CONTINUOUS_PROCESSING /* Keep processing to ensure delay buffer is handled correctly. */
};

MA_API ma_result ma_chorus_node_init(ma_node_graph* pNodeGraph, const ma_chorus_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_chorus_node* pChorusNode)
{
    ma_result result;
    ma_node_config baseConfig;
    float max_delay_ms;
    ma_uint32 delay_buffer_size_in_frames;
    ma_uint32 delay_buffer_size_in_bytes;

    if (pChorusNode == NULL || pConfig == NULL) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pChorusNode);

    pChorusNode->channels   = pConfig->channels;
    pChorusNode->sampleRate = pConfig->sampleRate;
    pChorusNode->delay      = pConfig->delay;
    pChorusNode->rate       = pConfig->rate;
    pChorusNode->depth      = pConfig->depth;
    pChorusNode->wet        = pConfig->wet;
    pChorusNode->dry        = pConfig->dry;
    pChorusNode->writeIndex = 0;
    pChorusNode->lfoPhase   = 0.0f;

    /* Calculate the maximum required delay buffer size to avoid buffer overflows. Add a small safety margin. */
    max_delay_ms = pConfig->delay + pConfig->depth + 1.0f;
    delay_buffer_size_in_frames = (ma_uint32)((max_delay_ms / 1000.0f) * pConfig->sampleRate);
    if (delay_buffer_size_in_frames == 0) {
        delay_buffer_size_in_frames = 1;
    }

    pChorusNode->delayBufferSizeInFrames = delay_buffer_size_in_frames;
    delay_buffer_size_in_bytes = pChorusNode->delayBufferSizeInFrames * pConfig->channels * sizeof(float);

    pChorusNode->pDelayBuffer = (float*)ma_malloc(delay_buffer_size_in_bytes, pAllocationCallbacks);
    if (pChorusNode->pDelayBuffer == NULL) {
        return MA_OUT_OF_MEMORY;
    }
    MA_ZERO_MEMORY(pChorusNode->pDelayBuffer, delay_buffer_size_in_bytes);

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable          = &g_ma_chorus_node_vtable;
    baseConfig.pInputChannels  = &pConfig->channels;
    baseConfig.pOutputChannels = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pChorusNode->baseNode);
    if (result != MA_SUCCESS) {
        ma_free(pChorusNode->pDelayBuffer, pAllocationCallbacks);
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_chorus_node_uninit(ma_chorus_node* pChorusNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    if (pChorusNode == NULL) {
        return;
    }

    /* Uninitialize the base node first. */
    ma_node_uninit(&pChorusNode->baseNode, pAllocationCallbacks);

    /* Free the delay buffer. */
    ma_free(pChorusNode->pDelayBuffer, pAllocationCallbacks);
}