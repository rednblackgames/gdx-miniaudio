package games.rednblack.miniaudio;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.MathUtils;

public class Main implements ApplicationListener {

    MiniAudio miniAudio;

    public static void main(String[] args) {
        new Lwjgl3Application(new Main(), new Lwjgl3ApplicationConfiguration());
    }

    MASound maSound;
    @Override
    public void create() {
        miniAudio = new MiniAudio();
        //int res = miniAudio.playSound("Median_test.ogg");
        //int res = miniAudio.playSound("piano2.wav");
        //System.out.println(res);
        maSound = miniAudio.createSound("piano2.wav");
        maSound.fadeIn(1000);
        maSound.loop();
        System.out.println(maSound.isPlaying());
        System.out.println(maSound.isLooping());
        System.out.println(maSound.isEnd());
        System.out.println(maSound.isPlaying());
    }

    @Override
    public void resize(int width, int height) {

    }
    float i = 0;

    @Override
    public void render() {
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
        miniAudio.dispose();
        maSound.dispose();
    }
}
