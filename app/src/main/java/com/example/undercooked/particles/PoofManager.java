package com.example.undercooked.particles; // this file belongs to the particles package

import android.content.Context;        // gives access to app resources
import android.graphics.Bitmap;        // Bitmap is an image
import android.graphics.BitmapFactory; // loads images from the drawable folder
import android.graphics.Canvas;        // Canvas is the surface we draw onto

/**
 * PoofManager — plays a small poof (smoke/magic) effect when the chef does something.
 *
 * The effect is made of 5 images (poof_01 to poof_05) shown one after another
 * like a flip-book animation. It plays once and then disappears.
 */
public class PoofManager {

    private final Bitmap[] poofFrames;  // array of 5 images that make up the animation
    private boolean isPlaying    = false; // true while the animation is running
    private int     currentFrame = 0;    // which frame is currently shown (0 to 4)
    private long    lastFrameTime = 0;   // when the current frame started showing (milliseconds)
    private final int POOF_SPEED = 60;   // how many milliseconds each frame is shown — 60ms is fast

    private float drawX; // x position where the effect is drawn
    private float drawY; // y position where the effect is drawn

    // loads all 5 poof images from the drawable folder
    public PoofManager(Context context) {
        poofFrames = new Bitmap[5]; // create an array to hold 5 images

        for (int i = 0; i < 5; i++) { // loop through all 5 frames
            String name = "poof_0" + (i + 1); // build file names: "poof_01", "poof_02", etc.
            // find the resource ID for the image by its name
            int id = context.getResources().getIdentifier(
                    name, "drawable", context.getPackageName());
            if (id != 0) { // if the image exists
                poofFrames[i] = BitmapFactory.decodeResource(context.getResources(), id); // load it
            }
        }
    }

    // starts the poof animation at the given position on screen
    public void triggerPoof(float x, float y) {
        this.drawX         = x;     // save the x position
        this.drawY         = y;     // save the y position
        this.currentFrame  = 0;     // start from the first frame
        this.isPlaying     = true;  // begin the animation
        this.lastFrameTime = System.currentTimeMillis(); // record when we started
    }

    // draws the current frame and advances to the next one when enough time has passed
    // this method is called every game frame
    public void drawAndUpdate(Canvas canvas) {
        if (!isPlaying) return;                        // if not playing, nothing to draw
        if (poofFrames[currentFrame] == null) return;  // if the image is missing, skip

        // calculate the top-left corner so the image is centred on the given position
        float finalX = drawX - (poofFrames[currentFrame].getWidth()  / 2f);
        float finalY = drawY - (poofFrames[currentFrame].getHeight() / 2f);

        canvas.drawBitmap(poofFrames[currentFrame], finalX, finalY, null); // draw the frame

        // check if enough time has passed to show the next frame
        if (System.currentTimeMillis() - lastFrameTime > POOF_SPEED) {
            currentFrame++;                               // move to the next frame
            lastFrameTime = System.currentTimeMillis(); // reset the frame timer

            if (currentFrame >= 5) {  // if we have shown all 5 frames
                isPlaying = false;    // the animation is finished
            }
        }
    }
}