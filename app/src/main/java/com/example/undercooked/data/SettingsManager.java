// ══════════════════════════════════════════════════
// FILE: SettingsManager.java
// ══════════════════════════════════════════════════
package com.example.undercooked.data;

import android.content.Context;           // gives access to app files and resources
import android.content.SharedPreferences; // saves simple key-value data that persists after the app closes

/**
 * SettingsManager — saves and loads the player's game settings.
 * Uses SharedPreferences — like a small dictionary stored on the device.
 * Settings survive closing the app and restarting the phone.
 */
public class SettingsManager {

    private static final String PREFS_NAME = "undercooked_settings"; // the name of the file where settings are saved
    private static final String KEY_MUSIC  = "music_enabled";        // key for the music setting
    private static final String KEY_SFX    = "sfx_enabled";          // key for the sound effects setting

    private final SharedPreferences prefs; // the object that does the actual saving and loading

    // opens (or creates) the settings file
    public SettingsManager(Context context) {
        // MODE_PRIVATE means only our app can read this file
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // returns whether music is enabled — defaults to true if never saved before
    public boolean isMusicEnabled() {
        return prefs.getBoolean(KEY_MUSIC, true); // second argument is the default value
    }

    // saves the music setting — true = on, false = off
    public void setMusicEnabled(boolean enabled) {
        prefs.edit()                         // open edit mode
                .putBoolean(KEY_MUSIC, enabled) // write the value
                .apply();                       // save asynchronously (does not block the UI)
    }

    // returns whether sound effects are enabled — defaults to true
    public boolean isSfxEnabled() {
        return prefs.getBoolean(KEY_SFX, true);
    }

    // saves the sound effects setting
    public void setSfxEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(KEY_SFX, enabled)
                .apply();
    }
}