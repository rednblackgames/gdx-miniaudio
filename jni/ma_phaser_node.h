/* Include ma_phaser_node.h after miniaudio.h */
#ifndef ma_phaser_node_h
#define ma_phaser_node_h

#include <math.h> /* For tanf() and sinf() */

#ifdef __cplusplus
extern "C" {
#endif

/*
A phaser creates its effect using a series of all-pass filters. The LFO
modulates the center frequency of these filters to create a sweeping sound.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    ma_uint32 stages;       /* Number of all-pass filter stages (2 to 12 is typical). Default: 4 */
    float rate;             /* LFO rate in Hz. Default: 0.5 */
    float depth;            /* LFO depth (0.0 to 1.0). Default: 1.0 */
    float feedback;         /* Feedback level (-1.0 to 1.0). Default: 0.5 */
    float wet;              /* Wet signal mix level (0.0 to 1.0). Default: 0.5 */
    float dry;              /* Dry signal mix level (0.0 to 1.0). Default: 0.5 */
    float frequencyRangeMin;/* LFO sweep range start in Hz. Default: 440.0 */
    float frequencyRangeMax;/* LFO sweep range end in Hz. Default: 1600.0 */
} ma_phaser_node_config;

MA_API ma_phaser_node_config ma_phaser_node_config_init(ma_uint32 channels, ma_uint32 sampleRate);


typedef struct
{
    ma_node_base baseNode;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    ma_uint32 numStages;
    float rate;
    float depth;
    float feedback;
    float wet;
    float dry;
    float rangeMin;
    float rangeMax;

    /* Internal state */
    float lfoPhase;
    float* pFilterState; /* State for each stage and channel. Size = numStages * channels */
    float feedbackSample[MA_MAX_CHANNELS];
} ma_phaser_node;

MA_API ma_result ma_phaser_node_init(ma_node_graph* pNodeGraph, const ma_phaser_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_phaser_node* pPhaserNode);
MA_API void ma_phaser_node_uninit(ma_phaser_node* pPhaserNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_phaser_node_h */