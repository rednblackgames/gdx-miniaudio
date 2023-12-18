package games.rednblack.miniaudio;

/**
 * Listener to get notified when native code generate some kind of logs.
 *
 * NOTE: this callback may be invoked on different threads.
 *
 * @author fgnm
 */
public interface MALogCallback {
    void onLog(MALogLevel level, String message);
}
