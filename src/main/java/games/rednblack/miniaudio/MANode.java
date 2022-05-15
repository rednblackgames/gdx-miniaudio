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
    public void attachToNode(MANode previousNode, int outputBusIndex) {
        if (outputBusIndex >= previousNode.getSupportedOutputs())
            throw new IllegalArgumentException("Wrong output bus number, the node support up to " + previousNode.getSupportedOutputs() + " buses.");

        miniAudio.attachOutputBus(previousNode, outputBusIndex, this, 0);
    }

    /**
     * Returns how many output bus has this node.
     *
     * @return autput bus for this node
     */
    public abstract int getSupportedOutputs();
}
