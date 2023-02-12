#include "ma_encoder_node.h"

MA_API ma_encoder_node_config ma_encoder_node_config_init(ma_format format, ma_uint32 channels, ma_uint32 sampleRate, const char* pFilePath)
{
    ma_encoder_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig = ma_node_config_init();  /* Input and output channels will be set in ma_encoder_node_init(). */
    config.format     = format;
    config.channels   = channels;
    config.sampleRate = sampleRate;
    config.pFilePath  = pFilePath;

    return config;
}

static void ma_encoder_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_encoder_node* pEncoderNode = (ma_encoder_node*)pNode;
    ma_encoder_write_pcm_frames(pEncoderNode->encoder, *ppFramesIn, *pFrameCountIn, NULL);
}

static ma_node_vtable g_ma_encoder_node_vtable =
{
    ma_encoder_node_process_pcm_frames,
    NULL,
    1,  /* 1 input channel. */
    1,  /* 1 output channel. */
    MA_NODE_FLAG_PASSTHROUGH
};

MA_API ma_result ma_encoder_node_init(ma_node_graph* pNodeGraph, const ma_encoder_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_encoder_node* pEncoderNode)
{
    ma_result result;
    ma_node_config baseConfig;

    if (pEncoderNode == NULL) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pEncoderNode);

    if (pConfig == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_encoder_config config = ma_encoder_config_init(ma_encoding_format_wav, pConfig->format, pConfig->channels, pConfig->sampleRate);
    ma_encoder* encoder = (ma_encoder*) ma_malloc(sizeof(ma_encoder), NULL);
    result = ma_encoder_init_file(pConfig->pFilePath, &config, encoder);
    if (result != MA_SUCCESS) {
        return result;
    }
    pEncoderNode->encoder = encoder;

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable          = &g_ma_encoder_node_vtable;
    baseConfig.pInputChannels  = &pConfig->channels;
    baseConfig.pOutputChannels = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pEncoderNode->baseNode);
    if (result != MA_SUCCESS) {
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_encoder_node_uninit(ma_encoder_node* pEncoderNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    /* The base node is always uninitialized first. */
    ma_node_uninit(pEncoderNode, pAllocationCallbacks);
    ma_encoder_uninit(pEncoderNode->encoder);
    ma_free(pEncoderNode->encoder, NULL);
}
