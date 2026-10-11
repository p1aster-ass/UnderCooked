package com.example.undercooked.entities.characters; // this file belongs to the characters package

import android.content.Context;        // gives access to app resources
import android.graphics.Bitmap;        // Bitmap is an image that can be drawn on a Canvas
import android.graphics.BitmapFactory; // loads images from the drawable folder
import android.graphics.Canvas;        // Canvas is the surface we draw onto
import android.util.Log;               // Log prints messages to help us debug

import com.example.undercooked.entities.base.GameObject;  // Chef1 extends GameObject
import com.example.undercooked.entities.enums.ChefState;  // the possible states of the chef
import com.example.undercooked.entities.enums.Direction;  // the direction the chef is facing

/**
 * Chef1 — the chef that the player controls.
 *
 * Extends GameObject, so it has a position, width and height.
 * Responsible for:
 * - showing the correct animation based on state and direction
 * - tracking what item the chef is holding
 */
public class Chef1 extends GameObject {

    // how long each animation frame is displayed (in seconds)
    private static final float ANIM_SPEED_NORMAL   = 0.12f; // normal animation speed (idle / running)
    private static final float ANIM_SPEED_CHOPPING = 0.08f; // chopping is a bit faster
    private static final int   ANIM_FRAMES         = 6;     // each animation has 6 frames

    private final Context  context;       // saved so we can load sprite images later
    private       Bitmap   currentFrame;  // the image that is currently drawn on screen
    private       ChefState state     = ChefState.IDLE;   // starting state — standing still
    private       Direction direction  = Direction.SOUTH;  // starting direction — facing down
    private       int      frameIndex  = 1;    // which frame we are on (1 to 6)
    private       float    animTimer   = 0f;   // time elapsed since the last frame change
    private       String   heldItem    = "none"; // what the chef is holding — "none" means empty hands

    // creates a new chef at the given position
    public Chef1(Context context, float x, float y) {
        super(x, y, 64, 64); // call the GameObject constructor — 64x64 pixels
        this.context = context; // save the context for loading images
        refreshBitmap(); // load the first frame immediately
    }

    // called every frame — advances the animation timer and loads the correct sprite
    @Override
    public void update(float deltaTime) {
        // choose animation speed based on current state
        float speed = (state == ChefState.CHOPPING) ? ANIM_SPEED_CHOPPING : ANIM_SPEED_NORMAL;

        animTimer += deltaTime; // add the time that passed since the last frame

        if (animTimer >= speed) { // if enough time has passed, move to the next frame
            animTimer  = 0f;      // reset the timer
            frameIndex = (frameIndex % ANIM_FRAMES) + 1; // cycle 1→2→3→4→5→6→1
        }

        refreshBitmap(); // load the sprite that matches the current state and frame
    }

    // loads the sprite image that matches the current state, direction and frame number
    private void refreshBitmap() {
        String resName = buildResourceName(); // build the file name like "running_down_03_1"
        // ask Android to find the resource ID for this file name
        int resId = context.getResources().getIdentifier(
                resName, "drawable", context.getPackageName());

        if (resId != 0) { // if the image was found
            currentFrame = BitmapFactory.decodeResource(context.getResources(), resId); // load it
        } else {
            Log.e("CHEF_ANIM", "Missing sprite: " + resName); // print an error if the file is missing
        }
    }

    // builds the sprite file name based on the current state, direction and frame index
    // example results: "running_down_03_1", "idle_up_01_1", "interacting_top_02_1"
    private String buildResourceName() {
        String dir = directionToString(); // get the direction as a string

        if (state == ChefState.RUNNING)   return "running_"     + dir + "_0" + frameIndex + "_1";
        if (state == ChefState.CHOPPING)  return "interacting_" + dir + "_0" + frameIndex + "_1";
        return "idle_" + dir + "_0" + frameIndex + "_1"; // default: idle animation
    }

    // converts the Direction enum to the string used in sprite file names
    private String directionToString() {
        if (direction == Direction.NORTH) {
            // north is called "up" in idle sprites and "top" in all other sprites
            return (state == ChefState.IDLE) ? "up" : "top";
        }
        if (direction == Direction.SOUTH) return "down";  // south = looking down
        if (direction == Direction.EAST)  return "right"; // east = looking right
        if (direction == Direction.WEST)  return "left";  // west = looking left
        return "down"; // default fallback
    }

    // draws the chef's current sprite frame onto the canvas
    @Override
    public void render(Canvas canvas) {
        if (currentFrame == null) return; // if there is no image yet, do nothing

        // draw the image centred on the chef's position
        canvas.drawBitmap(currentFrame,
                position.x - currentFrame.getWidth()  / 2f, // shift left by half the width
                position.y - currentFrame.getHeight() / 2f, // shift up by half the height
                null); // null = no special paint effects
    }

    // ── Getters and Setters ───────────────────────

    public String    getHeldItem()             { return heldItem; }  // returns what the chef is holding
    public void      setHeldItem(String item)  { heldItem = item; }  // changes what the chef holds
    public ChefState getState()                { return state; }     // returns the current state
    public void      setState(ChefState s)     { state = s; }        // changes the state
    public Direction getDirection()            { return direction; } // returns the current direction
    public void      setDirection(Direction d) { direction = d; }    // changes the direction
}