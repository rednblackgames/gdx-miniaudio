#include "ma_visualizer_node.h"

MA_API ma_visualizer_node_config ma_visualizer_node_config_init(ma_uint32 channels, ma_visualizer_node_pcm_callback callback, void* pUserData)
{
    ma_visualizer_node_config config;

    MA_ZERO_OBJECT(&config);
    config.nodeConfig = ma_node_config_init();
    config.channels   = channels;
    config.callback   = callback;
    config.pUserData  = pUserData;

    return config;
}

static void ma_visualizer_node_process_pcm_frames(ma_node* pNode, const float** ppFramesIn, ma_uint32* pFrameCountIn, float** ppFramesOut, ma_uint32* pFrameCountOut)
{
    ma_visualizer_node* pVisualizerNode = (ma_visualizer_node*)pNode;
    ma_uint32 channels = ma_node_get_input_channels(pNode, 0);

    if (pVisualizerNode->callback != NULL) {
        pVisualizerNode->callback(*ppFramesIn, *pFrameCountIn, channels, pVisualizerNode->pUserData);
    }
}

static ma_node_vtable g_ma_visualizer_node_vtable =
{
    ma_visualizer_node_process_pcm_frames,
    NULL,
    1,  /* 1 input bus. */
    1,  /* 1 output bus. */
    MA_NODE_FLAG_PASSTHROUGH
};

MA_API ma_result ma_visualizer_node_init(ma_node_graph* pNodeGraph, const ma_visualizer_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_visualizer_node* pVisualizerNode)
{
    ma_result result;
    ma_node_config baseConfig;

    if (pVisualizerNode == NULL) {
        return MA_INVALID_ARGS;
    }

    MA_ZERO_OBJECT(pVisualizerNode);

    if (pConfig == NULL) {
        return MA_INVALID_ARGS;
    }

    pVisualizerNode->callback  = pConfig->callback;
    pVisualizerNode->pUserData = pConfig->pUserData;

    baseConfig = pConfig->nodeConfig;
    baseConfig.vtable          = &g_ma_visualizer_node_vtable;
    baseConfig.pInputChannels  = &pConfig->channels;
    baseConfig.pOutputChannels = &pConfig->channels;

    result = ma_node_init(pNodeGraph, &baseConfig, pAllocationCallbacks, &pVisualizerNode->baseNode);
    if (result != MA_SUCCESS) {
        return result;
    }

    return MA_SUCCESS;
}

MA_API void ma_visualizer_node_uninit(ma_visualizer_node* pVisualizerNode, const ma_allocation_callbacks* pAllocationCallbacks)
{
    ma_node_uninit(pVisualizerNode, pAllocationCallbacks);
}
