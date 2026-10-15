#ifndef MINIAUDIO_JAR_VFS_H
#define MINIAUDIO_JAR_VFS_H

// Desktop VFS able to read entries packed inside jar (zip) archives.
//
// Archives are registered once from Java and identified by an index, entries are then
// opened with paths in the form "jar:<archive index>:<entry name>". Every other path is
// forwarded to the default VFS.
//
// - STORED entries are read straight from the archive file, no extra memory needed.
// - DEFLATED entries are inflated into memory when opened (encoded data, not PCM), so
//   decoders still see a regular seekable file with length and streaming support.

#define MINIZ_NO_STDIO
#define MINIZ_NO_TIME
#define MINIZ_NO_DEFLATE_APIS
#define MINIZ_NO_ARCHIVE_WRITING_APIS
#define MINIZ_NO_ZLIB_COMPATIBLE_NAMES
#include "miniz.c"

#define MA_JAR_VFS_PREFIX "jar:"
#define MA_JAR_VFS_PREFIX_LEN 4

#define MA_ZIP_LOCAL_HEADER_SIGNATURE 0x04034b50
#define MA_ZIP_LOCAL_HEADER_SIZE 30

typedef struct ma_jar_vfs ma_jar_vfs;

typedef struct {
    ma_jar_vfs* pVFS;
    // The archive file is shared by every entry, lock guards seek+read pairs and miniz state
    ma_vfs_file handle;
    ma_mutex lock;
    mz_zip_archive zip;
} ma_jar_archive;

struct ma_jar_vfs {
    // Same layout of ma_default_vfs, so this VFS can be passed to ma_default_vfs_* functions
    ma_vfs_callbacks cb;
    ma_allocation_callbacks allocationCallbacks;
    ma_log* pLog;
    ma_mutex archivesLock;
    ma_jar_archive** ppArchives;
    ma_uint32 archiveCount;
    ma_uint32 archiveCapacity;
};

typedef enum {
    jar_file_type_default = 0,
    jar_file_type_stored = 1,
    jar_file_type_memory = 2
} ma_jar_vfs_file_type;

typedef struct {
    ma_jar_vfs_file_type type;
    ma_vfs_file handle;         // jar_file_type_default
    ma_jar_archive* pArchive;   // jar_file_type_stored and jar_file_type_memory
    ma_uint64 dataOffset;       // jar_file_type_stored, absolute offset of entry data inside the archive
    void* pData;                // jar_file_type_memory, inflated entry
    ma_uint64 size;
    ma_uint64 cursor;
} ma_jar_vfs_file;

// Must be called with archive lock held
static ma_result ma_jar_archive_read_at(ma_jar_archive* pArchive, ma_uint64 offset, void* pDst, size_t sizeInBytes, size_t* pBytesRead) {
    *pBytesRead = 0;

    ma_result result = ma_default_vfs_seek((ma_vfs*) pArchive->pVFS, pArchive->handle, (ma_int64) offset, ma_seek_origin_start);
    if (result != MA_SUCCESS) return result;

    while (*pBytesRead < sizeInBytes) {
        size_t bytesRead = 0;
        result = ma_default_vfs_read((ma_vfs*) pArchive->pVFS, pArchive->handle, (ma_uint8*) pDst + *pBytesRead, sizeInBytes - *pBytesRead, &bytesRead);
        *pBytesRead += bytesRead;
        if (result != MA_SUCCESS || bytesRead == 0) break;
    }

    return (result == MA_AT_END) ? MA_SUCCESS : result;
}

static size_t ma_jar_archive_miniz_read(void* pOpaque, mz_uint64 offset, void* pBuf, size_t n) {
    size_t bytesRead;
    ma_jar_archive_read_at((ma_jar_archive*) pOpaque, offset, pBuf, n, &bytesRead);
    return bytesRead;
}

static ma_uint16 ma_jar_read_le16(const ma_uint8* p) {
    return (ma_uint16) (p[0] | (p[1] << 8));
}

static ma_uint32 ma_jar_read_le32(const ma_uint8* p) {
    return (ma_uint32) p[0] | ((ma_uint32) p[1] << 8) | ((ma_uint32) p[2] << 16) | ((ma_uint32) p[3] << 24);
}

