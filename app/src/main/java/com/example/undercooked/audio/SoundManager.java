package com.example.undercooked.audio; // this file belongs to the audio package

import android.content.Context; // Context gives us access to app resources like sound files
import android.media.AudioAttributes; // defines what kind of sound we are playing (game, music, etc.)
import android.media.MediaPlayer; // plays long audio files like background music and footsteps
import android.media.SoundPool; // plays short sound effects quickly — faster than MediaPlayer

import com.example.undercooked.R; // access to the sound files stored in res/raw

/**
 * SoundManager — handles all sounds in the game.
 *
 * Long sounds (music, footsteps, chopping) use MediaPlayer.
 * Short sounds (bell, error) use SoundPool because it is faster to trigger.
 */
public class SoundManager {

    private final Context context; // saved so we can load sounds later

    // long sounds — MediaPlayer is best for files longer than a few seconds
    private MediaPlayer backgroundMusic; // the background music that loops during the game
    private MediaPlayer footstepsPlayer; // the footstep sound that plays while the chef walks
    private MediaPlayer choppingPlayer;  // the chopping sound that plays while the chef chops

    // short sounds — SoundPool plays these instantly without delay
    private SoundPool soundPool; // the sound engine for short effects
    private int bellSound;       // the ID of the bell sound inside the SoundPool
    private int errorSound;      // the ID of the error sound inside the SoundPool

    private boolean sfxEnabled = true; // whether sound effects are on — can be turned off in settings

    // we skip the first 1.34 seconds of the footstep file because it is silent
    private static final int FOOTSTEPS_START_MS = 1340;
    private static final int BELL_START_MS      = 0; // the bell starts from the beginning

    // constructor — loads all sounds when SoundManager is created
    public SoundManager(Context context) {
        this.context = context; // save the context for later use

        // load the background music from res/raw/map1_sound.wav
        backgroundMusic = MediaPlayer.create(context, R.raw.map1_sound);
        if (backgroundMusic != null) { // check that loading succeeded before configuring
            backgroundMusic.setLooping(true);        // loop forever during the game
            backgroundMusic.setVolume(0.3f, 0.3f);   // lower volume so it does not overpower effects
        }

        // load the footstep sound from res/raw/footstepssound.wav
        footstepsPlayer = MediaPlayer.create(context, R.raw.footstepssound);
        if (footstepsPlayer != null) { // check that loading succeeded
            footstepsPlayer.setLooping(true);                 // loop while the chef is moving
            footstepsPlayer.setVolume(1.0f, 1.0f);            // full volume
            footstepsPlayer.seekTo(FOOTSTEPS_START_MS);       // skip the silent intro
        }

        // create AudioAttributes to tell the system this sound is for a game
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)                        // used in a game
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)   // it is a sound effect
                .build(); // build the AudioAttributes object

        // create SoundPool — up to 5 sounds can play at the same time
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)                       // allow 5 simultaneous sounds
                .setAudioAttributes(audioAttributes)   // use the game settings we defined above
                .build();

        // load the short sounds into SoundPool and save their IDs
        bellSound  = soundPool.load(context, R.raw.bellsound,  1); // load bell sound
        errorSound = soundPool.load(context, R.raw.errorsound, 1); // load error sound
    }

    // ── Background Music ──────────────────────────

    // start the background music — only if it is not already playing
    public void playMusic() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) // check it exists and is not playing
            backgroundMusic.start(); // start playing
    }

    // pause the background music — only if it is currently playing
    public void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) // check it exists and is playing
            backgroundMusic.pause(); // pause (not stop) so we can resume from the same position
    }

    // ── Footsteps ─────────────────────────────────

    // start the footstep sound when the chef starts walking
    public void startFootsteps() {
        if (footstepsPlayer != null && !footstepsPlayer.isPlaying()) // only start if not already playing
            footstepsPlayer.start(); // start playing
    }

    // stop the footstep sound when the chef stops walking
    public void stopFootsteps() {
        if (footstepsPlayer != null && footstepsPlayer.isPlaying()) { // check it is playing
            footstepsPlayer.pause();                          // pause the sound
            footstepsPlayer.seekTo(FOOTSTEPS_START_MS);      // go back to skip the silent intro next time
        }
    }

    // change the footstep volume based on how fast the chef is moving
    public void setFootstepVolume(float volume) {
        if (volume < 0f) volume = 0f; // volume cannot be negative
        if (volume > 1f) volume = 1f; // volume cannot exceed 1.0
        if (footstepsPlayer != null)
            footstepsPlayer.setVolume(volume, volume); // set left and right channel volume
    }

    // ── Chopping ──────────────────────────────────

    // start the chopping sound when the chef begins chopping an ingredient
    public void startChopping() {
        if (choppingPlayer == null) { // if there is no player yet, create one
            choppingPlayer = MediaPlayer.create(context, R.raw.choppingsound); // load the file
            if (choppingPlayer != null) {
                choppingPlayer.setLooping(true); // loop while the chef is chopping
                choppingPlayer.seekTo(1350);     // skip 1.35 seconds of silence at the start
            }
        }
        if (choppingPlayer != null && !choppingPlayer.isPlaying()) // only start if not already playing
            choppingPlayer.start(); // start playing
    }

    // stop the chopping sound and free the memory it used
    public void stopChopping() {
        if (choppingPlayer != null) {
            if (choppingPlayer.isPlaying()) choppingPlayer.stop(); // stop the sound
            choppingPlayer.release(); // free the memory the MediaPlayer was using
            choppingPlayer = null;    // mark that there is no active player
        }
    }

    // ── Bell ──────────────────────────────────────

    // play the bell sound — used when chopping finishes or a dish is delivered
    public void playBell() {
        if (!sfxEnabled) return; // do not play if sound effects are turned off in settings
        MediaPlayer bell = MediaPlayer.create(context, R.raw.bellsound); // load the bell sound
        if (bell != null) {
            bell.seekTo(BELL_START_MS);                          // start from the beginning
            bell.setOnCompletionListener(MediaPlayer::release);  // auto-free memory when done
            bell.start();                                        // play the sound
        }
    }

    // ── Error ─────────────────────────────────────

    // play the error sound — can be used when the chef does something invalid
    public void playError() {
        if (!sfxEnabled) return; // do not play if sound effects are turned off
        soundPool.play(errorSound, 1.0f, 1.0f, 0, 0, 1.0f); // play at full volume, no loop
    }

    // ── Settings ──────────────────────────────────

    // enable or disable sound effects (bell, error) from the settings screen
    public void setSfxEnabled(boolean enabled) {
        this.sfxEnabled = enabled; // save the choice
    }

    // returns whether sound effects are currently enabled
    public boolean isSfxEnabled() {
        return sfxEnabled;
    }

    // ── Cleanup ───────────────────────────────────

    // call this when the game closes — frees all memory used by sounds
    public void destroy() {
        if (backgroundMusic != null) { backgroundMusic.stop(); backgroundMusic.release(); } // free music
        if (footstepsPlayer  != null) { footstepsPlayer.stop();  footstepsPlayer.release(); } // free footsteps
        if (choppingPlayer   != null) { choppingPlayer.release(); } // free chopping sound
        if (soundPool        != null) { soundPool.release(); }      // free the SoundPool
    }
}