package games.rednblack.miniaudio.loader;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import games.rednblack.miniaudio.MAAudioBuffer;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;

/**
 *  {@link AssetLoader} to load {@link MASound} instances
 *
 * @author fgnm
 */
public class MASoundLoader extends AsynchronousAssetLoader<MASound, MASoundLoaderParameters> {
    private final MiniAudio miniAudio;
    private MASound sound;

    public MASoundLoader(MiniAudio ma, FileHandleResolver resolver) {
        super(resolver);
        if (ma == null) throw new IllegalArgumentException("MiniAudio instance cannot be null");
        miniAudio = ma;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, MASoundLoaderParameters parameter) {
        if (parameter != null) {
            if (parameter.loadFromMemory) {
                byte[] data = file.readBytes();
                MAAudioBuffer decodedBuffer = miniAudio.decodeBytes(data, 2);
                sound = miniAudio.createSound(decodedBuffer, parameter.flags, parameter.maGroup);
                sound.setLinkedAudioBuffer(decodedBuffer);
            } else {
                sound = miniAudio.createSound(file.path(), parameter.flags, parameter.maGroup, parameter.external);
            }
        } else {
            //Native library cannot access internal jar resources, desktop backends must read bytes and fully decode
            //audio in memory to avoid extraction
            if (Gdx.app.getType() == Application.ApplicationType.Desktop &&
                    (file.type() == Files.FileType.Classpath || file.type() == Files.FileType.Internal)) {

                Gdx.app.error(MiniAudio.TAG, file.name() + ": attempt to load Classpath/Internal asset from desktop. Reading from memory...");

                byte[] data = file.readBytes();
                MAAudioBuffer decodedBuffer = miniAudio.decodeBytes(data, 2);
                sound = miniAudio.createSound(decodedBuffer);
                sound.setLinkedAudioBuffer(decodedBuffer);
            } else {
                sound = miniAudio.createSound(file.path());
            }
        }
    }

    @Override
    public MASound loadSync(AssetManager manager, String fileName, FileHandle file, MASoundLoaderParameters parameter) {
        MASound sound = this.sound;
        this.sound = null;
        return sound;
    }

    @Override
    public void unloadAsync(AssetManager manager, String fileName, FileHandle file, MASoundLoaderParameters parameter) {
        MASound sound = this.sound;
        if (sound != null)
            sound.dispose();
        this.sound = null;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, MASoundLoaderParameters parameter) {
        return null;
    }
}
