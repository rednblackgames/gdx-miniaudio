/* Include ma_visualizer_node.h after miniaudio.h */
#ifndef ma_visualizer_node_h
#define ma_visualizer_node_h

#ifdef __cplusplus
extern "C" {
#endif

typedef void (* ma_visualizer_node_pcm_callback)(const float* pFrames, ma_uint32 frameCount, ma_uint32 channels, void* pUserData);

/*
The visualizer node has one input and one output.
Audio passes through unchanged while the callback receives a copy of the PCM data.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_uint32 channels;
    ma_visualizer_node_pcm_callback callback;
    void* pUserData;
} ma_visualizer_node_config;

MA_API ma_visualizer_node_config ma_visualizer_node_config_init(ma_uint32 channels, ma_visualizer_node_pcm_callback callback, void* pUserData);


typedef struct
{
    ma_node_base baseNode;
    ma_visualizer_node_pcm_callback callback;
    void* pUserData;
} ma_visualizer_node;

MA_API ma_result ma_visualizer_node_init(ma_node_graph* pNodeGraph, const ma_visualizer_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_visualizer_node* pVisualizerNode);
MA_API void ma_visualizer_node_uninit(ma_visualizer_node* pVisualizerNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_visualizer_node_h */
