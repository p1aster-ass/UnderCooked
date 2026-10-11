package com.example.undercooked; // this file belongs to the main package of the app

import android.os.Bundle;                          // Bundle stores data that survives screen rotations
import androidx.appcompat.app.AppCompatActivity;   // the base class for every screen in an Android app

/**
 * MainActivity — the screen that runs the actual game.
 *
 * When this activity is created it creates a GameView and sets it as the full-screen content.
 * GameView handles all drawing and game logic using a background thread.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // let Android initialise the activity before we do anything

        GameView gameView = new GameView(this); // create the custom game view — this is where all the action happens
        setContentView(gameView);               // make the GameView fill the entire screen
    }
}