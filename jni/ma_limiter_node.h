/* Include ma_limiter_node.h after miniaudio.h */
#ifndef ma_limiter_node_h
#define ma_limiter_node_h

#include <math.h> /* For powf, fabsf, expf */

#ifdef __cplusplus
extern "C" {
#endif

/*
A peak limiter acts as a "brick wall", preventing an audio signal from
exceeding a set ceiling level. It's used to prevent digital clipping
and increase the overall loudness of a signal.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    float ceilingDb;    /* The maximum output level in decibels (dB). Default: -1.0 */
    float attackMs;     /* Attack time in milliseconds. Default: 1.0 */
    float releaseMs;    /* Release time in milliseconds. Default: 100.0 */
    float inputGainDb;  /* Gain applied to the input signal in dB. Default: 0.0 */
} ma_limiter_node_config;

MA_API ma_limiter_node_config ma_limiter_node_config_init(ma_uint32 channels, ma_uint32 sampleRate);


typedef struct
{
    ma_node_base baseNode;
    ma_uint32 channels;

    /* Config parameters converted for processing */
    float ceilingLinear;
    float inputGainLinear;
    ma_uint32 sampleRate;

    /* Internal state */
    float attackCoeff;
    float releaseCoeff;
    float envelope; /* A single envelope for all channels to maintain stereo image */
} ma_limiter_node;

MA_API ma_result ma_limiter_node_init(ma_node_graph* pNodeGraph, const ma_limiter_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_limiter_node* pLimiterNode);
MA_API void ma_limiter_node_uninit(ma_limiter_node* pLimiterNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_limiter_node_h */