// Returns MA_TRUE if pFilePath is a "jar:<index>:<entry>" path, ppEntry points to the entry name
static ma_bool32 ma_jar_vfs_parse_path(const char* pFilePath, ma_uint32* pIndex, const char** ppEntry) {
    if (strncmp(pFilePath, MA_JAR_VFS_PREFIX, MA_JAR_VFS_PREFIX_LEN) != 0) return MA_FALSE;

    const char* p = pFilePath + MA_JAR_VFS_PREFIX_LEN;
    ma_uint32 index = 0;
    if (*p < '0' || *p > '9') return MA_FALSE;
    while (*p >= '0' && *p <= '9') {
        index = index * 10 + (ma_uint32) (*p - '0');
        p++;
    }
    if (*p != ':') return MA_FALSE;

    *pIndex = index;
    *ppEntry = p + 1;
    return MA_TRUE;
}

static ma_bool32 ma_jar_vfs_parse_path_w(const wchar_t* pFilePath, ma_uint32* pIndex, const wchar_t** ppEntry) {
    const wchar_t* p = pFilePath;
    for (int i = 0; i < MA_JAR_VFS_PREFIX_LEN; i++) {
        if (p[i] != (wchar_t) MA_JAR_VFS_PREFIX[i]) return MA_FALSE;
    }
    p += MA_JAR_VFS_PREFIX_LEN;

    ma_uint32 index = 0;
    if (*p < L'0' || *p > L'9') return MA_FALSE;
    while (*p >= L'0' && *p <= L'9') {
        index = index * 10 + (ma_uint32) (*p - L'0');
        p++;
    }
    if (*p != L':') return MA_FALSE;

    *pIndex = index;
    *ppEntry = p + 1;
    return MA_TRUE;
}

// Zip entry names are UTF-8, wide paths (UTF-16 on Windows, UTF-32 elsewhere) must be converted
static char* ma_jar_vfs_wide_to_utf8(const wchar_t* pStr, const ma_allocation_callbacks* pAllocationCallbacks) {
    size_t length = wcslen(pStr);
    char* pUTF8 = (char*) ma_malloc(length * 4 + 1, pAllocationCallbacks);
    if (pUTF8 == NULL) return NULL;

    size_t o = 0;
    for (size_t i = 0; i < length; i++) {
        ma_uint32 c = (ma_uint32) pStr[i];
        if (sizeof(wchar_t) == 2 && c >= 0xD800 && c <= 0xDBFF && i + 1 < length) {
            ma_uint32 low = (ma_uint32) pStr[i + 1];
            if (low >= 0xDC00 && low <= 0xDFFF) {
                c = 0x10000 + ((c - 0xD800) << 10) + (low - 0xDC00);
                i++;
            }
        }
        if (c >= 0xD800 && c <= 0xDFFF) c = 0xFFFD;

        if (c < 0x80) {
            pUTF8[o++] = (char) c;
        } else if (c < 0x800) {
            pUTF8[o++] = (char) (0xC0 | (c >> 6));
            pUTF8[o++] = (char) (0x80 | (c & 0x3F));
        } else if (c < 0x10000) {
            pUTF8[o++] = (char) (0xE0 | (c >> 12));
            pUTF8[o++] = (char) (0x80 | ((c >> 6) & 0x3F));
            pUTF8[o++] = (char) (0x80 | (c & 0x3F));
        } else {
            pUTF8[o++] = (char) (0xF0 | (c >> 18));
            pUTF8[o++] = (char) (0x80 | ((c >> 12) & 0x3F));
            pUTF8[o++] = (char) (0x80 | ((c >> 6) & 0x3F));
            pUTF8[o++] = (char) (0x80 | (c & 0x3F));
        }
    }
    pUTF8[o] = '\0';
    return pUTF8;
}

static ma_jar_archive* ma_jar_vfs_get_archive(ma_jar_vfs* pVFS, ma_uint32 index) {
    ma_jar_archive* pArchive = NULL;
    ma_mutex_lock(&pVFS->archivesLock);
    if (index < pVFS->archiveCount) {
        pArchive = pVFS->ppArchives[index];
    }
    ma_mutex_unlock(&pVFS->archivesLock);
    return pArchive;
}

