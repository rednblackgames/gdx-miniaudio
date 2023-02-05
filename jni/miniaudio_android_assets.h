#ifndef MINIAUDIO_ANDROID_ASSETS_H
#define MINIAUDIO_ANDROID_ASSETS_H

#define MA_ANDROID_STORAGE_EXTERNAL_PREFIX "external:"
#define MA_ANDROID_STORAGE_EXTERNAL_PREFIX_LEN 9

#include <android/asset_manager.h>

typedef struct {
    ma_vfs_callbacks cb;
    ma_allocation_callbacks allocationCallbacks;
    AAssetManager* asset_manager;
} ma_android_vfs;

typedef enum {
    storage_type_assets = 0,
    storage_type_external = 1
} ma_android_vfs_storage_type;

typedef struct {
    ma_vfs_file* file;
    ma_android_vfs_storage_type storage_type;
} ma_android_vfs_file;

ma_result ma_android_vfs_open(ma_vfs* pVFS, const char* pFilePath, ma_uint32 openMode, ma_vfs_file* pFile) {
    if (pFile == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) ma_malloc(sizeof(ma_android_vfs_file), NULL);
    wrapperFile->file = (ma_vfs_file*) ma_malloc(sizeof(ma_vfs_file), NULL);

    if (strncmp(MA_ANDROID_STORAGE_EXTERNAL_PREFIX, pFilePath, MA_ANDROID_STORAGE_EXTERNAL_PREFIX_LEN) == 0) {
        wrapperFile->storage_type = storage_type_external;

        ma_result res = ma_default_vfs_open(pVFS, pFilePath + MA_ANDROID_STORAGE_EXTERNAL_PREFIX_LEN, openMode, wrapperFile->file);

        if (res == MA_SUCCESS) {
            *pFile = (ma_vfs_file*) wrapperFile;
        } else {
            ma_free(wrapperFile->file, NULL);
            ma_free(wrapperFile, NULL);
        }
        return res;
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;

    if (androidVfs->asset_manager == NULL) {
        ma_free(wrapperFile->file, NULL);
        ma_free(wrapperFile, NULL);
        return MA_UNAVAILABLE;
    }

    *pFile = NULL;

    if (pFilePath == NULL || openMode == 0) {
        ma_free(wrapperFile->file, NULL);
        ma_free(wrapperFile, NULL);
        return MA_INVALID_ARGS;
    }

    AAsset* asset = AAssetManager_open(androidVfs->asset_manager, pFilePath, AASSET_MODE_STREAMING);
    if (asset == NULL) {
        ma_free(wrapperFile->file, NULL);
        ma_free(wrapperFile, NULL);
        return MA_DOES_NOT_EXIST;
    }

    wrapperFile->storage_type = storage_type_assets;
    wrapperFile->file = (ma_vfs_file*) asset;
    *pFile = (ma_vfs_file*) wrapperFile;

    return MA_SUCCESS;
}

ma_result ma_android_vfs_close(ma_vfs* pVFS, ma_vfs_file file) {
    if (file == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;
    if (wrapperFile->storage_type == storage_type_external) {
        return ma_default_vfs_close(pVFS, *wrapperFile->file);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;

    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->file;

    AAsset_close(asset);

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
        return ma_default_vfs_read(pVFS, *wrapperFile->file, pDst, sizeInBytes, pBytesRead);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;

    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->file;
    int result = AAsset_read(asset, pDst, sizeInBytes);

    if (pBytesRead != NULL) {
        *pBytesRead = result;
    }

    if (result != sizeInBytes) {
        if (result == 0) {
            return MA_AT_END;
        } else {
            return MA_ERROR;
        }
    }

    return MA_SUCCESS;
}

ma_result ma_android_vfs_seek(ma_vfs* pVFS, ma_vfs_file file, ma_int64 offset, ma_seek_origin origin) {
    if (file == NULL) {
        return MA_INVALID_ARGS;
    }

    ma_android_vfs_file* wrapperFile = (ma_android_vfs_file*) file;
    if (wrapperFile->storage_type == storage_type_external) {
        return ma_default_vfs_seek(pVFS, *wrapperFile->file, offset, origin);
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

    AAsset* asset = (AAsset*) wrapperFile->file;
    off_t result = AAsset_seek(asset, offset, whence);

    if (result == -1) {
        return MA_ERROR;
    }

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
        return ma_default_vfs_info(pVFS, *wrapperFile->file, pInfo);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;

    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->file;
    off_t result = AAsset_getLength(asset);

    pInfo->sizeInBytes = result;
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
        return ma_default_vfs_tell(pVFS, *wrapperFile->file, pCursor);
    }

    ma_android_vfs* androidVfs = (ma_android_vfs*) pVFS;

    if (androidVfs->asset_manager == NULL) {
        return MA_UNAVAILABLE;
    }

    AAsset* asset = (AAsset*) wrapperFile->file;

    off_t length = AAsset_getLength(asset);
    off_t remainingLength = AAsset_getRemainingLength(asset);

    *pCursor = length - remainingLength;

    return MA_SUCCESS;
}

ma_result ma_android_vfs_init(ma_android_vfs* pVFS, const ma_allocation_callbacks* pAllocationCallbacks) {
    if (pVFS == NULL) {
        return MA_INVALID_ARGS;
    }

    pVFS->cb.onOpen  = ma_android_vfs_open;
    pVFS->cb.onOpenW = NULL;
    pVFS->cb.onClose = ma_android_vfs_close;
    pVFS->cb.onRead  = ma_android_vfs_read;
    pVFS->cb.onWrite = NULL;
    pVFS->cb.onSeek  = ma_android_vfs_seek;
    pVFS->cb.onTell  = ma_android_vfs_tell;
    pVFS->cb.onInfo  = ma_android_vfs_info;
    ma_allocation_callbacks_init_copy(&pVFS->allocationCallbacks, pAllocationCallbacks);

    pVFS->asset_manager = NULL;

    return MA_SUCCESS;
}

#endif
