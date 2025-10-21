/* Include ma_flanger_node.h after miniaudio.h */
#ifndef ma_flanger_node_h
#define ma_flanger_node_h

#include <math.h> /* For sinf() */

#ifdef __cplusplus
extern "C" {
#endif

/*
A flanger is a delay-based effect that uses an LFO to modulate a very short
delay line. It also includes a feedback path to create its characteristic
swooshing sound.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    float delay;    /* Base delay in milliseconds (typically 1-10ms). Default: 5.0 */
    float rate;     /* LFO rate in Hz. Default: 0.2 */
    float depth;    /* Modulation depth in milliseconds. Default: 2.0 */
    float feedback; /* Feedback level (-1.0 to 1.0). Default: 0.5 */
    float wet;      /* Wet signal mix level (0.0 to 1.0). Default: 0.5 */
    float dry;      /* Dry signal mix level (0.0 to 1.0). Default: 0.5 */
} ma_flanger_node_config;

MA_API ma_flanger_node_config ma_flanger_node_config_init(ma_uint32 channels, ma_uint32 sampleRate);


typedef struct
{
    ma_node_base baseNode;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    float delay;
    float rate;
    float depth;
    float feedback;
    float wet;
    float dry;

    /* Internal state */
    float* pDelayBuffer;
    ma_uint32 delayBufferSizeInFrames;
    ma_uint32 writeIndex;
    float lfoPhase;
} ma_flanger_node;

MA_API ma_result ma_flanger_node_init(ma_node_graph* pNodeGraph, const ma_flanger_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_flanger_node* pFlangerNode);
MA_API void ma_flanger_node_uninit(ma_flanger_node* pFlangerNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_flanger_node_h */