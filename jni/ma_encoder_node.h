/* Include ma_encoder_node.h after miniaudio.h */
#ifndef ma_encoder_node_h
#define ma_encoder_node_h

#ifdef __cplusplus
extern "C" {
#endif

/*
The encoder node has one input and one output.
*/
typedef struct
{
    ma_node_config nodeConfig;
    ma_format format;
    ma_uint32 channels;
    ma_uint32 sampleRate;
    const char* pFilePath;
} ma_encoder_node_config;

MA_API ma_encoder_node_config ma_encoder_node_config_init(ma_format format, ma_uint32 channels, ma_uint32 sampleRate, const char* pFilePath);


typedef struct
{
    ma_node_base baseNode;
    ma_encoder* encoder;
} ma_encoder_node;

MA_API ma_result ma_encoder_node_init(ma_node_graph* pNodeGraph, const ma_encoder_node_config* pConfig, const ma_allocation_callbacks* pAllocationCallbacks, ma_encoder_node* pEncoderNode);
MA_API void ma_encoder_node_uninit(ma_encoder_node* pEncoderNode, const ma_allocation_callbacks* pAllocationCallbacks);

#ifdef __cplusplus
}
#endif
#endif  /* ma_encoder_node_h */