static ma_result ma_jar_vfs_open_entry(ma_jar_vfs* pVFS, ma_uint32 index, const char* pEntry, ma_jar_vfs_file* pFile) {
    ma_jar_archive* pArchive = ma_jar_vfs_get_archive(pVFS, index);
    if (pArchive == NULL) return MA_INVALID_ARGS;

    ma_result result = MA_SUCCESS;
    ma_mutex_lock(&pArchive->lock);

    int fileIndex = mz_zip_reader_locate_file(&pArchive->zip, pEntry, NULL, 0);
    mz_zip_archive_file_stat stat;
    if (fileIndex < 0 || !mz_zip_reader_file_stat(&pArchive->zip, (mz_uint) fileIndex, &stat) || stat.m_is_directory) {
        result = MA_DOES_NOT_EXIST;
    } else if (!stat.m_is_supported) {
        ma_log_postf(pVFS->pLog, MA_LOG_LEVEL_ERROR, "Jar entry %s is encrypted or uses an unsupported compression method.\n", pEntry);
        result = MA_NOT_IMPLEMENTED;
    } else if (stat.m_method == 0) {
        // STORED, entry data begins after the local header, which can have its own extra field
        ma_uint8 header[MA_ZIP_LOCAL_HEADER_SIZE];
        size_t bytesRead;
        result = ma_jar_archive_read_at(pArchive, stat.m_local_header_ofs, header, MA_ZIP_LOCAL_HEADER_SIZE, &bytesRead);
        if (result == MA_SUCCESS && (bytesRead != MA_ZIP_LOCAL_HEADER_SIZE || ma_jar_read_le32(header) != MA_ZIP_LOCAL_HEADER_SIGNATURE)) {
            result = MA_INVALID_FILE;
        }

        if (result == MA_SUCCESS) {
            ma_uint64 dataOffset = stat.m_local_header_ofs + MA_ZIP_LOCAL_HEADER_SIZE + ma_jar_read_le16(header + 26) + ma_jar_read_le16(header + 28);
            if (stat.m_comp_size != stat.m_uncomp_size || dataOffset + stat.m_comp_size > pArchive->zip.m_archive_size) {
                result = MA_INVALID_FILE;
            } else {
                pFile->type = jar_file_type_stored;
                pFile->pArchive = pArchive;
                pFile->dataOffset = dataOffset;
                pFile->size = stat.m_uncomp_size;
            }
        }

        if (result != MA_SUCCESS) {
            ma_log_postf(pVFS->pLog, MA_LOG_LEVEL_ERROR, "Jar entry %s has a corrupted local header.\n", pEntry);
        }
    } else {
        // DEFLATED, inflate the whole entry (CRC is checked by miniz)
        size_t size = 0;
        void* pData = mz_zip_reader_extract_to_heap(&pArchive->zip, (mz_uint) fileIndex, &size, 0);
        if (pData == NULL) {
            ma_log_postf(pVFS->pLog, MA_LOG_LEVEL_ERROR, "Unable to inflate jar entry %s: %s.\n", pEntry,
                         mz_zip_get_error_string(mz_zip_get_last_error(&pArchive->zip)));
            result = MA_ERROR;
        } else {
            pFile->type = jar_file_type_memory;
            pFile->pArchive = pArchive;
            pFile->pData = pData;
            pFile->size = size;
        }
    }

    ma_mutex_unlock(&pArchive->lock);
    return result;
}

