/* Include ma_compressor_node.h after miniaudio.h */
#ifndef ma_compressor_node_h
#define ma_compressor_node_h

#include <math.h> /* For log10f, powf, fabsf, expf */

#ifdef __cplusplus
extern "C" {
#endif

/*
A compressor reduces the dynamic range of an audio signal.
This implementation supports side-chaining via a second input bus.
- Input 0: The main signal to be compressed.
- Input 1: The side-chain/control signal. If not connected, Input 0 is used.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    float threshold;  /* Threshold in decibels (dB). Default: -24.0 */
    float ratio;      /* Compression ratio. Default: 4.0 (for 4:1) */
    float attack;     /* Attack time in milliseconds. Default: 5.0 */
    float release;    /* Release time in milliseconds. Default: 100.0 */
    float makeupGain; /* Makeup gain in decibels (dB). Default: 0.0 */
} ma_compressor_node_config;

MA_API ma_compressor_node_config ma_compressor_node_config_init(ma_uint32 channels, ma_uint32 sampleRate);


typedef struct
{
    ma_node_base baseNode;
    ma_uint32 channels;

    /* Config parameters */
    float threshold;
    float ratio;
    float makeupGainLinear;
    ma_uint32 sampleRate;

    /* Internal state */
    float attackCoeff;
    float releaseCoeff;
    float envelope[MA_MAX_CHANNELS];
} ma_compressor_node;

MA_API ma_result ma_compressor_node_init(ma_node_graph* pNodeGraph, const ma_compressor_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_compressor_node* pCompressorNode);
MA_API void ma_compressor_node_uninit(ma_compressor_node* pCompressorNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_compressor_node_h */