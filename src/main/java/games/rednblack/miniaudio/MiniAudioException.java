package games.rednblack.miniaudio;

/**
 * Custom Exception to handle errors code from MiniAudio native code
 *
 * @author fgnm
 */
public class MiniAudioException extends RuntimeException {
    private final int maResult;

    public MiniAudioException(String errorMessage, int maResult) {
        super(errorMessage);
        this.maResult = maResult;
    }

    /**
     * Get error code produced by native code, refer to {@link MAResult}
     *
     * @return ma_result integer code
     */
    public int getMAResult() {
        return maResult;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " [Code " + maResult + "]";
    }
}
