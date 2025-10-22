#ifndef MINIAUDIO_ANDROID_ASSETS_H
#define MINIAUDIO_ANDROID_ASSETS_H

#define MA_ANDROID_STORAGE_EXTERNAL_PREFIX "external:"
#define MA_ANDROID_STORAGE_EXTERNAL_PREFIX_LEN 9

#include <android/asset_manager.h>

typedef struct {
    ma_vfs_callbacks cb;
    ma_allocation_callbacks allocationCallbacks;
    AAssetManager* asset_manager;
    ma_log* pLog;
} ma_android_vfs;

typedef enum {
    storage_type_assets = 0,
    storage_type_external = 1
} ma_android_vfs_storage_type;

typedef struct {
    ma_vfs_file handle;
    ma_android_vfs_storage_type storage_type;
} ma_android_vfs_file;

ma_result ma_android_vfs_open(ma_vfs* pVFS, const char* pFilePath, ma_uint32 openMode, ma_vfs_file* pFile) {
    if (pFile == NULL) {
        return MA_INVALID_ARGS;
    }

    // Always clear the output parameter on entry.
    *pFile = NULL;

    if (pFilePath == NULL || openMode == 0) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;
    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) ma_malloc(sizeof(ma_android_vfs_file), &androidVfs->allocationCallbacks);
    if (wrapperFile == NULL) {
        return MA_OUT_OF_MEMORY;
    }

    if (strncmp(MA_ANDROID_STORAGE_EXTERNAL_PREFIX, pFilePath, MA_ANDROID_STORAGE_EXTERNAL_PREFIX_LEN) == 0) {
        wrapperFile->storage_type = storage_type_external;
        wrapperFile->handle = NULL;

        // Pass the address of the handle member so the function can fill it.
        ma_result res = ma_default_vfs_open(pVFS, pFilePath + MA_ANDROID_STORAGE_EXTERNAL_PREFIX_LEN, openMode, &wrapperFile->handle);

        if (res == MA_SUCCESS) {
            *pFile = (ma_vfs_file*) wrapperFile;
        } else {
            // No extra pointer to free, just the wrapper itself.
            ma_free(wrapperFile, &androidVfs->allocationCallbacks);
        }
        return res;
    }

    // From here, we handle assets.
    if (androidVfs->asset_manager == NULL) {
        ma_free(wrapperFile, &androidVfs->allocationCallbacks);
        return MA_UNAVAILABLE;
    }

    AAsset* asset = AAssetManager_open(androidVfs->asset_manager, pFilePath, AASSET_MODE_STREAMING);
    if (asset == NULL) {
        ma_free(wrapperFile, &androidVfs->allocationCallbacks);
        return MA_DOES_NOT_EXIST;
    }

    wrapperFile->storage_type = storage_type_assets;
    // Store the handle directly. No extra allocation is needed.
    wrapperFile->handle = (ma_vfs_file) asset;
    *pFile = (ma_vfs_file*) wrapperFile;

    return MA_SUCCESS;
}

