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
     * @param outputBusIndex the output bus index of the node
     */
    public void attachToThisNode(MANode previousNode, int outputBusIndex) {
        if (outputBusIndex >= previousNode.getSupportedOutputs())
            throw new IllegalArgumentException("Wrong output bus number, the node support up to " + previousNode.getSupportedOutputs() + " buses.");

        miniAudio.attachOutputBus(previousNode, outputBusIndex, this, 0);
    }

    /**
     * Detach this node in effects graph. If you want to just move the output bus from
     * one attachment to another, you do not need to detach first. You can just call {@link #attachToThisNode} and it'll
     * deal with it for you.
     *
     * @param outputBusIndex output bus index of the node
     */
    public void detach(int outputBusIndex) {
        if (outputBusIndex >= getSupportedOutputs())
            throw new IllegalArgumentException("Wrong output bus number, the node support up to " + getSupportedOutputs() + " buses.");

        miniAudio.detachOutputBus(this, outputBusIndex);
    }

    /**
     * Returns how many output bus has this node.
     *
     * @return autput bus for this node
     */
    public abstract int getSupportedOutputs();
}
