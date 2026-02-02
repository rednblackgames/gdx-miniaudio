# gdx-miniaudio

![maven-central](https://img.shields.io/maven-central/v/games.rednblack.miniaudio/miniaudio?color=blue&label=release)
![sonatype-nexus](https://img.shields.io/maven-metadata/v?label=snapshot&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fgames%2Frednblack%2Fminiaudio%2Fminiaudio%2Fmaven-metadata.xml)

`gdx-miniaudio` is a cross-platform Audio Engine for libGDX based on [MiniAudio](https://miniaud.io/) library.

| gdx-miniaudio | libGDX |
|---------------|--------|
| <=0.7         | 1.14.0 |
| <=0.5         | 1.12.1 |
| <=0.3         | 1.11.0 |

## Features

- Reading files from internal Assets
- Supported formats: WAV, FLAC, MP3, OGG Vorbis/Opus
- Global start and stop/pause
- Master volume control
- Virtual 3D Listener (Position/Direction/WorldUp/Cone)
- Multiple 3D Listeners
- "Fire and forget" Sounds
- Sound creation (with custom flags)
- Sound play/pause/stop/loop/isLooping/isEnd/seekTo
- Sound Volume/Pan/Pitch
- Sound Spatialization (position, direction, cone, attenuation model, rolloff, etc.)
- Sound Fading (In/Out/Custom)
- Sound length and current cursor position
- Sound end callback
- Chain multiple sounds gapless
- Graph Nodes Filters/Effects
- Play raw PCM data
- Waves and Noise generation
- Sound Groups with atomic management
- Sound Groups Spatialization (position, direction, cone, attenuation model, rolloff, etc.)
- libGDX AssetManager Loader
- MASoundPool with libGDX Pool API
- Audio Input Node (microphone)
- Audio Encoder Node (only wav)

**Filters**
- Band Pass
- Biquad
- High Pass
- High Shelf
- Low Pass
- Low Shelf
- Notching
- Peaking

**Effects**

- Delay/Echo
- Reverb
- Chorus
- Flanger
- Phaser
- Compressor (with side-chain control)
- Peak Limiter

**Mixers**

- Channel Splitter
- Channel Combiner
- Stream Splitter (duplicate source)
- Leading silence trimmer

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
Min SDK Required is `Android 24`

```groovy
dependencies {
    natives "games.rednblack.miniaudio:gdx-miniaudio-platform:$miniaudioVersion:natives-armeabi-v7a"
    natives "games.rednblack.miniaudio:gdx-miniaudio-platform:$miniaudioVersion:natives-arm64-v8a"
    natives "games.rednblack.miniaudio:gdx-miniaudio-platform:$miniaudioVersion:natives-x86"
    natives "games.rednblack.miniaudio:gdx-miniaudio-platform:$miniaudioVersion:natives-x86_64"
}
```

<details>
  <summary>Until v0.5</summary>

```groovy
dependencies {
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-armeabi-v7a"
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-arm64-v8a"
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-x86"
    natives "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-x86_64"
}
```
</details>

#### iOS

Min iOS Required is `15.0`

```groovy
dependencies {
    implementation "games.rednblack.miniaudio:gdx-miniaudio-platform:$miniaudioVersion:natives-ios"
}
```

<details>
  <summary>Until v0.5</summary>

```groovy
dependencies {
    implementation "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-ios"
}
```
</details>

#### Desktop

```groovy
dependencies {
    implementation "games.rednblack.miniaudio:gdx-miniaudio-platform:$miniaudioVersion:natives-desktop"
}
```

<details>
  <summary>Until v0.5</summary>

```groovy
dependencies {
    implementation "games.rednblack.miniaudio:miniaudio:$miniaudioVersion:natives-desktop"
}
```
</details>

### Usage

Usage of MiniAudio Engine is straightforward.

```java
public class Main implements ApplicationListener {
    MiniAudio miniAudio;
    MASound maSound;
    AssetManager assetManager;
    
    @Override
    public void create() {
        //Create only one MiniAudio object!
        miniAudio = new MiniAudio();
        
        //Play simple audio
        int res = miniAudio.playSound("piano2.wav");
        //res contains result code, check it for errors with
        //games.rednblack.miniaudio.MAResult class

        //Create and load a sound without starting it
        maSound = miniAudio.createSound("piano2.wav");
        // .. sound customization ...
        maSound.play();
        
        //AssetManager Loader
        assetManager = new AssetManager();
        assetManager.setLoader(MASound.class, new MASoundLoader(miniAudio, assetManager.getFileHandleResolver()));
        assetManager.load("game.ogg", MASound.class);
        // ... load as usual ...
    }

    @Override
    public void dispose() {
        //Always dispose everything! First all sounds and then the engine
        maSound.dispose();
        //If MASounds are loaded with the AssetManager be sure to dispose it first
        assetManager.dispose();
        
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

### Proguard Rules
If your Java code is obfuscated, be sure to keep required JNI methods.

```
-keepclassmembers class games.rednblack.miniaudio.MiniAudio {
    public void on_native_sound_end(long);
    public void on_native_log(int, java.lang.String);
    public void on_native_notification(int);
}
```

### Effects Graph

MiniAudio comes with a powerful effects system based on graph design.

```
    >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Data flows left to right >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                                                   +-----------------+
                              +----------+    +----= Low Pass Filter =----+
         +---------------+    |          =----+    +-----------------+    |    +----------+
         |    MASound    =----= Splitter |                                +----= ENDPOINT |
         +---------------+    |          =----+    +-----------------+    |    +----------+
                              +----------+    +----=  Echo / Delay   =----+
                                                   +-----------------+
```

```java
MASplitter splitter = new MASplitter(miniAudio);
MALowPassFilter lowPassFilter = new MALowPassFilter(miniAudio, 550, 8);
MADelayNode delayNode = new MADelayNode(miniAudio, 0.25f, 0.45f);

miniAudio.attachToEngineOutput(lowPassFilter, 0);
miniAudio.attachToEngineOutput(delayNode, 0);

lowPassFilter.attachToThisNode(splitter, 0);
delayNode.attachToThisNode(splitter, 1);

splitter.attachToThisNode(maSound, 0);

maSound.loop();
```

## TODO

- Blocking Stream PCM data (like libGDX specs - maybe)

- MiniAudio has tons of additional features, open a issue to request more bindings

## Licenses

- `gdx-miniaudio` is available under the Apache 2.0 Open Source License.
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

- [MiniAudio](https://github.com/mackron/miniaudio) is licensed under public domain or MIT No Attribution.

- `opusfile`
```
Copyright (c) 1994-2013 Xiph.Org Foundation and contributors

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of the Xiph.Org Foundation nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
- `opus`
```
Copyright 2001-2023 Xiph.Org, Skype Limited, Octasic,
                    Jean-Marc Valin, Timothy B. Terriberry,
                    CSIRO, Gregory Maxwell, Mark Borgerding,
                    Erik de Castro Lopo, Mozilla, Amazon

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Internet Society, IETF or IETF Trust, nor the
names of specific contributors, may be used to endorse or promote
products derived from this software without specific prior written
permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Opus is subject to the royalty-free patent licenses which are
specified at:

Xiph.Org Foundation:
https://datatracker.ietf.org/ipr/1524/

Microsoft Corporation:
https://datatracker.ietf.org/ipr/1914/

Broadcom Corporation:
https://datatracker.ietf.org/ipr/1526/
```
- `ogg`
```
Copyright (c) 2002, Xiph.org Foundation

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of the Xiph.org Foundation nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

- `vorbis`
```
Copyright (c) 2002-2020 Xiph.org Foundation

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of the Xiph.org Foundation nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```