static ma_result ma_jar_vfs_open_internal(ma_vfs* pVFS, const char* pFilePath, const wchar_t* pFilePathW, ma_uint32 openMode, ma_vfs_file* pFile) {
    if (pFile == NULL) return MA_INVALID_ARGS;
    *pFile = NULL;
    if ((pFilePath == NULL && pFilePathW == NULL) || openMode == 0) return MA_INVALID_ARGS;

    ma_jar_vfs* jarVfs = (ma_jar_vfs*) pVFS;
    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) ma_calloc(sizeof(ma_jar_vfs_file), &jarVfs->allocationCallbacks);
    if (wrapperFile == NULL) return MA_OUT_OF_MEMORY;

    ma_result result;
    ma_uint32 index;
    if (pFilePath != NULL) {
        const char* pEntry;
        if (ma_jar_vfs_parse_path(pFilePath, &index, &pEntry)) {
            result = (openMode & MA_OPEN_MODE_WRITE) != 0 ? MA_ACCESS_DENIED : ma_jar_vfs_open_entry(jarVfs, index, pEntry, wrapperFile);
        } else {
            wrapperFile->type = jar_file_type_default;
            result = ma_default_vfs_open(pVFS, pFilePath, openMode, &wrapperFile->handle);
        }
    } else {
        const wchar_t* pEntryW;
        if (ma_jar_vfs_parse_path_w(pFilePathW, &index, &pEntryW)) {
            if ((openMode & MA_OPEN_MODE_WRITE) != 0) {
                result = MA_ACCESS_DENIED;
            } else {
                char* pEntry = ma_jar_vfs_wide_to_utf8(pEntryW, &jarVfs->allocationCallbacks);
                result = pEntry == NULL ? MA_OUT_OF_MEMORY : ma_jar_vfs_open_entry(jarVfs, index, pEntry, wrapperFile);
                ma_free(pEntry, &jarVfs->allocationCallbacks);
            }
        } else {
            wrapperFile->type = jar_file_type_default;
            result = ma_default_vfs_open_w(pVFS, pFilePathW, openMode, &wrapperFile->handle);
        }
    }

    if (result != MA_SUCCESS) {
        ma_free(wrapperFile, &jarVfs->allocationCallbacks);
        return result;
    }

    *pFile = (ma_vfs_file) wrapperFile;
    return MA_SUCCESS;
}

ma_result ma_jar_vfs_open(ma_vfs* pVFS, const char* pFilePath, ma_uint32 openMode, ma_vfs_file* pFile) {
    return ma_jar_vfs_open_internal(pVFS, pFilePath, NULL, openMode, pFile);
}

ma_result ma_jar_vfs_open_w(ma_vfs* pVFS, const wchar_t* pFilePath, ma_uint32 openMode, ma_vfs_file* pFile) {
    return ma_jar_vfs_open_internal(pVFS, NULL, pFilePath, openMode, pFile);
}

ma_result ma_jar_vfs_close(ma_vfs* pVFS, ma_vfs_file file) {
    if (file == NULL) return MA_INVALID_ARGS;

    ma_jar_vfs* jarVfs = (ma_jar_vfs*) pVFS;
    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) file;

    ma_result result = MA_SUCCESS;
    if (wrapperFile->type == jar_file_type_default) {
        result = ma_default_vfs_close(pVFS, wrapperFile->handle);
    } else if (wrapperFile->type == jar_file_type_memory) {
        mz_zip_archive* pZip = &wrapperFile->pArchive->zip;
        pZip->m_pFree(pZip->m_pAlloc_opaque, wrapperFile->pData);
    }

    ma_free(wrapperFile, &jarVfs->allocationCallbacks);
    return result;
}

ma_result ma_jar_vfs_read(ma_vfs* pVFS, ma_vfs_file file, void* pDst, size_t sizeInBytes, size_t* pBytesRead) {
    if (pBytesRead != NULL) *pBytesRead = 0;
    if (file == NULL || pDst == NULL) return MA_INVALID_ARGS;

    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) file;
    if (wrapperFile->type == jar_file_type_default) {
        return ma_default_vfs_read(pVFS, wrapperFile->handle, pDst, sizeInBytes, pBytesRead);
    }

    if (wrapperFile->cursor >= wrapperFile->size) return MA_AT_END;

    ma_uint64 remaining = wrapperFile->size - wrapperFile->cursor;
    size_t bytesToRead = remaining < sizeInBytes ? (size_t) remaining : sizeInBytes;
    size_t bytesRead = 0;

    if (wrapperFile->type == jar_file_type_memory) {
        MA_COPY_MEMORY(pDst, (ma_uint8*) wrapperFile->pData + wrapperFile->cursor, bytesToRead);
        bytesRead = bytesToRead;
    } else {
        ma_jar_archive* pArchive = wrapperFile->pArchive;
        ma_mutex_lock(&pArchive->lock);
        ma_result result = ma_jar_archive_read_at(pArchive, wrapperFile->dataOffset + wrapperFile->cursor, pDst, bytesToRead, &bytesRead);
        ma_mutex_unlock(&pArchive->lock);
        if (result != MA_SUCCESS) return result;
    }

    wrapperFile->cursor += bytesRead;
    if (pBytesRead != NULL) *pBytesRead = bytesRead;
    return bytesRead == 0 ? MA_AT_END : MA_SUCCESS;
}