ma_result ma_android_vfs_close(ma_vfs* pVFS, ma_vfs_file file) {
    if (file == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;
    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;

    if (wrapperFile->storage_type == storage_type_external) {
        // Pass the handle directly to the close function.
        ma_result result = ma_default_vfs_close(pVFS, wrapperFile->handle);
        ma_free(wrapperFile, &androidVfs->allocationCallbacks);
        return result;
    }

    // Asset path.
    if (androidVfs->asset_manager == NULL) {
        ma_free(wrapperFile, &androidVfs->allocationCallbacks);
        return MA_UNAVAILABLE; // Should not happen if open succeeded, but good practice.
    }

    AAsset* asset = (AAsset*) wrapperFile->handle;
    AAsset_close(asset);
    ma_free(wrapperFile, &androidVfs->allocationCallbacks);

    return MA_SUCCESS;
}

ma_result ma_android_vfs_read(ma_vfs* pVFS, ma_vfs_file file, void* pDst, size_t sizeInBytes, size_t* pBytesRead) {
    if (pBytesRead != NULL) {
        *pBytesRead = 0;
    }

    if (file == NULL || pDst == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;
    if (wrapperFile->storage_type == storage_type_external) {
        // Pass the stored handle directly.
        return ma_default_vfs_read(pVFS, wrapperFile->handle, pDst, sizeInBytes, pBytesRead);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;
    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->handle;
    int result = AAsset_read(asset, pDst, sizeInBytes);

    if (result < 0) {
        ma_log_postf(androidVfs->pLog, MA_LOG_LEVEL_ERROR, "AAsset_read failed with error code %d.\n", result);
        return MA_ERROR;
    }

    if (pBytesRead != NULL) {
        *pBytesRead = (size_t) result;
    }

    if (result == 0) {
        return MA_AT_END;
    }

    return MA_SUCCESS;
}

ma_result ma_android_vfs_seek(ma_vfs* pVFS, ma_vfs_file file, ma_int64 offset, ma_seek_origin origin) {
    if (file == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;
    if (wrapperFile->storage_type == storage_type_external) {
        return ma_default_vfs_seek(pVFS, wrapperFile->handle, offset, origin);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;
    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    int whence;
    if (origin == ma_seek_origin_start) {
        whence = SEEK_SET;
    } else if (origin == ma_seek_origin_end) {
        whence = SEEK_END;
    } else {
        whence = SEEK_CUR;
    }

    AAsset* asset = (AAsset*) wrapperFile->handle;
    off_t result = AAsset_seek(asset, offset, whence);

    if (result == -1) {
        ma_log_postf(androidVfs->pLog, MA_LOG_LEVEL_ERROR, "AAsset_seek returned -1.\n");
        return MA_ERROR;
    }

    return MA_SUCCESS;
}

ma_result ma_android_vfs_tell(ma_vfs* pVFS, ma_vfs_file file, ma_int64* pCursor) {
    if (pCursor == NULL) {
        return MA_INVALID_ARGS;
    }
    *pCursor = 0;

    if (file == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;
    if (wrapperFile->storage_type == storage_type_external) {
        return ma_default_vfs_tell(pVFS, wrapperFile->handle, pCursor);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;
    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->handle;
    off_t length = AAsset_getLength(asset);
    off_t remainingLength = AAsset_getRemainingLength(asset);

    *pCursor = length - remainingLength;

    return MA_SUCCESS;
}

ma_result ma_android_vfs_info(ma_vfs* pVFS, ma_vfs_file file, ma_file_info* pInfo) {
    if (pInfo == NULL) {
        return MA_INVALID_ARGS;
    }
    MA_ZERO_OBJECT(pInfo);

    if (file == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;
    if (wrapperFile->storage_type == storage_type_external) {
        return ma_default_vfs_info(pVFS, wrapperFile->handle, pInfo);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;
    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->handle;
    off_t result = AAsset_getLength(asset);

    pInfo->sizeInBytes = result;
    return MA_SUCCESS;
}

ma_result ma_android_vfs_init(ma_android_vfs* pVFS, const ma_allocation_callbacks* pAllocationCallbacks) {
    if (pVFS == NULL) {
        return MA_INVALID_ARGS;
    }
    MA_ZERO_OBJECT(pVFS);

    pVFS->cb.onOpen  = ma_android_vfs_open;
    pVFS->cb.onOpenW = NULL; // Assets are read-only and use UTF-8 paths
    pVFS->cb.onClose = ma_android_vfs_close;
    pVFS->cb.onRead  = ma_android_vfs_read;
    pVFS->cb.onWrite = NULL; // Assets are read-only
    pVFS->cb.onSeek  = ma_android_vfs_seek;
    pVFS->cb.onTell  = ma_android_vfs_tell;
    pVFS->cb.onInfo  = ma_android_vfs_info;

    ma_allocation_callbacks_init_copy(&pVFS->allocationCallbacks, pAllocationCallbacks);
    pVFS->asset_manager = NULL;

    return MA_SUCCESS;
}

#endif // MINIAUDIO_ANDROID_ASSETS_H