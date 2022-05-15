package games.rednblack.miniaudio;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class Main implements ApplicationListener {

    MiniAudio miniAudio;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.disableAudio(true);
        new Lwjgl3Application(new Main(), configuration);
    }

    MASound maSound;
    MADelayNode delayNode;

    @Override
    public void create() {
        miniAudio = new MiniAudio();
        //int res = miniAudio.playSound("Median_test.ogg");
        //int res = miniAudio.playSound("piano2.wav");
        //System.out.println(res);
        maSound = miniAudio.createSound("Median_test.ogg");

        delayNode = new MADelayNode(miniAudio, 0.2f, 0.5f);
        miniAudio.attachToOutput(delayNode, 0);
        delayNode.attachToNode(maSound, 0);

        maSound.loop();
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.isLooping());
        System.out.println(maSound.isEnd());
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.getLength());
    }

    @Override
    public void resize(int width, int height) {

    }
    float i = 0;

    @Override
    public void render() {
        //System.out.println(maSound.getCursorPosition());
        //System.out.println("isLooping " + maSound.isLooping());
        //System.out.println("isEnd " + maSound.isEnd());
        //if (Gdx.graphics.getFrameId() == 200) maSound.seekTo(45);
        //if (Gdx.graphics.getFrameId() == 500) maSound.setPitch(1);
        i += 0.1f;
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
        delayNode.dispose();
        miniAudio.dispose();
    }
}
