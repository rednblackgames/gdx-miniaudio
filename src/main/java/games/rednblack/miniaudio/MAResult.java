package games.rednblack.miniaudio;

/**
 * Status codes from MiniAudio native library.
 *
 * @author fgnm
 */
public class MAResult {
    //MiniAudio Results
    public static final int MA_SUCCESS = 0,
            MA_ERROR = -1,  /* A generic error. */
            MA_INVALID_ARGS = -2,
            MA_INVALID_OPERATION = -3,
            MA_OUT_OF_MEMORY = -4,
            MA_OUT_OF_RANGE = -5,
            MA_ACCESS_DENIED = -6,
            MA_DOES_NOT_EXIST = -7,
            MA_ALREADY_EXISTS = -8,
            MA_TOO_MANY_OPEN_FILES = -9,
            MA_INVALID_FILE = -10,
            MA_TOO_BIG = -11,
            MA_PATH_TOO_LONG = -12,
            MA_NAME_TOO_LONG = -13,
            MA_NOT_DIRECTORY = -14,
            MA_IS_DIRECTORY = -15,
            MA_DIRECTORY_NOT_EMPTY = -16,
            MA_AT_END = -17,
            MA_NO_SPACE = -18,
            MA_BUSY = -19,
            MA_IO_ERROR = -20,
            MA_INTERRUPT = -21,
            MA_UNAVAILABLE = -22,
            MA_ALREADY_IN_USE = -23,
            MA_BAD_ADDRESS = -24,
            MA_BAD_SEEK = -25,
            MA_BAD_PIPE = -26,
            MA_DEADLOCK = -27,
            MA_TOO_MANY_LINKS = -28,
            MA_NOT_IMPLEMENTED = -29,
            MA_NO_MESSAGE = -30,
            MA_BAD_MESSAGE = -31,
            MA_NO_DATA_AVAILABLE = -32,
            MA_INVALID_DATA = -33,
            MA_TIMEOUT = -34,
            MA_NO_NETWORK = -35,
            MA_NOT_UNIQUE = -36,
            MA_NOT_SOCKET = -37,
            MA_NO_ADDRESS = -38,
            MA_BAD_PROTOCOL = -39,
            MA_PROTOCOL_UNAVAILABLE = -40,
            MA_PROTOCOL_NOT_SUPPORTED = -41,
            MA_PROTOCOL_FAMILY_NOT_SUPPORTED = -42,
            MA_ADDRESS_FAMILY_NOT_SUPPORTED = -43,
            MA_SOCKET_NOT_SUPPORTED = -44,
            MA_CONNECTION_RESET = -45,
            MA_ALREADY_CONNECTED = -46,
            MA_NOT_CONNECTED = -47,
            MA_CONNECTION_REFUSED = -48,
            MA_NO_HOST = -49,
            MA_IN_PROGRESS = -50,
            MA_CANCELLED = -51,
            MA_MEMORY_ALREADY_MAPPED = -52,

    /* General non-standard errors. */
    MA_CRC_MISMATCH = -100,

    /* General miniaudio-specific errors. */
    MA_FORMAT_NOT_SUPPORTED = -200,
            MA_DEVICE_TYPE_NOT_SUPPORTED = -201,
            MA_SHARE_MODE_NOT_SUPPORTED = -202,
            MA_NO_BACKEND = -203,
            MA_NO_DEVICE = -204,
            MA_API_NOT_FOUND = -205,
            MA_INVALID_DEVICE_CONFIG = -206,
            MA_LOOP = -207,
            MA_BACKEND_NOT_ENABLED = -208,

    /* State errors. */
    MA_DEVICE_NOT_INITIALIZED = -300,
            MA_DEVICE_ALREADY_INITIALIZED = -301,
            MA_DEVICE_NOT_STARTED = -302,
            MA_DEVICE_NOT_STOPPED = -303,

    /* Operation errors. */
    MA_FAILED_TO_INIT_BACKEND = -400,
            MA_FAILED_TO_OPEN_BACKEND_DEVICE = -401,
            MA_FAILED_TO_START_BACKEND_DEVICE = -402,
            MA_FAILED_TO_STOP_BACKEND_DEVICE = -403;

    /**
     * To simplify JNI communication if an error occurs in native code it's passed instead of the memory address.
     * This function check if the address is a real memory address or just an error.
     *
     * @param address native memory address to be checked
     * @return true if the address is an error, false otherwise
     */
    public static boolean checkErrors(long address) {
        return address >= MAResult.MA_FAILED_TO_STOP_BACKEND_DEVICE && address <= MAResult.MA_ERROR;
    }

    public static boolean checkErrors(float value) {
        return checkErrors((long) value);
    }

