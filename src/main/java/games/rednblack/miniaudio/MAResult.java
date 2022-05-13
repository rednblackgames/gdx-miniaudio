package games.rednblack.miniaudio;

/**
 * Status codes from MiniAudio native library.
 *
 * @author fgnm
 */
public class MAResult {
    //MiniAudio Results
    public static final int MA_SUCCESS                        =  0,
    MA_ERROR                          = -1,  /* A generic error. */
    MA_INVALID_ARGS                   = -2,
    MA_INVALID_OPERATION              = -3,
    MA_OUT_OF_MEMORY                  = -4,
    MA_OUT_OF_RANGE                   = -5,
    MA_ACCESS_DENIED                  = -6,
    MA_DOES_NOT_EXIST                 = -7,
    MA_ALREADY_EXISTS                 = -8,
    MA_TOO_MANY_OPEN_FILES            = -9,
    MA_INVALID_FILE                   = -10,
    MA_TOO_BIG                        = -11,
    MA_PATH_TOO_LONG                  = -12,
    MA_NAME_TOO_LONG                  = -13,
    MA_NOT_DIRECTORY                  = -14,
    MA_IS_DIRECTORY                   = -15,
    MA_DIRECTORY_NOT_EMPTY            = -16,
    MA_AT_END                         = -17,
    MA_NO_SPACE                       = -18,
    MA_BUSY                           = -19,
    MA_IO_ERROR                       = -20,
    MA_INTERRUPT                      = -21,
    MA_UNAVAILABLE                    = -22,
    MA_ALREADY_IN_USE                 = -23,
    MA_BAD_ADDRESS                    = -24,
    MA_BAD_SEEK                       = -25,
    MA_BAD_PIPE                       = -26,
    MA_DEADLOCK                       = -27,
    MA_TOO_MANY_LINKS                 = -28,
    MA_NOT_IMPLEMENTED                = -29,
    MA_NO_MESSAGE                     = -30,
    MA_BAD_MESSAGE                    = -31,
    MA_NO_DATA_AVAILABLE              = -32,
    MA_INVALID_DATA                   = -33,
    MA_TIMEOUT                        = -34,
    MA_NO_NETWORK                     = -35,
    MA_NOT_UNIQUE                     = -36,
    MA_NOT_SOCKET                     = -37,
    MA_NO_ADDRESS                     = -38,
    MA_BAD_PROTOCOL                   = -39,
    MA_PROTOCOL_UNAVAILABLE           = -40,
    MA_PROTOCOL_NOT_SUPPORTED         = -41,
    MA_PROTOCOL_FAMILY_NOT_SUPPORTED  = -42,
    MA_ADDRESS_FAMILY_NOT_SUPPORTED   = -43,
    MA_SOCKET_NOT_SUPPORTED           = -44,
    MA_CONNECTION_RESET               = -45,
    MA_ALREADY_CONNECTED              = -46,
    MA_NOT_CONNECTED                  = -47,
    MA_CONNECTION_REFUSED             = -48,
    MA_NO_HOST                        = -49,
    MA_IN_PROGRESS                    = -50,
    MA_CANCELLED                      = -51,
    MA_MEMORY_ALREADY_MAPPED          = -52,

    /* General miniaudio-specific errors. */
    MA_FORMAT_NOT_SUPPORTED           = -100,
    MA_DEVICE_TYPE_NOT_SUPPORTED      = -101,
    MA_SHARE_MODE_NOT_SUPPORTED       = -102,
    MA_NO_BACKEND                     = -103,
    MA_NO_DEVICE                      = -104,
    MA_API_NOT_FOUND                  = -105,
    MA_INVALID_DEVICE_CONFIG          = -106,
    MA_LOOP                           = -107,

    /* State errors. */
    MA_DEVICE_NOT_INITIALIZED         = -200,
    MA_DEVICE_ALREADY_INITIALIZED     = -201,
    MA_DEVICE_NOT_STARTED             = -202,
    MA_DEVICE_NOT_STOPPED             = -203,

    /* Operation errors. */
    MA_FAILED_TO_INIT_BACKEND         = -300,
    MA_FAILED_TO_OPEN_BACKEND_DEVICE  = -301,
    MA_FAILED_TO_START_BACKEND_DEVICE = -302,
    MA_FAILED_TO_STOP_BACKEND_DEVICE  = -303;
}