ma_result ma_jar_vfs_write(ma_vfs* pVFS, ma_vfs_file file, const void* pSrc, size_t sizeInBytes, size_t* pBytesWritten) {
    if (pBytesWritten != NULL) *pBytesWritten = 0;
    if (file == NULL) return MA_INVALID_ARGS;

    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) file;
    if (wrapperFile->type == jar_file_type_default) {
        return ma_default_vfs_write(pVFS, wrapperFile->handle, pSrc, sizeInBytes, pBytesWritten);
    }

    // Jar entries are read-only
    return MA_ACCESS_DENIED;
}

ma_result ma_jar_vfs_seek(ma_vfs* pVFS, ma_vfs_file file, ma_int64 offset, ma_seek_origin origin) {
    if (file == NULL) return MA_INVALID_ARGS;

    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) file;
    if (wrapperFile->type == jar_file_type_default) {
        return ma_default_vfs_seek(pVFS, wrapperFile->handle, offset, origin);
    }

    ma_int64 base;
    if (origin == ma_seek_origin_start) {
        base = 0;
    } else if (origin == ma_seek_origin_end) {
        base = (ma_int64) wrapperFile->size;
    } else {
        base = (ma_int64) wrapperFile->cursor;
    }

    // Seeking past the end is allowed like with regular files, next read will report MA_AT_END
    ma_int64 newCursor = base + offset;
    if (newCursor < 0) return MA_BAD_SEEK;

    wrapperFile->cursor = (ma_uint64) newCursor;
    return MA_SUCCESS;
}

ma_result ma_jar_vfs_tell(ma_vfs* pVFS, ma_vfs_file file, ma_int64* pCursor) {
    if (pCursor == NULL) return MA_INVALID_ARGS;
    *pCursor = 0;
    if (file == NULL) return MA_INVALID_ARGS;

    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) file;
    if (wrapperFile->type == jar_file_type_default) {
        return ma_default_vfs_tell(pVFS, wrapperFile->handle, pCursor);
    }

    *pCursor = (ma_int64) wrapperFile->cursor;
    return MA_SUCCESS;
}

ma_result ma_jar_vfs_info(ma_vfs* pVFS, ma_vfs_file file, ma_file_info* pInfo) {
    if (pInfo == NULL) return MA_INVALID_ARGS;
    MA_ZERO_OBJECT(pInfo);
    if (file == NULL) return MA_INVALID_ARGS;

    ma_jar_vfs_file* wrapperFile = (ma_jar_vfs_file*) file;
    if (wrapperFile->type == jar_file_type_default) {
        return ma_default_vfs_info(pVFS, wrapperFile->handle, pInfo);
    }

    pInfo->sizeInBytes = wrapperFile->size;
    return MA_SUCCESS;
}

ma_result ma_jar_vfs_init(ma_jar_vfs* pVFS, const ma_allocation_callbacks* pAllocationCallbacks) {
    if (pVFS == NULL) return MA_INVALID_ARGS;
    MA_ZERO_OBJECT(pVFS);

    pVFS->cb.onOpen  = ma_jar_vfs_open;
    pVFS->cb.onOpenW = ma_jar_vfs_open_w;
    pVFS->cb.onClose = ma_jar_vfs_close;
    pVFS->cb.onRead  = ma_jar_vfs_read;
    pVFS->cb.onWrite = ma_jar_vfs_write;
    pVFS->cb.onSeek  = ma_jar_vfs_seek;
    pVFS->cb.onTell  = ma_jar_vfs_tell;
    pVFS->cb.onInfo  = ma_jar_vfs_info;

    ma_allocation_callbacks_init_copy(&pVFS->allocationCallbacks, pAllocationCallbacks);

    return ma_mutex_init(&pVFS->archivesLock);
}

