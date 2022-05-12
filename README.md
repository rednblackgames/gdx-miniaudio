# gdx-miniaudio

libGDX bindings for [MiniAudio](https://miniaud.io/) library.

## Engine

- Init Engine + Android Assets Manager
- Supported formats: WAV, FLAC, MP3, OGG Vorbis
- Start and Stop
- Master Volume
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

# TODO

- Filters and Effects Graph
- Audio recording (do we really need this?)
- Seek to is not very accurate

## 3D Spatialization
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