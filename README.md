# gdx-miniaudio

![maven-central](https://img.shields.io/maven-central/v/games.rednblack.miniaudio/miniaudio?color=blue&label=release)
![sonatype-nexus](https://img.shields.io/nexus/s/games.rednblack.miniaudio/miniaudio?label=snapshot&server=https%3A%2F%2Foss.sonatype.org)

`gdx-miniaudio` is a cross-platform Audio Engine for libGDX based on [MiniAudio](https://miniaud.io/) library.

## Features

- Reading files from internal Assets
- Supported formats: WAV, FLAC, MP3, OGG Vorbis
- Global start and stop/pause
- Master volume control
- Virtual 3D Listener Position
- Virtual 3D Listener Direction
- Virtual 3D Listener WorldUp
- Virtual 3D Listener Cone
- "Fire and forget" Sounds
- Sound creation (with custom flags)
- Sound play/pause/stop/loop/isLooping/isEnd/seekTo (not precise yet)
- Sound Volume/Pan/Pitch
- Sound Spatialization
- Sound Position
- Sound Direction
- Sound Fading (In/Out/Custom)

## How to use

### Include gradle dependencies

Native dependencies needs to be included in `core`, `android`, `ios` and `desktop` submodules.

#### Core

```groovy
dependencies {
	api "games.rednblack.miniaudio:miniaudio:$miniaudioVersion"
}
```

#### Android

```groovy
dependencies {
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-armeabi-v7a"
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-arm64-v8a"
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-x86"
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-x86_64"
}
```

#### iOS

```groovy
dependencies {
    implementation "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-ios"
}
```

#### Desktop

```groovy
dependencies {
    implementation "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-desktop"
}
```

### Usage

Usage of MiniAudio Engine is straightforward.

```java
public class Main implements ApplicationListener {
    MiniAudio miniAudio;
    MASound maSound;
    
    @Override
    public void create() {
        //Create only one MiniAudio object!
        miniAudio = new MiniAudio();
        
        //Play simple audio
        int res = miniAudio.playSound("piano2.wav");
        //res contains result code, check it for errors with
        //games.rednblack.miniaudio.Result class

        //Create and load a sound without starting it
        maSound = miniAudio.createSound("piano2.wav");
        // .. sound customization ...
        maSound.play();
    }

    @Override
    public void dispose() {
        //Always dispose everything! First all sounds and then the engine
        maSound.dispose();
        miniAudio.dispose();
    }

    @Override
    public void pause() {
        miniAudio.stopEngine();
    }

    @Override
    public void resume() {
        miniAudio.startEngine();
    }
}
```

#### Android additions

If current platform is Android, native AssetManager object needs to be injected to `MiniAudio` engine.
Refers to [Interfacing with platform specific code](https://libgdx.com/wiki/app/interfacing-with-platform-specific-code).

```java
miniAudio.setupAndroid(Context#getAssets());
```

You can safely pass `null` on other platforms.

## TODO

- Filters and Effects Graph
- Audio recording (do we really need this?)
- Seek to is not very accurate

### 3D Spatialization
- Engine Multiple 3D Listeners (and ma_sound_set_pinned_listener_index)(are they useful?)
- ma_sound_set_cone
- ma_sound_set_velocity
- ma_sound_set_attenuation_model
- ma_sound_set_positioning
- ma_sound_set_rolloff
- ma_sound_set_min_gain
- ma_sound_set_max_gain
- ma_sound_set_min_distance
- ma_sound_set_max_distance
- ma_sound_set_doppler_factor

### License

[MiniAudio](https://github.com/mackron/miniaudio) is licensed under public domain or MIT No Attribution.

`gdx-miniaudio` is available under the Apache 2.0 Open Source License.
```
Copyright (c) 2022 Francesco Marongiu.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
