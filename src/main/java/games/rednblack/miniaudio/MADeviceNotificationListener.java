package games.rednblack.miniaudio;

/**
 * Listener to get notified when the device change its state.
 *
 * NOTE: listener is called on MiniAudio's thread.
 *
 * @author fgnm
 */
public interface MADeviceNotificationListener {
    void onNotification(MADeviceNotificationType type);
}
