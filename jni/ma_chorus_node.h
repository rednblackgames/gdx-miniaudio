/* Include ma_chorus_node.h after miniaudio.h */
#ifndef ma_chorus_node_h
#define ma_chorus_node_h

#include <math.h> /* For sinf() */

#ifdef __cplusplus
extern "C" {
#endif

/*
The chorus node has one input and one output.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    float delay;    /* Base delay of the chorus in milliseconds. (Default: 25.0) */
    float rate;     /* LFO rate in Hz. (Default: 0.5) */
    float depth;    /* Modulation depth in milliseconds. (Default: 2.0) */
    float wet;      /* Wet signal mix level (0.0 to 1.0). (Default: 0.5) */
    float dry;      /* Dry signal mix level (0.0 to 1.0). (Default: 0.5) */
} ma_chorus_node_config;

MA_API ma_chorus_node_config ma_chorus_node_config_init(ma_uint32 channels, ma_uint32 sampleRate);


typedef struct
{
    ma_node_base baseNode;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    float delay;
    float rate;
    float depth;
    float wet;
    float dry;

    /* Internal state */
    float* pDelayBuffer;
    ma_uint32 delayBufferSizeInFrames;
    ma_uint32 writeIndex;
    float lfoPhase;
} ma_chorus_node;

MA_API ma_result ma_chorus_node_init(ma_node_graph* pNodeGraph, const ma_chorus_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_chorus_node* pChorusNode);
MA_API void ma_chorus_node_uninit(ma_chorus_node* pChorusNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_chorus_node_h */