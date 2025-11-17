package games.rednblack.miniaudio;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Logger;
import games.rednblack.miniaudio.config.MAContextConfiguration;
import games.rednblack.miniaudio.config.MAEngineConfiguration;
import games.rednblack.miniaudio.config.MAiOSSessionCategory;
import games.rednblack.miniaudio.effect.*;
import games.rednblack.miniaudio.filter.MALowPassFilter;
import games.rednblack.miniaudio.loader.MASoundLoader;
import games.rednblack.miniaudio.mix.MAChannelCombiner;
import games.rednblack.miniaudio.mix.MAChannelSeparator;
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
    MAAudioBuffer decodedBuffer;

    AssetManager assetManager;
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
        MADeviceInfo[] devices = miniAudio.enumerateDevices();
        long def = 0;
        for (MADeviceInfo info : devices) {
            System.out.println(info.isCapture + " " + info.idAddress + " . " + info.name);
            if (info.name.startsWith("GA102 High Definition Audio Controller")) {
                def = info.idAddress;
                //break;
            }
        }
        System.out.println("GA102 High Definition Audio Controller " + def);
        engineConfiguration.playbackId = def;
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

       /*byte[] data = Gdx.files.internal("background.wav").readBytes();
        System.out.println("data length: " + data.length);
        decodedBuffer = miniAudio.decodeBytes(data, 2);
        MASound maSound1 = miniAudio.createSound(decodedBuffer);
        maSound1.setLinkedAudioBuffer(decodedBuffer);

        MAAudioBuffer audioBuffer = miniAudio.createAudioBuffer(4096, 2);
        maSound = miniAudio.createSound(audioBuffer);
        maSound.setLinkedAudioBuffer(audioBuffer);
        float[] pcmFrames = new float[4096];

        // 2. Definiamo le frequenze per le parti del suono "Hey".
        // Queste sono scelte arbitrarie per creare un effetto vocale.
        double freq_H = 300.0; // Frequenza per la parte "H" (più bassa)
        double freq_E = 500.0; // Frequenza per la parte "E" (più alta)
        double freq_Y = 400.0; // Frequenza per la parte finale "Y" (scende un po')

        // 3. Dividiamo l'array in sezioni per ogni "lettera".
        int h_end = pcmFrames.length / 4;      // Il primo 25%
        int e_end = pcmFrames.length * 3 / 4;  // Il successivo 50%
        // La parte finale "Y" occuperà il restante 25%.

        // 4. Generiamo le onde sinusoidali e le scriviamo nell'array.
        for (int i = 0; i < pcmFrames.length; i++) {
            double frequency;

            // Scegli la frequenza in base alla posizione nell'array
            if (i < h_end) {
                frequency = freq_H;
            } else if (i < e_end) {
                frequency = freq_E;
            } else {
                frequency = freq_Y;
            }

            // Calcola il valore del campione usando la formula dell'onda sinusoidale.
            // Math.sin(2 * PI * frequenza * tempo)
            // dove tempo = indice_campione / frequenza_campionamento
            double time = (double) i / 48000;
            pcmFrames[i] = (float) Math.sin(2 * Math.PI * frequency * time);

            // Aggiungiamo un leggero "fade out" alla fine per evitare un "click" secco.
            if (i > pcmFrames.length * 0.8) {
                float fadeMultiplier = 1.0f - ((float)(i - pcmFrames.length * 0.8f) / (pcmFrames.length * 0.2f));
                pcmFrames[i] *= fadeMultiplier;
            }
        }
        audioBuffer.write(pcmFrames);
        maSound.play();
        maSound.chainSound(maSound1);*/
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

        maSound = miniAudio.createSound("piano2.wav");
        maSound.loop();

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

        /*MACompressorNode compressorNode = new MACompressorNode(miniAudio);
        compressorNode.setRatio(24);
        compressorNode.setThreshold(-50);
        compressorNode.setRelease(200);
        MASplitter splitter = new MASplitter(miniAudio);
        splitter.attachToThisNode(maSound, 0);

        compressorNode.attachToThisNode(backgroundMusic, 0, 0);
        compressorNode.attachToThisNode(splitter, 0, 1);

        miniAudio.attachToEngineOutput(compressorNode, 0);
        miniAudio.attachToEngineOutput(splitter, 1);*/

        //backgroundMusic.loop();

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
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.isLooping());
        System.out.println(maSound.isEnd());
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.getLength());

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
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
        }
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
        //effectNode.dispose();
        //maGroup.dispose();
        miniAudio.dispose();
    }
}
