package games.rednblack.miniaudio;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Logger;
import games.rednblack.miniaudio.effect.MAReverbNode;
import games.rednblack.miniaudio.filter.MALowPassFilter;
import games.rednblack.miniaudio.mix.MAChannelCombiner;
import games.rednblack.miniaudio.mix.MAChannelSeparator;
import games.rednblack.miniaudio.effect.MADelayNode;
import games.rednblack.miniaudio.loader.MASoundLoader;
import games.rednblack.miniaudio.mix.MALTrim;
import games.rednblack.miniaudio.mix.MASplitter;

public class Main implements ApplicationListener {

    MiniAudio miniAudio;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.disableAudio(true);
        new Lwjgl3Application(new Main(), configuration);
    }

    MASound maSound;
    MAGroup maGroup;
    MANode effectNode;

    MALowPassFilter lowPassFilter;

    AssetManager assetManager;
    @Override
    public void create() {
        //miniAudio = new MiniAudio(1, 1, 0, 256, 44100);
        miniAudio = new MiniAudio();
        miniAudio.setDeviceNotificationListener(new MADeviceNotificationListener() {
            @Override
            public void onNotification(MADeviceNotificationType type) {
                System.out.println(type);
            }
        });
        MADeviceInfo[] devices = miniAudio.enumerateDevices();
        for (MADeviceInfo info : devices) {
            System.out.println(info.isCapture + " " + info.idAddress + " . " + info.name);
        }

        byte[] data = Gdx.files.internal("piano2.wav").readBytes();
        System.out.println("data length: " + data.length);
        MAAudioBuffer decodedBuffer = miniAudio.decodeBytes(data, data.length * 2, 2);
       /* miniAudio.initEngine(1, -1, -1, 2, 0, 512, 44100, MAFormatType.F32,false, false, false);
        miniAudio.setListenerDirection(0, 0, 1);
        miniAudio.setListenerCone(MathUtils.PI / 4f, MathUtils.PI / 4f, 2f);*/

        maGroup = miniAudio.createGroup();
        maGroup.setSpatialization(true);
        //int res = miniAudio.playSound("Median_test.ogg");
        //int res = miniAudio.playSound("piano2.wav");
        //System.out.println(res);
        //maSound = miniAudio.createSound("Median_test.ogg");
        maSound = miniAudio.createSound(decodedBuffer);
        //maSound.setPositioning(MAPositioning.RELATIVE);
        //maSound = miniAudio.createInputSound((short) 0, null);
        //maSound.setSpatialization(false);

        //effectNode = new MADelayNode(miniAudio, 0.25f, 0.45f, 1);
        effectNode = new MAReverbNode(miniAudio);
        //effectNode = new MALTrim(miniAudio, 0);
        //effectNode = new MABiquadFilter(miniAudio, .0102f, .0105f, .011f, .109f, .01047f, .1028f);
        //miniAudio.attachToEngineOutput(effectNode, 0);
        //effectNode.attachToThisNode(maSound, 0);

        /*effectNode = new MADelayNode(miniAudio, 0.25f, 0.45f, 1);
        MAChannelSeparator channelSeparator = new MAChannelSeparator(miniAudio, 2);
        MAChannelCombiner channelCombiner = new MAChannelCombiner(miniAudio, 2);

        miniAudio.attachToEngineOutput(channelCombiner, 0);
        channelCombiner.attachToThisNode(channelSeparator, 0, 0);
        channelCombiner.attachToThisNode(effectNode, 0, 1);
        effectNode.attachToThisNode(channelSeparator, 1);
        channelSeparator.attachToThisNode(maSound, 0);*/


        /*MASplitter splitter = new MASplitter(miniAudio);
        MALowPassFilter lowPassFilter = new MALowPassFilter(miniAudio, 550, 8);
        MADelayNode delayNode = new MADelayNode(miniAudio, 0.25f, 0.45f);

        miniAudio.attachToEngineOutput(lowPassFilter, 0);
        miniAudio.attachToEngineOutput(delayNode, 0);

        lowPassFilter.attachToThisNode(splitter, 0);
        delayNode.attachToThisNode(splitter, 1);

        splitter.attachToThisNode(maSound, 0);*/

        //maSound.setVolume(1);
        /*lowPassFilter = new MALowPassFilter(miniAudio, 100000, 1);
        miniAudio.attachToEngineOutput(lowPassFilter, 0);
        lowPassFilter.attachToThisNode(maSound, 0);*/
        maSound.loop();
        //maSound.setPositioning(MAPositioning.RELATIVE);
        //MAWaveform waveform = miniAudio.createWaveform(2, MAWaveformType.SAWTOOTH, 1, 400);
        //maSound = miniAudio.createSound(waveform);
        //maSound.play();
        //MANoise noise = miniAudio.createNoise(2, MANoiseType.WHITE, 0, 400);
        //MAWaveform waveform = miniAudio.createWaveform(2, MAWaveformType.SQUARE, 1, 400);
        //maSound = miniAudio.createSound(waveform);
        //maSound.play();
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.isLooping());
        System.out.println(maSound.isEnd());
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.getLength());

        assetManager = new AssetManager();
        assetManager.getLogger().setLevel(Logger.DEBUG);
        assetManager.setLoader(MASound.class, new MASoundLoader(miniAudio, assetManager.getFileHandleResolver()));
        //assetManager.load("game.ogg", MASound.class);
        assetManager.load("Median_test.ogg", MASound.class);
        //assetManager.load("Perfect_Mishap.ogg", MASound.class);
        //assetManager.load("piano2.wav", MASound.class);
    }

    @Override
    public void resize(int width, int height) {

    }
    private float angle;
    boolean loaded = false;
    float time = 0;
    @Override
    public void render() {
        if (!assetManager.update(60)) {
            System.out.println(assetManager.getProgress());
        } else if (!loaded) {
            loaded = true;
            /*MASound sound = assetManager.get("game.ogg", MASound.class);
            effectNode.attachToThisNode(sound, 0);
            sound.loop();*/
        }
        //System.out.println(maSound.getCursorPosition());
        //System.out.println("isLooping " + maSound.isLooping());
        //System.out.println("isEnd " + maSound.isEnd());
        /*if (Gdx.graphics.getFrameId() == 200) {
            //maSound.seekTo(45);
            MASound sound = assetManager.get("game.ogg", MASound.class);
            miniAudio.attachToEngineOutput(sound,0);
        }*/
        //if (Gdx.graphics.getFrameId() == 500) maSound.setPitch(1);
        angle += MathUtils.PI / 4f / 100f;
        time += Gdx.graphics.getDeltaTime();
        float a = (float) ((Math.cos(time) + 1f) / 2f);
        //lowPassFilter.reinit(550 * a);
        //miniAudio.setListenerDirection(0, 0, MathUtils.sin(angle));
        //maSound.setPosition(MathUtils.sin(angle), 0f, -MathUtils.cos(angle));
        //miniAudio.setListenerPosition(MathUtils.cosDeg(i)*5, 0, 0);
    }

    @Override
    public void pause() {
        miniAudio.stopEngine();
    }

    @Override
    public void resume() {
        miniAudio.startEngine();
    }

    @Override
    public void dispose() {
        assetManager.dispose();

        maSound.dispose();
        effectNode.dispose();
        maGroup.dispose();
        miniAudio.dispose();
    }
}
