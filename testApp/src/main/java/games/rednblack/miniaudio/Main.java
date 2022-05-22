package games.rednblack.miniaudio;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.MathUtils;
import games.rednblack.miniaudio.effect.MADelayNode;
import games.rednblack.miniaudio.filter.MABiquadFilter;

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

    @Override
    public void create() {
        miniAudio = new MiniAudio();
        miniAudio.setListenerDirection(0, 0, 1);
        miniAudio.setListenerCone(MathUtils.PI / 4f, MathUtils.PI / 4f, 2f);

        maGroup = miniAudio.createGroup();
        maGroup.setSpatialization(true);
        //int res = miniAudio.playSound("Median_test.ogg");
        //int res = miniAudio.playSound("piano2.wav");
        //System.out.println(res);
        maSound = miniAudio.createSound("Perfect_Mishap.ogg");
        maSound.setPositioning(MAPositioning.RELATIVE);

        //effectNode = new MADelayNode(miniAudio, 0.2f, 0.5f);
        effectNode = new MABiquadFilter(miniAudio, .0102f, .0105f, .011f, .109f, .01047f, .1028f);

        miniAudio.attachToOutput(effectNode, 0);
        //effectNode.attachToNode(maSound, 0);

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
    }

    @Override
    public void resize(int width, int height) {

    }
    private float angle;

    @Override
    public void render() {
        //System.out.println(maSound.getCursorPosition());
        //System.out.println("isLooping " + maSound.isLooping());
        //System.out.println("isEnd " + maSound.isEnd());
        //if (Gdx.graphics.getFrameId() == 200) maSound.seekTo(45);
        //if (Gdx.graphics.getFrameId() == 500) maSound.setPitch(1);
        angle += MathUtils.PI / 4f / 100f;
        maGroup.setPosition(MathUtils.sin(angle), 0f, -MathUtils.cos(angle));
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
        maSound.dispose();
        effectNode.dispose();
        maGroup.dispose();
        miniAudio.dispose();
    }
}