    public static String getErrorHumanDescription(int error) {
        switch (error) {
            case MA_ERROR:                         return "Generic error";
            case MA_INVALID_ARGS:                  return "Invalid argument";
            case MA_INVALID_OPERATION:             return "Invalid operation";
            case MA_OUT_OF_MEMORY:                 return "Out of memory";
            case MA_OUT_OF_RANGE:                  return "Out of range";
            case MA_ACCESS_DENIED:                 return "Permission denied";
            case MA_DOES_NOT_EXIST:                return "Resource does not exist";
            case MA_ALREADY_EXISTS:                return "Resource already exists";
            case MA_TOO_MANY_OPEN_FILES:           return "Too many open files";
            case MA_INVALID_FILE:                  return "Invalid file";
            case MA_TOO_BIG:                       return "Too large";
            case MA_PATH_TOO_LONG:                 return "Path too long";
            case MA_NAME_TOO_LONG:                 return "Name too long";
            case MA_NOT_DIRECTORY:                 return "Not a directory";
            case MA_IS_DIRECTORY:                  return "Is a directory";
            case MA_DIRECTORY_NOT_EMPTY:           return "Directory not empty";
            case MA_AT_END:                        return "At end";
            case MA_NO_SPACE:                      return "No space available";
            case MA_BUSY:                          return "Device or resource busy";
            case MA_IO_ERROR:                      return "Input/output error";
            case MA_INTERRUPT:                     return "Interrupted";
            case MA_UNAVAILABLE:                   return "Resource unavailable";
            case MA_ALREADY_IN_USE:                return "Resource already in use";
            case MA_BAD_ADDRESS:                   return "Bad address";
            case MA_BAD_SEEK:                      return "Illegal seek";
            case MA_BAD_PIPE:                      return "Broken pipe";
            case MA_DEADLOCK:                      return "Deadlock";
            case MA_TOO_MANY_LINKS:                return "Too many links";
            case MA_NOT_IMPLEMENTED:               return "Not implemented";
            case MA_NO_MESSAGE:                    return "No message of desired type";
            case MA_BAD_MESSAGE:                   return "Invalid message";
            case MA_NO_DATA_AVAILABLE:             return "No data available";
            case MA_INVALID_DATA:                  return "Invalid data";
            case MA_TIMEOUT:                       return "Timeout";
            case MA_NO_NETWORK:                    return "Network unavailable";
            case MA_NOT_UNIQUE:                    return "Not unique";
            case MA_NOT_SOCKET:                    return "Socket operation on non-socket";
            case MA_NO_ADDRESS:                    return "Destination address required";
            case MA_BAD_PROTOCOL:                  return "Protocol wrong type for socket";
            case MA_PROTOCOL_UNAVAILABLE:          return "Protocol not available";
            case MA_PROTOCOL_NOT_SUPPORTED:        return "Protocol not supported";
            case MA_PROTOCOL_FAMILY_NOT_SUPPORTED: return "Protocol family not supported";
            case MA_ADDRESS_FAMILY_NOT_SUPPORTED:  return "Address family not supported";
            case MA_SOCKET_NOT_SUPPORTED:          return "Socket type not supported";
            case MA_CONNECTION_RESET:              return "Connection reset";
            case MA_ALREADY_CONNECTED:             return "Already connected";
            case MA_NOT_CONNECTED:                 return "Not connected";
            case MA_CONNECTION_REFUSED:            return "Connection refused";
            case MA_NO_HOST:                       return "No host";
            case MA_IN_PROGRESS:                   return "Operation in progress";
            case MA_CANCELLED:                     return "Operation cancelled";
            case MA_MEMORY_ALREADY_MAPPED:         return "Memory already mapped";

            case MA_FORMAT_NOT_SUPPORTED:          return "Format not supported";
            case MA_DEVICE_TYPE_NOT_SUPPORTED:     return "Device type not supported";
            case MA_SHARE_MODE_NOT_SUPPORTED:      return "Share mode not supported";
            case MA_NO_BACKEND:                    return "No backend";
            case MA_NO_DEVICE:                     return "No device";
            case MA_API_NOT_FOUND:                 return "API not found";
            case MA_INVALID_DEVICE_CONFIG:         return "Invalid device config";

            case MA_DEVICE_NOT_INITIALIZED:        return "Device not initialized";
            case MA_DEVICE_NOT_STARTED:            return "Device not started";

            case MA_FAILED_TO_INIT_BACKEND:        return "Failed to initialize backend";
            case MA_FAILED_TO_OPEN_BACKEND_DEVICE: return "Failed to open backend device";
            case MA_FAILED_TO_START_BACKEND_DEVICE: return "Failed to start backend device";
            case MA_FAILED_TO_STOP_BACKEND_DEVICE: return "Failed to stop backend device";

            default:                               return "Unknown error";
        }
    }
}
