package games.rednblack.miniaudio;

import com.badlogic.gdx.utils.Disposable;

/**
 * Abstract wrapper to node element in the graph effects.
 *
 * @author fgnm
 */
public abstract class MANode implements Disposable {
    protected long address;
    protected final MiniAudio miniAudio;

    public MANode (MiniAudio miniAudio) {
        this.miniAudio = miniAudio;
    }

    /**
     * Get the native memory address for this node
     *
     * @return long that represents the native memory address
     */
    public long getAddress() {
        return address;
    }

    /**
     * Attach to the input of this node another {@link MANode} object
     *
     * @param previousNode the node that should be attached to this node input
     * @param outputBus the output bus index of the node
     */
    public abstract void attachToNode(MANode previousNode, int outputBus);

    /**
     * Returns how many output bus has this node.
     *
     * @return autput bus for this node
     */
    public abstract int getSupportedOutputs();
}
