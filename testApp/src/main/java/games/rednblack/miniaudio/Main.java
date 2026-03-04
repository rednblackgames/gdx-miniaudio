package games.rednblack.miniaudio;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;
import games.rednblack.miniaudio.config.MAContextConfiguration;
import games.rednblack.miniaudio.config.MAEngineConfiguration;
import games.rednblack.miniaudio.config.MAiOSSessionCategory;
import games.rednblack.miniaudio.effect.*;
import games.rednblack.miniaudio.filter.MALowPassFilter;
import games.rednblack.miniaudio.loader.MASoundLoader;
import games.rednblack.miniaudio.mix.MAChannelCombiner;
import games.rednblack.miniaudio.mix.MAChannelSeparator;
import games.rednblack.miniaudio.mix.MASplitter;
import games.rednblack.miniaudio.mix.MAVisualizerNode;

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
    MAAudioBuffer decodedBuffer;

    AssetManager assetManager;
    ShapeRenderer shapeRenderer;
    final float[] visualizerData = new float[8192];
    volatile int visualizerSamples;
    volatile int visualizerChannels;
    @Override
    public void create() {
        //miniAudio = new MiniAudio(1, 1, 0, 256, 44100);
        //miniAudio = new MiniAudio(null, false, true, false);
        MAContextConfiguration contextConfiguration = new MAContextConfiguration();
        contextConfiguration.logCallback = new MALogCallback() {
            @Override
            public void onLog(MALogLevel level, String message) {
                System.out.println("onLog " + level  + ", " + message + ": " + Thread.currentThread().getId());
            }
        };
        contextConfiguration.iOSSessionCategory = MAiOSSessionCategory.AMBIENT;
        MAEngineConfiguration engineConfiguration = new MAEngineConfiguration();
        miniAudio = new MiniAudio(contextConfiguration, null);
        listDevices();
        miniAudio.initEngine(engineConfiguration);
        miniAudio.setEndListener(new MASoundEndListener() {
            @Override
            public void onSoundEnd(MASound maSound) {
                System.out.println(maSound.getAddress());
            }
        });
        //miniAudio.initEngine(1, -1, -1, 0, 0, 100, 192000, MAFormatType.F32, true, false, true);
        /*miniAudio.setDeviceNotificationListener(new MADeviceNotificationListener() {
            @Override
            public void onNotification(MADeviceNotificationType type) {
                System.out.println("onNotification " + type + ": " + Thread.currentThread().getId());
            }
        });
        miniAudio.setEndListener(new MASoundEndListener() {
            @Override
            public void onSoundEnd(MASound maSound) {
                //System.out.println("Sound End: " + Thread.currentThread().getId());
            }
        });
        miniAudio.setLogCallback(new MALogCallback() {
            @Override
            public void onLog(MALogLevel level, String message) {
                System.out.println("onLog " + level  + ", " + message + ": " + Thread.currentThread().getId());
            }
        });*/

       /* miniAudio.initEngine(1, -1, -1, 2, 0, 512, 44100, MAFormatType.F32,false, false, false);
        miniAudio.setListenerDirection(0, 0, 1);
        miniAudio.setListenerCone(MathUtils.PI / 4f, MathUtils.PI / 4f, 2f);*/

        //maGroup = miniAudio.createGroup();
        //maGroup.setSpatialization(true);
        //int res = miniAudio.playSound("Median_test.ogg");
        //int res = miniAudio.playSound("piano2.wav");
        //System.out.println(res);
        //maSound = miniAudio.createSound("click.mp3");
        //maSound = miniAudio.createSound(decodedBuffer);
        //maSound.play();
        //maSound.setPositioning(MAPositioning.RELATIVE);
        //maSound = miniAudio.createInputSound((short) 0, null);

        /*maSound = miniAudio.createSound("Symphony No.6 (1st movement).ogg");
        maSound.loop();*/

        //Create a Group
        /*MAGroup group = miniAudio.createGroup();

        //Create all sounds attacched to the same group
        maSound = miniAudio.createSound("voice.mp3", (short) 0, group);
        short flags = MASound.Flags.MA_SOUND_FLAG_STREAM;
        MASound backgroundMusic = miniAudio.createSound("game.ogg", flags, null);
        System.out.println(backgroundMusic.getLength());

        //Basic peaking filter node
        MALimiterNode limiterNode = new MALimiterNode(miniAudio);
        limiterNode.setCeilingDb(-30);
        //Detach the group from main end point and attach to the limiter input
        limiterNode.attachToThisNode(group, 0);

        //Attach the limiter output to the main engine endpoint
        miniAudio.attachToEngineOutput(limiterNode, 0);*/

        MAVisualizerNode maVisualizerNode = new MAVisualizerNode(miniAudio);
        maVisualizerNode.setListener(new MAVisualizerListener() {
            @Override
            public void onVisualizerData(float[] pcmData, int totalSamples, int channels) {
                int len = Math.min(totalSamples, visualizerData.length);
                System.arraycopy(pcmData, 0, visualizerData, 0, len);
                visualizerChannels = channels;
                visualizerSamples = len;
            }
        });

        MASound backgroundMusic = miniAudio.createSound("background.wav");
        maSound = miniAudio.createSound("voice.mp3");
        MACompressorNode compressorNode = new MACompressorNode(miniAudio);
        compressorNode.setRatio(24);
        compressorNode.setThreshold(-50);
        compressorNode.setRelease(200);
        MASplitter splitter = new MASplitter(miniAudio);
        splitter.attachToThisNode(maSound, 0);

        compressorNode.attachToThisNode(backgroundMusic, 0, 0);
        compressorNode.attachToThisNode(splitter, 0, 1);

        maVisualizerNode.attachToThisNode(compressorNode, 0);

        miniAudio.attachToEngineOutput(maVisualizerNode, 0);
        miniAudio.attachToEngineOutput(splitter, 1);

        backgroundMusic.loop();

        /*MALimiterNode limiterNode = new MALimiterNode(miniAudio);
        limiterNode.setCeilingDb(-50);
        limiterNode.attachToThisNode(backgroundMusic, 0);
        miniAudio.attachToEngineOutput(limiterNode, 0);*/

        /*MAPhaserNode phaserNode = new MAPhaserNode(miniAudio);
        phaserNode.setFeedback(0.8f);
        phaserNode.setStages(4);
        phaserNode.setRate(1f);
        phaserNode.setDepth(3);
        phaserNode.setWet(1);
        phaserNode.setDry(1);
        phaserNode.setFrequencyRangeMin(800);
        phaserNode.setFrequencyRangeMax(1200);
        phaserNode.attachToThisNode(maSound, 0);
        miniAudio.attachToEngineOutput(phaserNode, 0);*/

        /*MADelayNode delayNode = new MADelayNode(miniAudio,  0.25f, 0.45f);
        delayNode.attachToThisNode(maSound, 0);

        MAChannelSeparator channelSeparator = new MAChannelSeparator(miniAudio, 2);
        channelSeparator.attachToThisNode(delayNode, 0);

        //Left channel volume
        channelSeparator.setOutputBusVolume(1, 1);
        //Right channel volume
        channelSeparator.setOutputBusVolume(0, 0f);

        MAChannelCombiner channelCombiner = new MAChannelCombiner(miniAudio, 2);
        channelCombiner.attachToThisNode(channelSeparator, 0, 0);
        channelCombiner.attachToThisNode(channelSeparator, 1, 1);


        miniAudio.attachToEngineOutput(channelCombiner, 0);*/

        /*MADelayNode delayNode = new MADelayNode(miniAudio,  0.25f, 0.45f);
        delayNode.attachToThisNode(maSound, 0);

        miniAudio.attachToEngineOutput(delayNode, 0);

        miniAudio.setListenerPosition(1f, 0, 0);*/

        //maSound.setVolume(2);

        //effectNode = new MADelayNode(miniAudio, 0.25f, 0.45f);
        //effectNode = new MAReverbNode(miniAudio);
        //effectNode.attachToThisNode(maSound, 0);
        //miniAudio.attachToEngineOutput(effectNode, 0);
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
        //maSound.loop();
        //maSound.setPositioning(MAPositioning.RELATIVE);
        //MAWaveform waveform = miniAudio.createWaveform(2, MAWaveformType.SAWTOOTH, 1, 400);
        //maSound = miniAudio.createSound(waveform);
        //maSound.play();
        //MANoise noise = miniAudio.createNoise(2, MANoiseType.WHITE, 0, 400);
        //MAWaveform waveform = miniAudio.createWaveform(2, MAWaveformType.SQUARE, 1, 400);
        //maSound = miniAudio.createSound(waveform);
        //maSound.play();
        /*System.out.println(maSound.isPlaying());
        System.out.println(maSound.isLooping());
        System.out.println(maSound.isEnd());
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.getLength());*/

        assetManager = new AssetManager();
        assetManager.getLogger().setLevel(Logger.DEBUG);
        assetManager.setLoader(MASound.class, new MASoundLoader(miniAudio, assetManager.getFileHandleResolver()));
        //assetManager.load("game.ogg", MASound.class);
        //assetManager.load("Median_test.ogg", MASound.class);
        //assetManager.load("Perfect_Mishap.ogg", MASound.class);
        //assetManager.load("piano2.wav", MASound.class);

        /*MASoundPool soundPool = new MASoundPool(miniAudio, "piano2.wav");
        MASound sound = soundPool.obtain();
        for (int i = 0; i < 10; i++) {
            sound.play();
        }*/

        shapeRenderer = new ShapeRenderer();
    }

    private void listDevices() {
        MADeviceInfo[] devices = miniAudio.getAvailableDevices();
        for (MADeviceInfo info : devices) {
            System.out.println(info.isCapture + " " + info.idAddress + " . " + info.name);
            for (MADeviceInfo.MADeviceNativeDataFormat format : info.nativeDataFormats) {
                System.out.println("\t-" + format.channels + " -> " + format.sampleRate + " -> " + format.format);
            }
        }
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            maSound.reset();
            maSound.play();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            maSound.stop(1000);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
            miniAudio.changeDevice(miniAudio.getAvailableDevices()[0], null);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
            miniAudio.changeDevice(miniAudio.getAvailableDevices()[1], null);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
            miniAudio.changeDevice(miniAudio.getAvailableDevices()[3], null);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            miniAudio.changeDevice(null, null);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            listDevices();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            miniAudio.refreshAvailableDevices();
        }
        /*if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            maSound.stop(1000);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            maSound.loop();
        }
        System.out.println(maSound.isPlaying());
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            System.out.println(maSound.isPlaying());
            System.out.println(maSound.isLooping());
            System.out.println(maSound.isEnd());
            System.out.println(maSound.getLength());
        }*/
        //System.out.println(maSound.getFadeVolume());
        //System.out.println(maSound.getCursorPosition());
        //System.out.println("isLooping " + maSound.isLooping());
        //System.out.println("isEnd " + maSound.isEnd());
        /*if (Gdx.graphics.getFrameId() == 200) {
            //maSound.seekTo(45);
            MASound sound = assetManager.get("game.ogg", MASound.class);
            miniAudio.attachToEngineOutput(sound,0);
        }*/
        time += Gdx.graphics.getDeltaTime();
        //if (time > 1 && time < 2) maSound.play();
        //if (Gdx.graphics.getFrameId() == 500) maSound.setPitch(1);
        /*angle += MathUtils.PI / 4f / 100f;
        time += Gdx.graphics.getDeltaTime();
        if (time > 0.3f) {
            long m = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            System.out.println(m);
            maSound.play();
            time = 0;
        }
        float a = (float) ((Math.cos(time) + 1f) / 2f);*/
        //lowPassFilter.reinit(550 * a);
        //miniAudio.setListenerDirection(0, 0, MathUtils.sin(angle));
        //maSound.setPosition(MathUtils.sin(angle), 0f, -MathUtils.cos(angle));
        //miniAudio.setListenerPosition(MathUtils.cosDeg(i)*5, 0, 0);

        ScreenUtils.clear(0, 0, 0, 1);
        int samples = visualizerSamples;
        int ch = visualizerChannels;
        if (samples > 0 && ch > 0) {
            float width = Gdx.graphics.getWidth();
            float height = Gdx.graphics.getHeight();
            float midY = height / 2f;
            int frames = samples / ch;
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0, 1, 0, 1);
            for (int i = 1; i < frames; i++) {
                float x1 = (i - 1) * width / (frames - 1);
                float x2 = i * width / (frames - 1);
                float y1 = midY + visualizerData[(i - 1) * ch] * midY;
                float y2 = midY + visualizerData[i * ch] * midY;
                shapeRenderer.line(x1, y1, x2, y2);
            }
            shapeRenderer.end();
        }
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
        shapeRenderer.dispose();

        //maSound.dispose();
        //effectNode.dispose();
        //maGroup.dispose();
        miniAudio.dispose();
    }
}
