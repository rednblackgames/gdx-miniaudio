package games.rednblack.miniaudio;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import games.rednblack.miniaudio.config.MAContextConfiguration;
import games.rednblack.miniaudio.config.MAEngineConfiguration;

/**
 * Main Audio Engine interface that handle calls with native library.
 *
 * @author fgnm
 */
public class MiniAudio implements Disposable {
    public static final String TAG = "GdxMiniAudio";

    static {
        new SharedLibraryLoader().load("gdx-miniaudio");
    }

    public static final int MA_ENGINE_MAX_LISTENERS = 4;
    public static final String MA_ANDROID_STORAGE_EXTERNAL_PREFIX = "external:";

    /*JNI
        #define STB_VORBIS_HEADER_ONLY
        #include "stb_vorbis.c"

        #define MA_AAUDIO_MIN_ANDROID_SDK_VERSION 31
        #define MINIAUDIO_IMPLEMENTATION
        #include "miniaudio.h"

        #include "ma_reverb_node.c"
        #include "ma_channel_separator_node.c"
        #include "ma_channel_combiner_node.c"
        #include "ma_ltrim_node.c"
        #include "ma_encoder_node.c"
        #include "ma_chorus_node.c"
        #include "ma_flanger_node.c"
        #include "ma_phaser_node.c"
        #include "ma_compressor_node.c"

        #include <stdio.h>
        #include <queue.h>

        #ifdef MA_ANDROID
        #include <android/asset_manager_jni.h>
        #include <android/log.h>
        #include "miniaudio_android_assets.h"
        ma_android_vfs* androidVFS;
        static jobject assetManagerGlobalRef;
        #endif

        #ifndef MA_ANDROID_LOG_TAG
        #define MA_ANDROID_LOG_TAG  "miniaudio"
        #endif

        ma_context context;
        ma_device_config deviceConfig;
        ma_device device;
        ma_engine engine;
        ma_log maLog;
        ma_log_callback pLogCallback;
        ma_audio_buffer_ref inputBufferData;

        #ifdef MA_APPLE_MOBILE
        #include <string>
        std::string getBundlePath(const char* fileName) {
            NSString* fileNameString = [NSString stringWithUTF8String: fileName];
            NSString* filePath = [[NSBundle mainBundle] pathForResource:fileNameString ofType:nil];
            if (filePath != nil) {
                return [filePath UTF8String];
            } else {
                return "";
            }
        }
        #endif

        void data_callback(ma_device* pDevice, void* pOutput, const void* pInput, ma_uint32 frameCount) {
            ma_audio_buffer_ref_set_data(&inputBufferData, pInput, frameCount);
            ma_engine_read_pcm_frames(&engine, pOutput, frameCount, NULL);
        }

        static JavaVM* jvm = 0;
        static jobject jMiniAudio;
        static jmethodID jon_native_sound_end;
        static jmethodID jon_native_log;
        static jmethodID jon_native_notification;
        static LockFreeQueue* lock_free_queue;
        static ma_thread* callback_thread;
        static int running_callback_thread = 1;

        // Helper macro for platform-specific thread attachment
        #ifdef MA_ANDROID
        #define THREAD_ATTACH_MACRO jvm->AttachCurrentThread(&env, NULL);
        #else
        #define THREAD_ATTACH_MACRO jvm->AttachCurrentThread((void**)&env, NULL);
        #endif

        // Attaches a valid JNIEnv instance for the current thread.
        #define ATTACH_ENV()                                                     \
            bool _hadToAttach = false;                                           \
            JNIEnv* env;                                                         \
            if (jvm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_EDETACHED) {   \
                THREAD_ATTACH_MACRO                                              \
                _hadToAttach = true;                                             \
            }

        // Detaches JNIEnv instance.
        #define DETACH_ENV()                \
            if (_hadToAttach) {             \
                jvm->DetachCurrentThread(); \
            }

        void sound_end_callback(void* pUserData, ma_sound* pSound) {
            Event* event = (Event*) ma_malloc(sizeof(Event), NULL);
            event->type = 0;
            event->sound = pSound;
            while (enqueue(lock_free_queue, event) == -1) {
                ma_sleep(100); // 0.1 seconds
            }
        }

        void ma_log_callback_jni(void* pUserData, ma_uint32 level, const char* pMessage) {
            if (pMessage == NULL) {
                return;
            }

            size_t len = strlen(pMessage);
            size_t new_len = len;
            if (len > 0 && pMessage[len - 1] == '\n') {
                new_len = len - 1;
            }

            Event* event = (Event*) ma_malloc(sizeof(Event), NULL);
            if (event == NULL) {
                return;
            }

            event->message = (char*) ma_malloc(new_len + 1, NULL);
            if (event->message == NULL) {
                ma_free(event, NULL);
                return;
            }

            memcpy(event->message, pMessage, new_len);
            event->message[new_len] = '\0';
            event->type = 1;
            event->level = level;

            while (enqueue(lock_free_queue, event) == -1) {
                ma_sleep(100); // 0.1 seconds
            }
        }

        void notification_callback_jni(const ma_device_notification* pNotification) {
            Event* event = (Event*) ma_malloc(sizeof(Event), NULL);
            event->type = 2;
            event->notificationType = pNotification->type;
            while (enqueue(lock_free_queue, event) == -1) {
                ma_sleep(100); // 0.1 seconds
            }
        }

        static ma_thread_result MA_THREADCALL post_event_to_jni(void* pUserData) {
            ATTACH_ENV()
            Event* event;
            while (running_callback_thread) {
                if (dequeue(lock_free_queue, &event) == 0) {
                    if (event->type == 0) {
                        env->CallVoidMethod(jMiniAudio, jon_native_sound_end, (jlong) event->sound);
                    } else if (event->type == 1) {
                        jstring javaMessage = env->NewStringUTF(event->message);
                        env->CallVoidMethod(jMiniAudio, jon_native_log, event->level, javaMessage);
                        env->DeleteLocalRef(javaMessage);
                        free(event->message);
                    } else if (event->type == 2) {
                        env->CallVoidMethod(jMiniAudio, jon_native_notification, event->notificationType);
                    }
                    ma_free(event, NULL);
                } else {
                    // Queue is empty, wait a bit
                    ma_sleep(100); // 0.1 seconds
                }
            }

            //Drain queue before exit
            while (dequeue(lock_free_queue, &event) == 0) {
                if (event->type == 0) {
                    env->CallVoidMethod(jMiniAudio, jon_native_sound_end, (jlong) event->sound);
                } else if (event->type == 1) {
                    jstring javaMessage = env->NewStringUTF(event->message);
                    env->CallVoidMethod(jMiniAudio, jon_native_log, event->level, javaMessage);
                    env->DeleteLocalRef(javaMessage);
                    free(event->message);
                } else if (event->type == 2) {
                    env->CallVoidMethod(jMiniAudio, jon_native_notification, event->notificationType);
                }
                ma_free(event, NULL);
            }

            env->DeleteGlobalRef(jMiniAudio);
            DETACH_ENV()
            ma_free(lock_free_queue, NULL);
            ma_free(callback_thread, NULL);
            return (ma_thread_result)0;
        }
     */

    private long engineAddress = 0;
    private final MASound endCallbackSound;
    private MASoundEndListener endListener;
    private MALogCallback logCallback;
    private MADeviceNotificationListener deviceNotificationListener;

    /**
     * Create a new MiniAudio Engine Instance
     *
     */
    public MiniAudio() {
        this(new MAContextConfiguration(), new MAEngineConfiguration());
    }

    /**
     * Create a new MiniAudio Engine Instance
     *
     * @param contextConfiguration configuration for the audio context
     * @param engineConfiguration configuration for engine,
     *                            or null if more customization is needed before engine initialization
     */
    public MiniAudio(MAContextConfiguration contextConfiguration, @Null MAEngineConfiguration engineConfiguration) {
        if (contextConfiguration == null)
            throw new IllegalArgumentException("contextConfiguration cannot be null");

        this.logCallback = contextConfiguration.logCallback;

        int result = jniInitContext(contextConfiguration.iOSSessionCategory.code, contextConfiguration.iOSSessionCategoryOptions, contextConfiguration.androidUseAAudio);
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Unable to init MiniAudio Context", result);
        }

        if (engineConfiguration != null)
            initEngine(engineConfiguration);