// Opens a jar archive and parses its central directory. Exactly one of pPath and pPathW must be set.
// On success pIndex receives the value to use in "jar:<index>:<entry>" paths.
ma_result ma_jar_vfs_register_archive(ma_jar_vfs* pVFS, const char* pPath, const wchar_t* pPathW, ma_uint32* pIndex) {
    if (pVFS == NULL || (pPath == NULL && pPathW == NULL) || pIndex == NULL) return MA_INVALID_ARGS;

    ma_jar_archive* pArchive = (ma_jar_archive*) ma_calloc(sizeof(ma_jar_archive), &pVFS->allocationCallbacks);
    if (pArchive == NULL) return MA_OUT_OF_MEMORY;
    pArchive->pVFS = pVFS;

    ma_result result = pPath != NULL
            ? ma_default_vfs_open((ma_vfs*) pVFS, pPath, MA_OPEN_MODE_READ, &pArchive->handle)
            : ma_default_vfs_open_w((ma_vfs*) pVFS, pPathW, MA_OPEN_MODE_READ, &pArchive->handle);
    if (result != MA_SUCCESS) {
        ma_free(pArchive, &pVFS->allocationCallbacks);
        return result;
    }

    ma_file_info info;
    result = ma_default_vfs_info((ma_vfs*) pVFS, pArchive->handle, &info);
    if (result == MA_SUCCESS) {
        result = ma_mutex_init(&pArchive->lock);
    }
    if (result != MA_SUCCESS) {
        ma_default_vfs_close((ma_vfs*) pVFS, pArchive->handle);
        ma_free(pArchive, &pVFS->allocationCallbacks);
        return result;
    }

    mz_zip_zero_struct(&pArchive->zip);
    pArchive->zip.m_pRead = ma_jar_archive_miniz_read;
    pArchive->zip.m_pIO_opaque = pArchive;

    ma_mutex_lock(&pArchive->lock);
    mz_bool zipOk = mz_zip_reader_init(&pArchive->zip, info.sizeInBytes, 0);
    ma_mutex_unlock(&pArchive->lock);

    if (!zipOk) {
        ma_log_postf(pVFS->pLog, MA_LOG_LEVEL_ERROR, "Unable to open jar archive: %s.\n",
                     mz_zip_get_error_string(mz_zip_get_last_error(&pArchive->zip)));
        ma_mutex_uninit(&pArchive->lock);
        ma_default_vfs_close((ma_vfs*) pVFS, pArchive->handle);
        ma_free(pArchive, &pVFS->allocationCallbacks);
        return MA_INVALID_FILE;
    }

    ma_mutex_lock(&pVFS->archivesLock);
    if (pVFS->archiveCount == pVFS->archiveCapacity) {
        ma_uint32 newCapacity = pVFS->archiveCapacity == 0 ? 4 : pVFS->archiveCapacity * 2;
        ma_jar_archive** ppNewArchives = (ma_jar_archive**) ma_realloc(pVFS->ppArchives, sizeof(ma_jar_archive*) * newCapacity, &pVFS->allocationCallbacks);
        if (ppNewArchives == NULL) {
            result = MA_OUT_OF_MEMORY;
        } else {
            pVFS->ppArchives = ppNewArchives;
            pVFS->archiveCapacity = newCapacity;
        }
    }
    if (result == MA_SUCCESS) {
        *pIndex = pVFS->archiveCount;
        pVFS->ppArchives[pVFS->archiveCount++] = pArchive;
    }
    ma_mutex_unlock(&pVFS->archivesLock);

    if (result != MA_SUCCESS) {
        mz_zip_reader_end(&pArchive->zip);
        ma_mutex_uninit(&pArchive->lock);
        ma_default_vfs_close((ma_vfs*) pVFS, pArchive->handle);
        ma_free(pArchive, &pVFS->allocationCallbacks);
    }
    return result;
}

// Must be called after the resource manager has been uninitialized
void ma_jar_vfs_uninit(ma_jar_vfs* pVFS) {
    if (pVFS == NULL) return;

    for (ma_uint32 i = 0; i < pVFS->archiveCount; i++) {
        ma_jar_archive* pArchive = pVFS->ppArchives[i];
        mz_zip_reader_end(&pArchive->zip);
        ma_mutex_uninit(&pArchive->lock);
        ma_default_vfs_close((ma_vfs*) pVFS, pArchive->handle);
        ma_free(pArchive, &pVFS->allocationCallbacks);
    }
    ma_free(pVFS->ppArchives, &pVFS->allocationCallbacks);
    pVFS->ppArchives = NULL;
    pVFS->archiveCount = 0;
    pVFS->archiveCapacity = 0;

    ma_mutex_uninit(&pVFS->archivesLock);
}

#endif // MINIAUDIO_JAR_VFS_H