        endCallbackSound = new MASound(this);
    }

    /**
     * Initialize MiniAudio Engine
     *
     * @param engineConfiguration configuration for engine
     */
    public void initEngine(MAEngineConfiguration engineConfiguration) {
        if (engineAddress != 0) throw new IllegalStateException("A MiniAudio Engine is already initialized.");

        if (engineConfiguration == null)
            throw new IllegalArgumentException("engineConfiguration cannot be null.");

        if (engineConfiguration.listenerCount < 1 || engineConfiguration.listenerCount > MA_ENGINE_MAX_LISTENERS)
            throw new IllegalArgumentException("Listeners must be between 1 and MA_ENGINE_MAX_LISTENERS");

        int result = jniInitEngine(
                engineConfiguration.listenerCount,
                engineConfiguration.playbackId,
                engineConfiguration.captureId,
                engineConfiguration.channels,
                engineConfiguration.bufferPeriodMillis,
                engineConfiguration.bufferPeriodFrames,
                engineConfiguration.sampleRate,
                engineConfiguration.formatType.code,
                engineConfiguration.fullDuplex,
                engineConfiguration.exclusive,
                engineConfiguration.lowLatency
        );
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Unable to init MiniAudio Engine", result);
        }
        engineAddress = jniEngineAddress();
    }

    private native int jniInitContext(int iOSSessionCategory, short iOSSessionCategoryOptions, boolean enableAAudioBackend);/*
        env->GetJavaVM(&jvm);
        jMiniAudio = env->NewGlobalRef(object);
        jclass handlerClass = env->GetObjectClass(jMiniAudio);
        jon_native_sound_end = env->GetMethodID(handlerClass, "on_native_sound_end", "(J)V");
        jon_native_log = env->GetMethodID(handlerClass, "on_native_log", "(ILjava/lang/String;)V");
        jon_native_notification = env->GetMethodID(handlerClass, "on_native_notification", "(I)V");

        lock_free_queue = (LockFreeQueue*) ma_malloc(sizeof(LockFreeQueue), NULL);
        init_queue(lock_free_queue);
        callback_thread = (ma_thread*) ma_malloc(sizeof(ma_thread), NULL);
        running_callback_thread = 1;
        ma_result thread_res = ma_thread_create(callback_thread, ma_thread_priority_normal, 0, post_event_to_jni, NULL, NULL);
        if (thread_res != MA_SUCCESS) return thread_res;

        ma_log_init(NULL, &maLog);
        pLogCallback = ma_log_callback_init(ma_log_callback_jni, NULL);
        ma_log_register_callback(&maLog, pLogCallback);

        ma_context_config config = ma_context_config_init();
        config.pLog = &maLog;
        config.coreaudio.sessionCategory = (ma_ios_session_category) iOSSessionCategory;
        config.coreaudio.sessionCategoryOptions = iOSSessionCategoryOptions;

        ma_backend fullBackends[] = {
            ma_backend_wasapi,
            ma_backend_dsound,
            ma_backend_winmm,
            ma_backend_coreaudio,
            ma_backend_sndio,
            ma_backend_audio4,
            ma_backend_oss,
            ma_backend_pulseaudio,
            ma_backend_alsa,
            ma_backend_jack,
            ma_backend_aaudio,
            ma_backend_opensl,
            ma_backend_webaudio,
            ma_backend_null
        };

        ma_backend noAAudioBackends[] = {
            ma_backend_wasapi,
            ma_backend_dsound,
            ma_backend_winmm,
            ma_backend_coreaudio,
            ma_backend_sndio,
            ma_backend_audio4,
            ma_backend_oss,
            ma_backend_pulseaudio,
            ma_backend_alsa,
            ma_backend_jack,
            ma_backend_opensl,
            ma_backend_webaudio,
            ma_backend_null
        };

        ma_result res = enableAAudioBackend ?
                            ma_context_init(fullBackends, sizeof(fullBackends) / sizeof(fullBackends[0]), &config, &context)
                            :
                            ma_context_init(noAAudioBackends, sizeof(noAAudioBackends) / sizeof(noAAudioBackends[0]), &config, &context);
        if (res != MA_SUCCESS) return res;

        return MA_SUCCESS;
    */

    /**
     * Enumerate every device attached to the device with their capabilities,
     * check devices before {@link #initEngine(MAEngineConfiguration)}
     *
     * @return array of devices information
     */
    public MADeviceInfo[] enumerateDevices() {
        return jniEnumerateDevices(MADeviceInfo.class, MADeviceInfo.MADeviceNativeDataFormat.class);
    }

    private native MADeviceInfo[] jniEnumerateDevices(Class infoClass, Class nativeFormatClass);/*
        ma_device_info* pPlaybackInfos;
        ma_uint32 playbackCount;
        ma_device_info* pCaptureInfos;
        ma_uint32 captureCount;

        ma_result res = ma_context_get_devices(&context, &pPlaybackInfos, &playbackCount, &pCaptureInfos, &captureCount);
        if (res != MA_SUCCESS) return NULL;

        jclass formatTypeClass = env->FindClass("games/rednblack/miniaudio/MAFormatType");

        jmethodID constructor = env->GetMethodID(infoClass, "<init>", "()V");
        jmethodID constructorFormat = env->GetMethodID(nativeFormatClass, "<init>", "()V");

        jfieldID fFormatUNKNOWN = env->GetStaticFieldID(formatTypeClass, "UNKNOWN", "Lgames/rednblack/miniaudio/MAFormatType;");
        jfieldID fFormatU8 = env->GetStaticFieldID(formatTypeClass, "U8", "Lgames/rednblack/miniaudio/MAFormatType;");
        jfieldID fFormatS16 = env->GetStaticFieldID(formatTypeClass, "S16", "Lgames/rednblack/miniaudio/MAFormatType;");
        jfieldID fFormatS24 = env->GetStaticFieldID(formatTypeClass, "S24", "Lgames/rednblack/miniaudio/MAFormatType;");
        jfieldID fFormatS32 = env->GetStaticFieldID(formatTypeClass, "S32", "Lgames/rednblack/miniaudio/MAFormatType;");
        jfieldID fFormatF32 = env->GetStaticFieldID(formatTypeClass, "F32", "Lgames/rednblack/miniaudio/MAFormatType;");

        jfieldID fName = env->GetFieldID(infoClass, "name", "Ljava/lang/String;");
        jfieldID fIsCapture = env->GetFieldID(infoClass, "isCapture", "Z");
        jfieldID fIdAddress = env->GetFieldID(infoClass, "idAddress", "J");
        jfieldID fIsDefault = env->GetFieldID(infoClass, "isDefault", "Z");
        jfieldID fNativeDataFormats = env->GetFieldID(infoClass, "nativeDataFormats", "[Lgames/rednblack/miniaudio/MADeviceInfo$MADeviceNativeDataFormat;");

        jfieldID fFormat =  env->GetFieldID(nativeFormatClass, "format", "Lgames/rednblack/miniaudio/MAFormatType;");
        jfieldID fChannels = env->GetFieldID(nativeFormatClass, "channels", "I");
        jfieldID fSampleRate = env->GetFieldID(nativeFormatClass, "sampleRate", "I");
        jfieldID fFlags = env->GetFieldID(nativeFormatClass, "flags", "I");

        jobjectArray ret = env->NewObjectArray(playbackCount + captureCount, infoClass, NULL);

        if (ret) {
            ma_uint32 iDevice = 0;
            for (; iDevice < playbackCount; iDevice += 1) {
                jobject obj = env->NewObject(infoClass, constructor);
                if(obj) {
                    ma_context_get_device_info(&context, ma_device_type_playback, &pPlaybackInfos[iDevice].id, &pPlaybackInfos[iDevice]);
                    env->SetBooleanField(obj, fIsCapture, 0);
                    env->SetLongField(obj, fIdAddress, (jlong) &pPlaybackInfos[iDevice].id);
                    env->SetBooleanField(obj, fIsDefault, pPlaybackInfos[iDevice].isDefault);
                    jstring buffer = env->NewStringUTF(pPlaybackInfos[iDevice].name);
                    env->SetObjectField(obj, fName, buffer);

                    jobjectArray formats = env->NewObjectArray(pPlaybackInfos[iDevice].nativeDataFormatCount, nativeFormatClass, NULL);
                    if (formats) {
                        for (ma_uint32 i = 0; i < pPlaybackInfos[iDevice].nativeDataFormatCount; i++) {
                            jobject objInfo = env->NewObject(nativeFormatClass, constructorFormat);

                            if (objInfo) {
                                switch(pPlaybackInfos[iDevice].nativeDataFormats[i].format) {
                                    case ma_format_unknown:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatUNKNOWN));
                                        break;
                                    case ma_format_u8:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatU8));
                                        break;
                                    case ma_format_s16:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatS16));
                                        break;
                                    case ma_format_s24:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatS24));
                                        break;
                                    case ma_format_s32:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatS32));
                                        break;
                                    case ma_format_f32:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatF32));
                                        break;
                                    default:
                                        break;
                                }
                                env->SetIntField(objInfo, fChannels, pPlaybackInfos[iDevice].nativeDataFormats[i].channels);
                                env->SetIntField(objInfo, fSampleRate, pPlaybackInfos[iDevice].nativeDataFormats[i].sampleRate);
                                env->SetIntField(objInfo, fFlags, pPlaybackInfos[iDevice].nativeDataFormats[i].flags);

                                env->SetObjectArrayElement(formats, i, objInfo);
                            }
                        }
                        env->SetObjectField(obj, fNativeDataFormats, formats);
                    }

                    env->SetObjectArrayElement(ret, iDevice, obj);
                }
            }

            for (ma_uint32 i2 = 0; i2 < captureCount; i2 += 1, iDevice += 1) {
                jobject obj = env->NewObject(infoClass, constructor);
                if(obj) {
                    ma_context_get_device_info(&context, ma_device_type_capture, &pCaptureInfos[i2].id, &pCaptureInfos[i2]);

                    env->SetBooleanField(obj, fIsCapture, 1);
                    env->SetLongField(obj, fIdAddress, (jlong) &pCaptureInfos[i2].id);
                    env->SetBooleanField(obj, fIsDefault, pCaptureInfos[i2].isDefault);
                    jstring buffer = env->NewStringUTF(pCaptureInfos[i2].name);
                    env->SetObjectField(obj, fName, buffer);

                    jobjectArray formats = env->NewObjectArray(pCaptureInfos[i2].nativeDataFormatCount, nativeFormatClass, NULL);
                    if (formats) {
                        for (ma_uint32 i = 0; i < pCaptureInfos[i2].nativeDataFormatCount; i++) {
                            jobject objInfo = env->NewObject(nativeFormatClass, constructorFormat);

                            if (objInfo) {
                                switch(pCaptureInfos[i2].nativeDataFormats[i].format) {
                                    case ma_format_unknown:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatUNKNOWN));
                                        break;
                                    case ma_format_u8:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatU8));
                                        break;
                                    case ma_format_s16:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatS16));
                                        break;
                                    case ma_format_s24:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatS24));
                                        break;
                                    case ma_format_s32:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatS32));
                                        break;
                                    case ma_format_f32:
                                        env->SetObjectField(objInfo, fFormat, env->GetStaticObjectField(formatTypeClass, fFormatF32));
                                        break;
                                    default:
                                        break;
                                }
                                env->SetIntField(objInfo, fChannels, pCaptureInfos[i2].nativeDataFormats[i].channels);
                                env->SetIntField(objInfo, fSampleRate, pCaptureInfos[i2].nativeDataFormats[i].sampleRate);
                                env->SetIntField(objInfo, fFlags, pCaptureInfos[i2].nativeDataFormats[i].flags);

                                env->SetObjectArrayElement(formats, i, objInfo);
                            }
                        }
                        env->SetObjectField(obj, fNativeDataFormats, formats);
                    }

                    env->SetObjectArrayElement(ret, iDevice, obj);
                }
            }
        }

        return ret;
    */

    private native int jniInitEngine(int listenerCount, long playbackId, long captureId, int channels, int bufferPeriodMillis, int bufferPeriodFrames, int sampleRate, int format, boolean fullDuplex, boolean exclusive, boolean lowLatency);/*
        ma_result res;
        if (fullDuplex)
            deviceConfig = ma_device_config_init(ma_device_type_duplex);
        else
            deviceConfig = ma_device_config_init(ma_device_type_playback);
        deviceConfig.capture.pDeviceID  = playbackId == -1 ? NULL : (ma_device_id*) playbackId;
        deviceConfig.capture.format     = (ma_format) format;
        deviceConfig.capture.channels   = channels;
        deviceConfig.capture.shareMode  = exclusive ? ma_share_mode_exclusive : ma_share_mode_shared;
        deviceConfig.playback.pDeviceID = captureId == -1 ? NULL : (ma_device_id*) captureId;
        deviceConfig.playback.format    = (ma_format) format;
        deviceConfig.playback.channels  = channels;
        deviceConfig.sampleRate         = sampleRate;
        deviceConfig.dataCallback       = data_callback;
        deviceConfig.notificationCallback = notification_callback_jni;
        deviceConfig.performanceProfile = lowLatency ? ma_performance_profile_low_latency : ma_performance_profile_conservative;
        deviceConfig.periodSizeInFrames = bufferPeriodFrames;
        deviceConfig.periodSizeInMilliseconds = bufferPeriodMillis;
        deviceConfig.wasapi.noAutoConvertSRC = true;
        deviceConfig.coreaudio.allowNominalSampleRateChange = true;
        res = ma_device_init(&context, &deviceConfig, &device);
        if (res != MA_SUCCESS) return res;

        res = ma_audio_buffer_ref_init(device.capture.format, device.capture.channels, NULL, 0, &inputBufferData);
        if (res != MA_SUCCESS) return res;

        ma_engine_config engineConfig = ma_engine_config_init();
        engineConfig.listenerCount = listenerCount;
        engineConfig.channels = channels;
        engineConfig.pDevice = &device;

        #if defined(MA_ANDROID)
        androidVFS = (ma_android_vfs*) ma_malloc(sizeof(ma_android_vfs), NULL);
        res = ma_android_vfs_init(androidVFS, NULL);
        androidVFS->pLog = &maLog;
        if (res != MA_SUCCESS) return res;
        engineConfig.pResourceManagerVFS = androidVFS;
        #endif

		return ma_engine_init(&engineConfig, &engine);
	*/

    private native long jniEngineAddress();/*
        return (jlong) &engine;
    */

    /**
     * Get engine native address
     *
     * @return the engine native address
     */
    public long getEngineAddress() {
        return engineAddress;
    }

    /**
     * Android native implementation needs the AssetManager reference from Java code.
     * Call this only in Android Applications and passing `Context#getAssets()` object.
     *
     * @param assetManager Android's Native AssetManager
     */
    public void setupAndroid(Object assetManager) {
        jniSetupAndroid(assetManager);
    }

    private native void jniSetupAndroid(Object assetManager);/*
        #if defined(MA_ANDROID)
        assetManagerGlobalRef = env->NewGlobalRef(assetManager);
        androidVFS->asset_manager = AAssetManager_fromJava(env, assetManager);
        #endif
    */

    /**
     * Emergency recovery function. AAudio backend appears to be buggy on many Android versions.
     * When the device fails to restart calling this function will likely bring back engine to a working state.
     * <a href="https://github.com/rednblackgames/gdx-miniaudio/issues/1">...</a>
     */
    public void resetAudioDevice() {
        int result = jniResetAudioDevice();
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Unable to reset Audio device", result);
        }
    }

    private native int jniResetAudioDevice();/*
        ma_device_uninit(&device);
        return ma_device_init(&context, &deviceConfig, &device);
    */

    /**
     * Dispose Engine resources
     */
    @Override
    public void dispose() {
        jniDispose();
    }

    private native void jniDispose();/*
        ma_device_uninit(&device);
        ma_engine_uninit(&engine);
        ma_context_uninit(&context);
        ma_log_unregister_callback(&maLog, pLogCallback);
        ma_log_uninit(&maLog);
        #if defined(MA_ANDROID)
        ma_free(androidVFS, NULL);
        env->DeleteGlobalRef(assetManagerGlobalRef);
        #endif
        running_callback_thread = 0;
    */

    /**
     * Set engine in play mode. Does nothing if Engine is already started.
     */
    public void startEngine() {
        int result = jniStartEngine();
        //If a generic error occurred during engine start try to recover the state without crashing by resetting audio device
        if (result == MAResult.MA_ERROR) {
            resetAudioDevice();
            result = jniStartEngine();
        }

        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Unable to start MiniAudio Engine", result);
        }
    }

    private native int jniStartEngine();/*
        ma_result res = ma_engine_start(&engine);
        if (res != MA_SUCCESS) return res;
        return MA_SUCCESS;
    */

    /**
     * Stop audio processing and pause the Engine. Does nothing if Engine is already paused.
     */
    public void stopEngine() {
        int result = jniStopEngine();
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Unable to stop MiniAudio Engine", result);
        }
    }

    private native int jniStopEngine();/*
        ma_result res = ma_engine_stop(&engine);
        if (res != MA_SUCCESS) return res;
        return MA_SUCCESS;
    */

    /**
     * Set master volume of the engine.
     *
     * @param volume float value where 0 is silence and 1 the base volume.
     */
    public void setMasterVolume(float volume) {
        int result = jniSetMasterVolume(volume);
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Unable to set MiniAudio master volume", result);
        }
    }

    private native int jniSetMasterVolume(float volume);/*
        ma_result res = ma_engine_set_volume(&engine, volume);
        if (res != MA_SUCCESS) return res;
        return MA_SUCCESS;
    */

    /**
     * Used for 3D Spatialization, set the position of the current listener in world coordinates.
     *
     * @param x position
     * @param y position
     * @param z position
     */
    public void setListenerPosition(float x, float y, float z) {
        setListenerPosition(0, x, y, z);
    }

    /**
     * Used for 3D Spatialization, set the position of the current listener in world coordinates.
     *
     * @param listenerIndex index of the listener
     * @param x position
     * @param y position
     * @param z position
     */
    public void setListenerPosition(int listenerIndex, float x, float y, float z) {
        jniSetListenerPosition(listenerIndex, x, y, z);
    }

    private native void jniSetListenerPosition(int listenerIndex, float x, float y, float z);/*
        ma_engine_listener_set_position(&engine, listenerIndex, x, y, z);
    */

    /**
     * The direction of the listener represents it's forward vector.
     *
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setListenerDirection(float forwardX, float forwardY, float forwardZ) {
        setListenerDirection(0, forwardX, forwardY, forwardZ);
    }

    /**
     * The direction of the listener represents it's forward vector.
     *
     * @param listenerIndex index of the listener
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setListenerDirection(int listenerIndex, float forwardX, float forwardY, float forwardZ) {
        jniSetListenerDirection(listenerIndex, forwardX, forwardY, forwardZ);
    }

    private native void jniSetListenerDirection(int listenerIndex, float forwardX, float forwardY, float forwardZ);/*
        ma_engine_listener_set_direction(&engine, listenerIndex, forwardX, forwardY, forwardZ);
    */

    /**
     * The listener's up vector can also be specified and defaults to +1 on the Y axis.
     * Default 0, 1, 0.
     *
     * @param x normal
     * @param y normal
     * @param z normal
     */
    public void setListenerWorldUp(float x, float y, float z) {
        setListenerWorldUp(0, x, y, z);
    }

    /**
     * The listener's up vector can also be specified and defaults to +1 on the Y axis.
     * Default 0, 1, 0.
     *
     * @param listenerIndex index of the listener
     * @param x normal
     * @param y normal
     * @param z normal
     */
    public void setListenerWorldUp(int listenerIndex, float x, float y, float z) {
        jniSetListenerWorldUp(listenerIndex, x, y, z);
    }

    private native void jniSetListenerWorldUp(int listenerIndex, float x, float y, float z);/*
        ma_engine_listener_set_world_up(&engine, listenerIndex, x, y, z);
    */

    public void setListenerVelocity(float x, float y, float z) {
        setListenerVelocity(0, x, y, z);
    }

    /**
     * The listener's velocity vector can also be specified to be used with spatialization
     * for the Doppler effect.
     * Default 0, 0, 0.
     *
     * @param listenerIndex index of the listener
     * @param x velocity
     * @param y velocity
     * @param z velocity
     */
    public void setListenerVelocity(int listenerIndex, float x, float y, float z) {
        jniSetListenerVelocity(listenerIndex, x, y, z);
    }

    private native void jniSetListenerVelocity(int listenerIndex, float x, float y, float z);/*
        ma_engine_listener_set_velocity(&engine, listenerIndex, x, y, z);
    */

    /**
     * The engine supports directional attenuation. The listener can have a cone the controls how sound is
     * attenuated based on the listener's direction. When a sound is between the inner and outer cones, it
     * will be attenuated between 1 and the cone's outer gain
     *
     * @param innerAngleInRadians inner angle in radiance
     * @param outerAngleInRadians outer angle in radiance
     * @param outerGain outer gain
     */
    public void setListenerCone(float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        setListenerCone(0, innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    /**
     * The engine supports directional attenuation. The listener can have a cone the controls how sound is
     * attenuated based on the listener's direction. When a sound is between the inner and outer cones, it
     * will be attenuated between 1 and the cone's outer gain
     *
     * @param listenerIndex index of the listener
     * @param innerAngleInRadians inner angle in radiance
     * @param outerAngleInRadians outer angle in radiance
     * @param outerGain outer gain
     */
    public void setListenerCone(int listenerIndex, float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        jniSetListenerCone(listenerIndex, innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    private native void jniSetListenerCone(int listenerIndex, float innerAngleInRadians, float outerAngleInRadians, float outerGain);/*
        ma_engine_listener_set_cone(&engine, listenerIndex, innerAngleInRadians, outerAngleInRadians, outerGain);
    */

    /**
     * Play sound with "fire and forget"
     *
     * @param fileName path of the file relative to assets folder
     * @return status check {@link MAResult}
     */
    public int playSound(String fileName) {
        return playSound(fileName, false);
    }

    /**
     * Play sound with "fire and forget"
     *
     * @param fileName path of the file relative to assets folder
     * @param external true if file needs to be read outside the `assets` directory (only for Android and iOS)
     * @return status check {@link MAResult}
     */
    public int playSound(String fileName, boolean external) {
        if (external && Gdx.app.getType() == Application.ApplicationType.Android)
            fileName = MA_ANDROID_STORAGE_EXTERNAL_PREFIX + fileName;
        return jniPlaySound(fileName, external);
    }

    private native int jniPlaySound(String fileName, boolean external);/*
        #if defined(MA_APPLE_MOBILE)
        std::string cppStr = getBundlePath(fileName);
        char* cFileName = const_cast<char*>(cppStr.c_str());
        return ma_engine_play_sound(&engine, external ? fileName : cFileName, NULL);
        #else
        return ma_engine_play_sound(&engine, fileName, NULL);
        #endif
    */

    /**
     * Load a new {@link MASound} object and upload sound data into memory. Each {@link MASound} object
     * represents a single instance of the sound. If you want to play the same sound multiple times at the same time,
     * you need to initialize a separate {@link MASound} object.
     *
     * @param fileName path of the file relative to assets folder
     * @return {@link MASound} object.
     */
    public MASound createSound(String fileName) {
        return createSound(fileName, (short) 0, null);
    }

    /**
     * Load a new {@link MASound} object and upload sound data into memory. Each {@link MASound} object
     * represents a single instance of the sound. If you want to play the same sound multiple times at the same time,
     * you need to initialize a separate {@link MASound} object.
     *
     * {@link MASound.Flags} are useful to customize sound loading and managing
     *
     * @param fileName path of the file relative to assets folder
     * @param flags flags for audio loading
     * @param group where sound should be attached, can be null
     * @return {@link MASound} object.
     */
    public MASound createSound(String fileName, short flags, MAGroup group) {
        return createSound(fileName, flags, group, false);
    }

    /**
     * Load a new {@link MASound} object and upload sound data into memory. Each {@link MASound} object
     * represents a single instance of the sound. If you want to play the same sound multiple times at the same time,
     * you need to initialize a separate {@link MASound} object.
     *
     * {@link MASound.Flags} are useful to customize sound loading and managing
     *
     * @param fileName path of the file relative to assets folder
     * @param flags flags for audio loading
     * @param group where sound should be attached, can be null
     * @param external true if file needs to be read outside the `assets` directory (only for Android and iOS)
     * @return {@link MASound} object.
     */
    public MASound createSound(String fileName, short flags, MAGroup group, boolean external) {
        if (external && Gdx.app.getType() == Application.ApplicationType.Android)
            fileName = MA_ANDROID_STORAGE_EXTERNAL_PREFIX + fileName;
        return new MASound(jniCreateSound(fileName, flags, group == null ? -1 : group.address, external), this);
    }

    private native long jniCreateSound(String fileName, short flags, long group, boolean external); /*
        ma_sound_group* pGroup = group == -1 ? NULL : (ma_sound_group*) group;
        ma_sound* sound = (ma_sound*) ma_malloc(sizeof(ma_sound), NULL);
        #if defined(MA_APPLE_MOBILE)
        std::string cppStr = getBundlePath(fileName);
        char* cFileName = const_cast<char*>(cppStr.c_str());
        ma_result result = ma_sound_init_from_file(&engine, external ? fileName : cFileName, flags, pGroup, NULL, sound);
        #else
        ma_result result = ma_sound_init_from_file(&engine, fileName, flags, pGroup, NULL, sound);
        #endif
        if (result != MA_SUCCESS) {
            ma_free(sound, NULL);
            return (jlong) result;
        }
        ma_sound_set_end_callback(sound, sound_end_callback, NULL);
        return (jlong) sound;
    */

    public MASound createInputSound(short flags, MAGroup group) {
        return new MASound(jniCreateInputSound(flags, group == null ? -1 : group.address), this);
    }

    private native long jniCreateInputSound(short flags, long group); /*
        ma_sound_group* pGroup = group == -1 ? NULL : (ma_sound_group*) group;
        ma_sound* sound = (ma_sound*) ma_malloc(sizeof(ma_sound), NULL);
        ma_result result = ma_sound_init_from_data_source(&engine, &inputBufferData, flags, pGroup, sound);
        if (result != MA_SUCCESS) {
            ma_free(sound, NULL);
            return (jlong) result;
        }
        return (jlong) sound;
    */

    /**
     * Free sound memory. Use when not needed.
     *
     * @param soundAddress native address to sound object
     */
    public void disposeSound(long soundAddress) {
        jniDisposeSound(soundAddress);
    }

    private native void jniDisposeSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_uninit(sound);
        ma_free(sound, NULL);
    */

    /**
     * Get the data source associated to a sound.
     *
     * @param soundAddress native address of the sound
     * @return native address of data source
     */
    public long getSoundDataSource(long soundAddress) {
        return jniGetSoundDataSource(soundAddress);
    }

    private native long jniGetSoundDataSource(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jlong) sound->pDataSource;
     */

    /**
     * Chain two data sources to be played gapless, loop of data sources can also be created.
     *
     * @param dataSource first data source
     * @param nextDataSource next data source played at the end of the first one
     */
    public void chainDataSources(MADataSource dataSource, MADataSource nextDataSource) {
        int result = jniChainDataSources(dataSource.address, nextDataSource.address);
        if (result != MAResult.MA_SUCCESS) {
            throw new MiniAudioException("Error while chaining data sources", result);
        }
    }

    private native int jniChainDataSources(long dataSource, long nextDataSource);/*
        return ma_data_source_set_next((ma_data_source*) dataSource, (ma_data_source*) nextDataSource);
    */

    /**
     * Play or resume sound.
     *
     * @param soundAddress native address to sound object
     */
    public void playSound(long soundAddress) {
        jniPlaySound(soundAddress);
    }

    private native void jniPlaySound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_start(sound);
    */

    /**
     * Pause sound.
     *
     * @param soundAddress native address to sound object
     */
    public void pauseSound(long soundAddress) {
        jniPauseSound(soundAddress);
    }

    private native void jniPauseSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_stop(sound);
    */

    /**
     * Pause and rewind sound to beginning.
     *
     * @param soundAddress native address to sound object
     */
    public void stopSound(long soundAddress) {
        jniStopSound(soundAddress);
    }

    private native void jniStopSound(long soundAddress); /*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_stop(sound);
        ma_sound_seek_to_pcm_frame(sound, 0);
    */

    /**
     * Check if a sound is playing
     *
     * @param soundAddress native address to sound object
     * @return true if sound is playing false otherwise
     */
    public boolean isSoundPlaying(long soundAddress) {
        return jniIsSoundPlaying(soundAddress);
    }

    private native boolean jniIsSoundPlaying(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_is_playing(sound);
    */

    /**
     * Check if a sound is at the end
     *
     * @param soundAddress native address to sound object
     * @return true if sound is at the end false otherwise
     */
    public boolean isSoundEnd(long soundAddress) {
        return jniIsSoundEnd(soundAddress);
    }

    private native boolean jniIsSoundEnd(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_at_end(sound);
    */

    /**
     * Check if a sound is looping
     *
     * @param soundAddress native address to sound object
     * @return true if sound is looping false otherwise
     */
    public boolean isSoundLooping(long soundAddress) {
        return jniIsSoundLooping(soundAddress);
    }

    private native boolean jniIsSoundLooping(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        return (jboolean) ma_sound_is_looping(sound);
    */

    /**
     * Set if the sound should loop or not
     *
     * @param soundAddress native address to sound object
     * @param looping true if sound should loop
     */
    public void setSoundLooping(long soundAddress, boolean looping) {
        jniSetSoundLooping(soundAddress, looping);
    }

    private native void jniSetSoundLooping(long soundAddress, boolean looping);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_looping(sound, looping ? MA_TRUE : MA_FALSE);
    */

    /**
     * By default sounds will be spatialized based on the closest listener. If a sound should always be spatialized
     * relative to a specific listener it can be pinned to one
     *
     * @param soundAddress native address to sound object
     * @param listenerIndex index of the pinned listener
     */
    public void setSoundPinnedListenerIndex(long soundAddress, int listenerIndex) {
        jniSetSoundPinnedListenerIndex(soundAddress, listenerIndex);
    }

    private native void jniSetSoundPinnedListenerIndex(long soundAddress, int listenerIndex);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pinned_listener_index(sound, listenerIndex);
    */

    /**
     * Set sound volume.
     *
     * @param soundAddress native address to sound object
     * @param volume 0 for silence, 1 for default volume, greater than 1 lauder
     */
    public void setSoundVolume(long soundAddress, float volume) {
        jniSetSoundVolume(soundAddress, volume);
    }

    private native void jniSetSoundVolume(long soundAddress, float volume);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_volume(sound, volume);
    */

    /**
     * Control Sound Pitch A larger value will result in a higher pitch. The pitch must be greater than 0.
     *
     * @param soundAddress native address to sound object
     * @param pitch value, 1 default
     */
    public void setSoundPitch(long soundAddress, float pitch) {
        if (pitch <= 0) throw new IllegalArgumentException("Pitch must be > 0");
        jniSetSoundPitch(soundAddress, pitch);
    }

    private native void jniSetSoundPitch(long soundAddress, float pitch);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pitch(sound, pitch);
    */

    /**
     * Setting the pan to 0 will result in an unpanned sound. Setting it to -1 will shift everything to the left, whereas
     * +1 will shift it to the right.
     *
     * @param soundAddress native address to sound object
     * @param pan value in the range [-1, 1]
     */
    public void setSoundPan(long soundAddress, float pan) {
        jniSetSoundPan(soundAddress, pan);
    }

    private native void jniSetSoundPan(long soundAddress, float pan);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_pan(sound, pan);
    */

    /**
     * Enable or disable sound spatialization effects.
     *
     * @param soundAddress native address to sound object
     * @param spatial true by default
     */
    public void setSoundSpatialization(long soundAddress, boolean spatial) {
        jniSetSoundSpatialization(soundAddress, spatial);
    }

    private native void jniSetSoundSpatialization(long soundAddress, boolean spatial);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_spatialization_enabled(sound, spatial ? MA_TRUE : MA_FALSE);
    */

    /**
     * Sounds have a position for 3D Spatialization. By default, the position of a sound is in absolute space or
     * relative to its listener see {@link #setSoundPositioning(long, MAPositioning)}
     *
     * @param soundAddress soundAddress native address to sound object
     * @param x position
     * @param y position
     * @param z position
     */
    public void setSoundPosition(long soundAddress, float x, float y, float z) {
        jniSetSoundPosition(soundAddress, x, y, z);
    }

    private native void jniSetSoundPosition(long soundAddress, float x, float y, float z);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_position(sound, x, y, z);
    */

    /**
     * Sounds have a direction for 3D Spatialization.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setSoundDirection(long soundAddress, float forwardX, float forwardY, float forwardZ) {
        jniSetSoundDirection(soundAddress, forwardX, forwardY, forwardZ);
    }

    private native void jniSetSoundDirection(long soundAddress, float forwardX, float forwardY, float forwardZ);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_direction(sound, forwardX, forwardY, forwardZ);
    */

    /**
     * Sound's also have a cone for controlling directional attenuation. This works exactly the same as
     * listeners
     *
     * @param soundAddress soundAddress native address to sound object
     * @param innerAngleInRadians inner angle in radiance
     * @param outerAngleInRadians outer angle in radiance
     * @param outerGain outer gain
     */
    public void setSoundCone(long soundAddress, float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        jniSetSoundCone(soundAddress, innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    private native void jniSetSoundCone(long soundAddress, float innerAngleInRadians, float outerAngleInRadians, float outerGain);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_cone(sound, innerAngleInRadians, outerAngleInRadians, outerGain);
    */

    /**
     * The velocity of a sound is used for doppler effect.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param velocityX doppler velocity on x-axis
     * @param velocityY doppler velocity on y-axis
     * @param velocityZ doppler velocity on z-axis
     */
    public void setSoundVelocity(long soundAddress, float velocityX, float velocityY, float velocityZ) {
        jniSetSoundVelocity(soundAddress, velocityX, velocityY, velocityZ);
    }

    private native void jniSetSoundVelocity(long soundAddress, float velocityX, float velocityY, float velocityZ);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_velocity(sound, velocityX, velocityY, velocityZ);
    */

    /**
     * The engine supports different attenuation models which can be configured on a per-sound basis.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param maAttenuationModel set pre defined attenuation model
     */
    public void setSoundAttenuationModel(long soundAddress, MAAttenuationModel maAttenuationModel) {
        jniSetSoundAttenuationModel(soundAddress, maAttenuationModel.code);
    }

    private native void jniSetSoundAttenuationModel(long soundAddress, int maAttenuationModel);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        switch (maAttenuationModel) {
            case 0:
                ma_sound_set_attenuation_model(sound, ma_attenuation_model_none);
                break;
            case 1:
                ma_sound_set_attenuation_model(sound, ma_attenuation_model_inverse);
                break;
            case 2:
                ma_sound_set_attenuation_model(sound, ma_attenuation_model_linear);
                break;
            case 3:
                ma_sound_set_attenuation_model(sound, ma_attenuation_model_exponential);
                break;
        }
    */

    /**
     * Sounds have a position. By default, the position of a sound is in absolute space,
     * but it can be changed to be relative to a listener.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param maPositioning type of coordinates position
     */
    public void setSoundPositioning(long soundAddress, MAPositioning maPositioning) {
        jniSetSoundPositioning(soundAddress, maPositioning.code);
    }

    private native void jniSetSoundPositioning(long soundAddress, int maPositioning);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        switch (maPositioning) {
            case 0:
                ma_sound_set_positioning(sound, ma_positioning_absolute);
                break;
            case 1:
                ma_sound_set_positioning(sound, ma_positioning_relative);
                break;
        }
    */

    /**
     * To control how quickly a sound rolls off as it moves away from the listener, rolloff needs to be configured
     *
     * @param soundAddress soundAddress native address to sound object
     * @param rolloff value of the rolloff effect
     */
    public void setSoundRolloff(long soundAddress, float rolloff) {
        jniSetSoundRolloff(soundAddress, rolloff);
    }

    private native void jniSetSoundRolloff(long soundAddress, float rolloff);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_rolloff(sound, rolloff);
    */

    /**
     * Set the minimum and maximum gain to apply from spatialization.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param minGain minimum gain to apply
     * @param maxGain maximum gain to apply
     */
    public void setSoundGainRange(long soundAddress, float minGain, float maxGain) {
        jniSetSoundGainRange(soundAddress, minGain, maxGain);
    }

    private native void jniSetSoundGainRange(long soundAddress, float minGain, float maxGain);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_min_gain(sound, minGain);
        ma_sound_set_max_gain(sound, maxGain);
    */

    /**
     * Likewise, in the calculation of attenuation, you can control the minimum and maximum distances for
     * the attenuation calculation. This is useful if you want to ensure sounds don't drop below a certain
     * volume after the listener moves further away and to have sounds play a maximum volume when the
     * listener is within a certain distance.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param minDistance minimum distance
     * @param maxDistance maximum distance
     */
    public void setSoundDistanceRange(long soundAddress, float minDistance, float maxDistance) {
        jniSetSoundDistanceRange(soundAddress, minDistance, maxDistance);
    }

    private native void jniSetSoundDistanceRange(long soundAddress, float minDistance, float maxDistance);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_min_distance(sound, minDistance);
        ma_sound_set_max_distance(sound, maxDistance);
    */

    /**
     * The engine's spatialization system supports doppler effect. The doppler factor can be configure on
     * a per-sound basis
     *
     * @param soundAddress soundAddress native address to sound object
     * @param dopplerFactor doppler factor
     */
    public void setSoundDopplerFactor(long soundAddress, float dopplerFactor) {
        jniSetSoundDopplerFactor(soundAddress, dopplerFactor);
    }

    private native void jniSetSoundDopplerFactor(long soundAddress, float dopplerFactor);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_doppler_factor(sound, dopplerFactor);
    */

    /**
     * Set current audio playing position in seconds.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param seconds to seek the audio track
     */
    public void seekSoundTo(long soundAddress, float seconds) {
        jniSeekSoundTo(soundAddress, seconds);
    }

    private native void jniSeekSoundTo(long soundAddress, float seconds);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_uint64 frameIndex = ma_engine_get_sample_rate(&engine) * ma_engine_get_channels(&engine) * seconds;
        ma_sound_seek_to_pcm_frame(sound, frameIndex);
    */

    /**
     * Smoothly fade audio volume between two values.
     *
     * @param soundAddress soundAddress native address to sound object
     * @param start starting volume (use -1 for current volume)
     * @param end ending volume (use -1 for current volume)
     * @param milliseconds fade duration in milliseconds
     */
    public void soundFade(long soundAddress, float start, float end, float milliseconds) {
        jniSoundFade(soundAddress, start, end, milliseconds);
    }

    private native void jniSoundFade(long soundAddress, float start, float end, float milliseconds);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_sound_set_fade_in_milliseconds(sound, start, end, milliseconds);
    */

    /**
     * Get current sound cursor position.
     *
     * @param soundAddress soundAddress native address to sound object
     * @return cursor position in seconds
     */
    public float getSoundCursorPosition(long soundAddress) {
        return jniGetSoundCursorPosition(soundAddress);
    }

    private native float jniGetSoundCursorPosition(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        float cursor = 0;
        ma_result res = ma_sound_get_cursor_in_seconds(sound, &cursor);
        if (res != MA_SUCCESS) return res;
        return cursor;
    */

    /**
     * Get current sound cursor position.
     *
     * @param soundAddress soundAddress native address to sound object
     * @return cursor position in PCM frames
     */
    public int getSoundCursorPCMPosition(long soundAddress) {
        return jniGetSoundCursorPCMPosition(soundAddress);
    }

    private native int jniGetSoundCursorPCMPosition(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        ma_uint64 cursor = 0;
        ma_result res = ma_sound_get_cursor_in_pcm_frames(sound, &cursor);
        if (res != MA_SUCCESS) return res;
        return cursor;
    */

    /**
     * Get total length of the sound.
     *
     * @param soundAddress soundAddress native address to sound object
     * @return sound length in seconds
     */
    public float getSoundLength(long soundAddress) {
        return jniGetSoundLength(soundAddress);
    }

    private native float jniGetSoundLength(long soundAddress);/*
        ma_sound* sound = (ma_sound*) soundAddress;
        float length = 0;
        ma_result res = ma_sound_get_length_in_seconds(sound, &length);
        if (res != MA_SUCCESS) return res;
        return length;
     */

    private native long jniGetOutputEndpoint(long graphAddress);/*
        ma_engine* g_engine = (ma_engine*) graphAddress;
        return (jlong) ma_engine_get_endpoint(g_engine);
    */

    /**
     * Connect two nodes into the graph node effects system.
     *
     * @param firstNode left node of the link
     * @param busIndex output bus index of the left node
     * @param secondNode right node of the link
     * @param secondBusIndex input bus index of the right node
     */
    public void attachOutputBus(MANode firstNode, int busIndex, MANode secondNode, int secondBusIndex) {
        int res = jniAttachOutputBus(firstNode.address, busIndex, secondNode.address, secondBusIndex);
        if (res != MAResult.MA_SUCCESS)
            throw new MiniAudioException("Could not attach nodes", res);
    }

    private native int jniAttachOutputBus(long firstNode, int busIndex, long secondNode, int secondBusIndex);/*
        ma_node* pNode = (ma_node*) firstNode;
        ma_node* pOtherNode = (ma_node*) secondNode;
        return ma_node_attach_output_bus(pNode, busIndex, pOtherNode, secondBusIndex);
    */

    /**
     * Detach a specific output bus from the node in effects graph. If you want to just move the output bus from
     * one attachment to another, you do not need to detach first. You can just call {@link #attachOutputBus} and it'll
     * deal with it for you.
     *
     * @param node node to be detached
     * @param busIndex output bus index of the node
     */
    public void detachOutputBus(MANode node, int busIndex) {
        int res = jniDetachOutputBus(node.address, busIndex);
        if (res != MAResult.MA_SUCCESS)
            throw new MiniAudioException("Could not detach node", res);
    }

    private native int jniDetachOutputBus(long node, int busIndex);/*
        ma_node* pNode = (ma_node*) node;
        return ma_node_detach_output_bus(pNode, busIndex);
    */

    /**
     * The volume of an output bus can be configured on a per-bus basis.
     *
     * @param node node to be modified
     * @param busIndex output bus index of the node
     * @param volume of the bus
     */
    public void setOutputBusVolume(MANode node, int busIndex, float volume) {
        int res = jniSetOutputBusVolume(node.address, busIndex, volume);
        if (res != MAResult.MA_SUCCESS)
            throw new MiniAudioException("Could not set output bus volume", res);
    }

    private native int jniSetOutputBusVolume(long node, int busIndex, float volume);/*
        ma_node* pNode = (ma_node*) node;
        return ma_node_set_output_bus_volume(pNode, busIndex, volume);
    */

    /**
     * Attach a node graph to the main engine audio output.
     *
     * @param node to be attached to the engine output
     * @param busIndex index of the input Node
     */
    public void attachToEngineOutput(MANode node, int busIndex) {
        if (busIndex >= node.getSupportedOutputs())
            throw new IllegalArgumentException("Wrong output bus number, the node support up to " + node.getSupportedOutputs() + " buses.");

        int res = jniAttachOutputBus(node.address, busIndex, jniGetOutputEndpoint(engineAddress), 0);
        if (res != MAResult.MA_SUCCESS)
            throw new MiniAudioException("Could not attach node to graph output", res);
    }

    /**
     * Create a new Audio Buffer and allocated a float array data buffer to store raw PCM data.
     *
     * @param size of the buffer
     * @param channels how many channels
     * @return {@link MAAudioBuffer} that can be used for create a new Sound with {@link #createSound(MADataSource)}
     */
    public MAAudioBuffer createAudioBuffer(int size, int channels) {
        long dataBuffer = jniCreateFloatBuffer(size * channels);
        return new MAAudioBuffer(jniCreateAudioBuffer(dataBuffer, size, channels), dataBuffer, size, this);
    }

    private native long jniCreateFloatBuffer(int size);/*
        return (jlong) calloc(size, sizeof(float));
    */

    private native long jniCreateAudioBuffer(long dataBuffer, int size, int channels);/*
        float* pExistingData = (float*) dataBuffer;
        ma_audio_buffer_config config = ma_audio_buffer_config_init(ma_format_f32, channels, size, pExistingData, NULL);

        ma_audio_buffer* buffer = (ma_audio_buffer*) ma_malloc(sizeof(ma_audio_buffer), NULL);
        ma_result result = ma_audio_buffer_init(&config, buffer);
        if (result != MA_SUCCESS) {
            free(buffer);
            return (jlong) result;
        }

        return (jlong) buffer;
    */

    /**
     * Dispose and free memory of the Audio Buffer and attached data buffer
     *
     * @param audioBuffer audio buffer native memory address
     * @param dataBuffer data buffer native memory address
     */
    public void disposeAudioBuffer(long audioBuffer, long dataBuffer) {
        jniDisposeAudioBuffer(audioBuffer, dataBuffer);
    }

    private native void jniDisposeAudioBuffer(long audioBuffer, long dataBuffer);/*
        ma_audio_buffer* buffer = (ma_audio_buffer*) audioBuffer;
        float* pExistingData = (float*) dataBuffer;

        ma_audio_buffer_uninit(buffer);
        ma_free(buffer, NULL);
        free(pExistingData);
    */

    /**
     * Create a new {@link MASound} from an arbitrary {@link MADataSource}.
     *
     * @param dataSource source of the audio data
     * @return {@link MASound} object ready to be used
     */
    public MASound createSound(MADataSource dataSource) {
        return createSound(dataSource, (short) 0, null);
    }

    /**
     * Create a new {@link MASound} from an arbitrary {@link MADataSource}.
     *
     * @param dataSource source of the audio data
     * @param flags flags for audio loading
     * @param group where sound should be attached, can be null
     * @return {@link MASound} object ready to be used
     */
    public MASound createSound(MADataSource dataSource, short flags, MAGroup group) {
        return new MASound(jniCreateSoundFromDataSource(dataSource.address, flags, group == null ? -1 : group.address), this);
    }

    private native long jniCreateSoundFromDataSource(long dataSource, short flags, long group);/*
        ma_sound_group* pGroup = group == -1 ? NULL : (ma_sound_group*) group;
        ma_data_source* source = (ma_data_source*) dataSource;
        ma_sound* sound = (ma_sound*) ma_malloc(sizeof(ma_sound), NULL);
        ma_result result = ma_sound_init_from_data_source(&engine, source, flags, pGroup, sound);
        if (result != MA_SUCCESS) {
            ma_free(sound, NULL);
            return (jlong) result;
        }
        return (jlong) sound;
    */

    /**
     * Create a new waveform object to generate Sine, Square, Triangle and Sawtooth waves
     *
     * @param channels number of channels, usually 2
     * @param type {@link MAWaveformType} of the wave
     * @param amplitude amplitude of the wave
     * @param frequency frequency of the wave
     * @return new {@link  MAWaveform} object
     */
    public MAWaveform createWaveform(int channels, MAWaveformType type, double amplitude, double frequency) {
        return new MAWaveform(jniCreateWaveform(channels, type.code, amplitude, frequency), this);
    }

    private native long jniCreateWaveform(int channels, int type, double amplitude, double frequency);/*
        ma_uint32 sampleRate = ma_engine_get_sample_rate(&engine);
        ma_waveform_type waveType;
        switch(type) {
            case 0:
                waveType = ma_waveform_type_sine;
                break;
            case 1:
                waveType = ma_waveform_type_square;
                break;
            case 2:
                waveType = ma_waveform_type_triangle;
                break;
            case 3:
                waveType = ma_waveform_type_sawtooth;
                break;
            default:
                waveType = ma_waveform_type_sine;
        }

        ma_waveform_config config = ma_waveform_config_init(ma_format_f32, channels, sampleRate, waveType, amplitude, frequency);

        ma_waveform* waveform = (ma_waveform*) ma_malloc(sizeof(ma_waveform), NULL);
        ma_result result = ma_waveform_init(&config, waveform);
        if (result != MA_SUCCESS) {
            free(waveform);
            return (jlong) result;
        }

        return (jlong) waveform;
    */

    /**
     * Change waveform amplitude dynamically.
     *
     * @param waveformAddress native address to waveform object
     * @param amplitude amplitude of the wave
     */
    public void setWaveformAmplitude(long waveformAddress, double amplitude) {
        jniSetWaveformAmplitude(waveformAddress, amplitude);
    }

    private native void jniSetWaveformAmplitude(long waveformAddress, double amplitude);/*
        ma_waveform* waveform = (ma_waveform*) waveformAddress;
        ma_waveform_set_amplitude(waveform, amplitude);
    */

    /**
     * Change waveform frequency dynamically.
     *
     * @param waveformAddress native address to waveform object
     * @param frequency frequency of the wave
     */
    public void setWaveformFrequency(long waveformAddress, double frequency) {
        jniSetWaveformFrequency(waveformAddress, frequency);
    }

    private native void jniSetWaveformFrequency(long waveformAddress, double frequency);/*
        ma_waveform* waveform = (ma_waveform*) waveformAddress;
        ma_waveform_set_frequency(waveform, frequency);
    */

    /**
     * Change waveform type dynamically.
     *
     * @param waveformAddress native address to waveform object
     * @param type {@link MAWaveformType} of the wave
     */
    public void setWaveformType(long waveformAddress, MAWaveformType type) {
        jniSetWaveformType(waveformAddress, type.code);
    }

    private native void jniSetWaveformType(long waveformAddress, int type);/*
        ma_waveform* waveform = (ma_waveform*) waveformAddress;
        ma_waveform_type waveType;
        switch(type) {
            case 0:
                waveType = ma_waveform_type_sine;
                break;
            case 1:
                waveType = ma_waveform_type_square;
                break;
            case 2:
                waveType = ma_waveform_type_triangle;
                break;
            case 3:
                waveType = ma_waveform_type_sawtooth;
                break;
            default:
                waveType = ma_waveform_type_sine;
        }
        ma_waveform_set_type(waveform, waveType);
    */

    /**
     * Free waveform memory. Use when not needed.
     *
     * @param waveformAddress native address to waveform object
     */
    public void disposeWaveform(long waveformAddress) {
        jniDisposeWaveform(waveformAddress);
    }

    private native void jniDisposeWaveform(long waveformAddress); /*
        ma_waveform* waveform = (ma_waveform*) waveformAddress;
        ma_waveform_uninit(waveform);
        ma_free(waveform, NULL);
    */

    /**
     * Creates a new Noise object. The noise API uses simple LCG random number generation. It supports a custom seed
     * which is useful for things like automated testing requiring reproducibility. Setting the seed to zero will
     * default to MA_DEFAULT_LCG_SEED.
     *
     * @param channels number of channels, usually 2
     * @param type {@link MANoiseType} of the wave
     * @param seed random seed or 0 for default LCG Seed
     * @param amplitude amplitude of the wave
     * @return new {@link  MANoise} object
     */
    public MANoise createNoise(int channels, MANoiseType type, int seed, double amplitude) {
        return new MANoise(jniCreateNoise(channels, type.code, seed, amplitude), this);
    }

    private native long jniCreateNoise(int channels, int type, int seed, double amplitude);/*
        ma_noise_type noiseType;
        switch(type) {
            case 0:
                noiseType = ma_noise_type_white;
                break;
            case 1:
                noiseType = ma_noise_type_pink;
                break;
            case 2:
                noiseType = ma_noise_type_brownian;
                break;
            default:
                noiseType = ma_noise_type_white;
        }

        ma_noise_config config = ma_noise_config_init(ma_format_f32, channels, noiseType, seed, amplitude);

        ma_noise* noise = (ma_noise*) ma_malloc(sizeof(ma_noise), NULL);
        ma_result result = ma_noise_init(&config, NULL, noise);
        if (result != MA_SUCCESS) {
            free(noise);
            return (jlong) result;
        }

        return (jlong) noise;
    */

    /**
     * Free noise memory. Use when not needed.
     *
     * @param noiseAddress native address to noise object
     */
    public void disposeNoise(long noiseAddress) {
        jniDisposeNoise(noiseAddress);
    }

    private native void jniDisposeNoise(long noiseAddress); /*
        ma_noise* noise = (ma_noise*) noiseAddress;
        ma_noise_uninit(noise, NULL);
        ma_free(noise, NULL);
    */

    /**
     * Create a new sound group to manage multiple sounds together.
     *
     * @return {@link MAGroup} object
     */
    public MAGroup createGroup() {
        return createGroup((short) 0, null);
    }

    /**
     * Create a new sound group to manage multiple sounds together.
     *
     * @param flags group customizations using {@link games.rednblack.miniaudio.MASound.Flags}
     * @param parentGroup parent group
     * @return {@link MAGroup} object
     */
    public MAGroup createGroup(short flags, MAGroup parentGroup) {
        return new MAGroup(jniCreateGroup(flags, parentGroup == null ? -1 : parentGroup.address), this);
    }

    private native long jniCreateGroup(short flags, long parentGroup);/*
        ma_sound_group* pParentGroup = parentGroup == -1 ? NULL : (ma_sound_group*) parentGroup;
        ma_sound_group* group = (ma_sound_group*) ma_malloc(sizeof(ma_sound_group), NULL);
        ma_result result = ma_sound_group_init(&engine, flags, pParentGroup, group);
        if (result != MA_SUCCESS) {
            free(group);
            return (jlong) result;
        }

        return (jlong) group;
    */

    /**
     * Free group memory. Use when not needed.
     *
     * @param groupAddress native address to group object
     */
    public void disposeGroup(long groupAddress) {
        jniDisposeGroup(groupAddress);
    }

    private native void jniDisposeGroup(long groupAddress); /*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_uninit(group);
        ma_free(group, NULL);
    */

    /**
     * Play or resume sound group.
     *
     * @param groupAddress native address to group object
     */
    public void playGroup(long groupAddress) {
        jniPlayGroup(groupAddress);
    }

    private native void jniPlayGroup(long groupAddress); /*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_start(group);
    */

    /**
     * Pause sound group.
     *
     * @param groupAddress native address to group object
     */
    public void pauseGroup(long groupAddress) {
        jniPauseGroup(groupAddress);
    }

    private native void jniPauseGroup(long groupAddress); /*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_stop(group);
    */

    /**
     * Set sound group volume.
     *
     * @param groupAddress native address to group object
     * @param volume 0 for silence, 1 for default volume, greater than 1 lauder
     */
    public void setGroupVolume(long groupAddress, float volume) {
        jniSetGroupVolume(groupAddress, volume);
    }

    private native void jniSetGroupVolume(long groupAddress, float volume);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_volume(group, volume);
    */

    /**
     * Control Sound Group Pitch A larger value will result in a higher pitch. The pitch must be greater than 0.
     *
     * @param groupAddress native address to group object
     * @param pitch value, 1 default
     */
    public void setGroupPitch(long groupAddress, float pitch) {
        if (pitch <= 0) throw new IllegalArgumentException("Pitch must be > 0");
        jniSetGroupPitch(groupAddress, pitch);
    }

    private native void jniSetGroupPitch(long groupAddress, float pitch);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_pitch(group, pitch);
    */

    /**
     * Setting the pan to 0 will result in an unpanned sound. Setting it to -1 will shift everything to the left, whereas
     * +1 will shift it to the right.
     *
     * @param groupAddress native address to group object
     * @param pan value in the range [-1, 1]
     */
    public void setGroupPan(long groupAddress, float pan) {
        jniSetGroupPan(groupAddress, pan);
    }

    private native void jniSetGroupPan(long groupAddress, float pan);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_pan(group, pan);
    */

    /**
     * Enable or disable sound spatialization effects.
     *
     * @param groupAddress native address to group object
     * @param spatial true by default
     */
    public void setGroupSpatialization(long groupAddress, boolean spatial) {
        jniSetGroupSpatialization(groupAddress, spatial);
    }

    private native void jniSetGroupSpatialization(long groupAddress, boolean spatial);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_spatialization_enabled(group, spatial ? MA_TRUE : MA_FALSE);
    */

    /**
     * By default, groups will be spatialized based on the closest listener. If a group should always be spatialized
     * relative to a specific listener it can be pinned to one
     *
     * @param groupAddress native address to group object
     * @param listenerIndex index of the pinned listener
     */
    public void setGroupPinnedListenerIndex(long groupAddress, int listenerIndex) {
        jniSetGroupPinnedListenerIndex(groupAddress, listenerIndex);
    }

    private native void jniSetGroupPinnedListenerIndex(long groupAddress, int listenerIndex);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_pinned_listener_index(group, listenerIndex);
    */

    /**
     * Groups have a position for 3D Spatialization. By default, the position of a group is in absolute space or
     * relative to its listener see {@link #setGroupPositioning(long, MAPositioning)}.
     *
     * @param groupAddress native address to group object
     * @param x position
     * @param y position
     * @param z position
     */
    public void setGroupPosition(long groupAddress, float x, float y, float z) {
        jniSetGroupPosition(groupAddress, x, y, z);
    }

    private native void jniSetGroupPosition(long groupAddress, float x, float y, float z);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_position(group, x, y, z);
    */

    /**
     * Groups have a direction for 3D Spatialization.
     *
     * @param groupAddress native address to group object
     * @param forwardX direction
     * @param forwardY direction
     * @param forwardZ direction
     */
    public void setGroupDirection(long groupAddress, float forwardX, float forwardY, float forwardZ) {
        jniSetGroupDirection(groupAddress, forwardX, forwardY, forwardZ);
    }

    private native void jniSetGroupDirection(long groupAddress, float forwardX, float forwardY, float forwardZ);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_direction(group, forwardX, forwardY, forwardZ);
    */

    /**
     * The velocity of a group is used for doppler effect.
     *
     * @param groupAddress native address to group object
     * @param velocityX doppler velocity on x-axis
     * @param velocityY doppler velocity on y-axis
     * @param velocityZ doppler velocity on z-axis
     */
    public void setGroupVelocity(long groupAddress, float velocityX, float velocityY, float velocityZ) {
        jniSetGroupVelocity(groupAddress, velocityX, velocityY, velocityZ);
    }

    private native void jniSetGroupVelocity(long groupAddress, float velocityX, float velocityY, float velocityZ);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_velocity(group, velocityX, velocityY, velocityZ);
    */

    /**
     * The engine supports different attenuation models which can be configured on a per-sound basis.
     *
     * @param groupAddress native address to group object
     * @param maAttenuationModel set pre defined attenuation model
     */
    public void setGroupAttenuationModel(long groupAddress, MAAttenuationModel maAttenuationModel) {
        jniSetGroupAttenuationModel(groupAddress, maAttenuationModel.code);
    }

    private native void jniSetGroupAttenuationModel(long groupAddress, int maAttenuationModel);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        switch (maAttenuationModel) {
            case 0:
                ma_sound_group_set_attenuation_model(group, ma_attenuation_model_none);
                break;
            case 1:
                ma_sound_group_set_attenuation_model(group, ma_attenuation_model_inverse);
                break;
            case 2:
                ma_sound_group_set_attenuation_model(group, ma_attenuation_model_linear);
                break;
            case 3:
                ma_sound_group_set_attenuation_model(group, ma_attenuation_model_exponential);
                break;
        }
    */

    /**
     * Groups have a position. By default, the position of a sound is in absolute space,
     * but it can be changed to be relative to a listener.
     *
     * @param groupAddress native address to group object
     * @param maPositioning type of coordinates position
     */
    public void setGroupPositioning(long groupAddress, MAPositioning maPositioning) {
        jniSetGroupPositioning(groupAddress, maPositioning.code);
    }

    private native void jniSetGroupPositioning(long groupAddress, int maPositioning);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        switch (maPositioning) {
            case 0:
                ma_sound_group_set_positioning(group, ma_positioning_absolute);
                break;
            case 1:
                ma_sound_group_set_positioning(group, ma_positioning_relative);
                break;
        }
    */

    /**
     * To control how quickly a group rolls off as it moves away from the listener, rolloff needs to be configured
     *
     * @param groupAddress native address to group object
     * @param rolloff value of the rolloff effect
     */
    public void setGroupRolloff(long groupAddress, float rolloff) {
        jniSetGroupRolloff(groupAddress, rolloff);
    }

    private native void jniSetGroupRolloff(long groupAddress, float rolloff);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_rolloff(group, rolloff);
    */

    /**
     * Set the minimum and maximum gain to apply from spatialization.
     *
     * @param groupAddress native address to group object
     * @param minGain minimum gain to apply
     * @param maxGain maximum gain to apply
     */
    public void setGroupGainRange(long groupAddress, float minGain, float maxGain) {
        jniSetGroupGainRange(groupAddress, minGain, maxGain);
    }

    private native void jniSetGroupGainRange(long groupAddress, float minGain, float maxGain);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_min_gain(group, minGain);
        ma_sound_group_set_max_gain(group, maxGain);
    */

    /**
     * Likewise, in the calculation of attenuation, you can control the minimum and maximum distances for
     * the attenuation calculation. This is useful if you want to ensure sounds don't drop below a certain
     * volume after the listener moves further away and to have sounds play a maximum volume when the
     * listener is within a certain distance.
     *
     * @param groupAddress native address to group object
     * @param minDistance minimum distance
     * @param maxDistance maximum distance
     */
    public void setGroupDistanceRange(long groupAddress, float minDistance, float maxDistance) {
        jniSetGroupDistanceRange(groupAddress, minDistance, maxDistance);
    }

    private native void jniSetGroupDistanceRange(long groupAddress, float minDistance, float maxDistance);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_min_distance(group, minDistance);
        ma_sound_group_set_max_distance(group, maxDistance);
    */

    /**
     * Groups also have a cone for controlling directional attenuation. This works exactly the same as
     * listeners
     *
     * @param groupAddress native address to group object
     * @param innerAngleInRadians inner angle in radiance
     * @param outerAngleInRadians outer angle in radiance
     * @param outerGain outer gain
     */
    public void setGroupCone(long groupAddress, float innerAngleInRadians, float outerAngleInRadians, float outerGain) {
        jniSetGroupCone(groupAddress, innerAngleInRadians, outerAngleInRadians, outerGain);
    }

    private native void jniSetGroupCone(long groupAddress, float innerAngleInRadians, float outerAngleInRadians, float outerGain);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_cone(group, innerAngleInRadians, outerAngleInRadians, outerGain);
    */

    /**
     * The engine's spatialization system supports doppler effect. The doppler factor can be configured on
     * a per-sound basis
     *
     * @param groupAddress native address to group object
     * @param dopplerFactor doppler factor
     */
    public void setGroupDopplerFactor(long groupAddress, float dopplerFactor) {
        jniSetGroupDopplerFactor(groupAddress, dopplerFactor);
    }

    private native void jniSetGroupDopplerFactor(long groupAddress, float dopplerFactor);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_doppler_factor(group, dopplerFactor);
    */

    /**
     * Smoothly fade group's volume between two values.
     *
     * @param groupAddress native address to group object
     * @param start starting volume (use -1 for current volume)
     * @param end ending volume (use -1 for current volume)
     * @param milliseconds fade duration in milliseconds
     */
    public void groupFade(long groupAddress, float start, float end, float milliseconds) {
        jniGroupFade(groupAddress, start, end, milliseconds);
    }

    private native void jniGroupFade(long groupAddress, float start, float end, float milliseconds);/*
        ma_sound_group* group = (ma_sound_group*) groupAddress;
        ma_sound_group_set_fade_in_milliseconds(group, start, end, milliseconds);
    */

    /**
     * Attach a listener to be notified when a sound is finished playing.
     * MASound must be created with {@link #createSound(String)}.
     *
     * NOTE: Listener is called on MiniAudio thread.
     *
     * @param endListener {@link MASoundEndListener}
     */
    public void setEndListener(MASoundEndListener endListener) {
        this.endListener = endListener;
    }

    public void on_native_sound_end(long soundAddress) {
        if (endListener != null) {
            endCallbackSound.setAddress(soundAddress);
            endListener.onSoundEnd(endCallbackSound);
        }
    }

    /**
     * Set a custom log callback function. If null, default libGDX logging will be used.
     *
     * @param logCallback {@link MALogCallback} custom log callback
     */
    public void setLogCallback(MALogCallback logCallback) {
        this.logCallback = logCallback;
    }

    public void on_native_log(int level, String message) {
        if (logCallback != null) {
            logCallback.onLog(MALogLevel.decode(level), message);
            return;
        }

        if (Gdx.app == null) return;

        switch (level) {
            case 1:
            case 2:
                Gdx.app.error(TAG, message);
                break;
            case 3:
                Gdx.app.log(TAG, message);
                break;
            case 4:
                Gdx.app.debug(TAG, message);
                break;
        }
    }

    /**
     * Set a listener to be notified when the device change its state, i.e. when an interruption begins.
     *
     * @param deviceNotificationListener {@link MADeviceNotificationListener}
     */
    public void setDeviceNotificationListener(MADeviceNotificationListener deviceNotificationListener) {
        this.deviceNotificationListener = deviceNotificationListener;
    }

    public void on_native_notification(int type) {
        if (deviceNotificationListener != null) {
            deviceNotificationListener.onNotification(MADeviceNotificationType.decode(type));
        }
    }

    /**
     * Decode a bytes array into a {@link MAAudioBuffer} using MiniAudio's decoder, useful when there's no direct access
     * to files.
     *
     * @param data bytes array input data
     * @param outputSize size of the decoded PCM frames
     * @param outputChannels how many channels (usually 2)
     * @return {@link MAAudioBuffer} with decoded data inside
     */
    public MAAudioBuffer decodeBytes(byte[] data, int outputSize, int outputChannels) {
        long dataBuffer = jniCreateFloatBuffer(outputSize * outputChannels);
        int decodedFrames = jniDecodeBytes(data, data.length, dataBuffer, outputSize, outputChannels);
        if (MAResult.checkErrors(decodedFrames)) {
            throw new MiniAudioException("Error while decoding byte array", decodedFrames);
        }

        if (decodedFrames == 0) {
            throw new MiniAudioException("Could not decode any frame from data", MAResult.MA_ERROR);
        }

        return new MAAudioBuffer(jniCreateAudioBuffer(dataBuffer, decodedFrames, outputChannels), dataBuffer, decodedFrames, this);
    }

    private native int jniDecodeBytes(byte[] data, int length, long outBuffer, int frameCount, int outputChannels);/*
        ma_uint32 sampleRate = ma_engine_get_sample_rate(&engine);

        void* framesOut = (void*) outBuffer;

        ma_decoder decoder;
        ma_decoder_config config = ma_decoder_config_init(ma_format_f32, outputChannels, sampleRate);

        ma_result result = ma_decoder_init_memory(data, length, &config, &decoder);
        if (result != MA_SUCCESS) {
            ma_decoder_uninit(&decoder);
            return result;
        }

        ma_uint64 framesOutCount = 0;

        result = ma_decoder_read_pcm_frames(&decoder, framesOut, frameCount, &framesOutCount);
        if (result != MA_SUCCESS) {
            ma_decoder_uninit(&decoder);
            return result;
        }

        ma_decoder_uninit(&decoder);
        return framesOutCount;
    */
}
