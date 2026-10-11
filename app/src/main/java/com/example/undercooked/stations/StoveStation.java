package com.example.undercooked.stations; // this file belongs to the stations package

import android.util.Log; // Log prints messages for debugging

import com.example.undercooked.data.RecipeManager; // used to look up cook recipes

/**
 * StoveStation — cooks ingredients automatically after the chef places them.
 *
 * How it works:
 * 1. Chef presses USE while holding a cookable item → item goes on the stove, cooking starts
 * 2. Chef can walk away — the stove cooks on its own
 * 3. After COOK_DURATION seconds → item is ready, stove stays ON (waiting for pickup)
 * 4. Chef presses USE again → picks up the cooked item, stove turns OFF
 */
public class StoveStation {

    // the three states the stove can be in
    public enum StoveState {
        OFF,     // the stove is empty and not doing anything
        COOKING, // the stove is actively cooking an item
        DONE     // cooking is finished and the item is waiting to be picked up
    }

    private static final float COOK_DURATION = 7.0f; // seconds to cook an item — longer than the knife (2s)

    public final float mapX; // the x position of this stove on the map
    public final float mapY; // the y position of this stove on the map

    private StoveState state       = StoveState.OFF; // start in the OFF state
    private float      cookTimer   = 0f;             // how many seconds have passed since cooking started
    private String     cookingItem = null;           // the item currently being cooked
    private String     cookedItem  = null;           // the result that is ready to be picked up

    // creates a new stove at the given map position
    public StoveStation(float mapX, float mapY) {
        this.mapX = mapX; // save the x position
        this.mapY = mapY; // save the y position
    }

    // ─────────────────────────────────────────────
    //  Update — called every frame
    // ─────────────────────────────────────────────

    // advances the cooking timer — only does something while the stove is COOKING
    public void update(float dt) {
        if (state != StoveState.COOKING) return; // only count time while actively cooking

        cookTimer += dt; // add the time since the last frame

        if (cookTimer >= COOK_DURATION) { // if enough time has passed
            cookedItem  = RecipeManager.cook(cookingItem); // look up the result in RecipeManager
            cookingItem = null;           // the raw item is gone — it has been cooked
            cookTimer   = COOK_DURATION; // clamp the timer so progress stays exactly at 1.0
            state       = StoveState.DONE; // switch to DONE — stove stays visually ON
            Log.d("STOVE", "Cooking done: " + cookedItem);
        }
    }

    // ─────────────────────────────────────────────
    //  Interaction — called when chef presses USE nearby
    // ─────────────────────────────────────────────

    // handles what happens when the chef interacts with this stove
    // heldItem = what the chef is currently holding ("none" = empty hands)
    // returns  = what the chef should be holding after the interaction
    public String interact(String heldItem) {

        // case 1: cooking is finished — chef picks up the result and the stove turns off
        if (state == StoveState.DONE) {
            String result = cookedItem; // save the cooked item
            cookedItem = null;          // clear the stove
            state      = StoveState.OFF; // turn the stove off
            Log.d("STOVE", "Chef picked up: " + result);
            return result != null ? result : "none"; // give the item to the chef
        }

        // case 2: still cooking — the chef cannot interact with it yet
        if (state == StoveState.COOKING) {
            Log.d("STOVE", "Still cooking...");
            return heldItem; // chef keeps whatever they were holding
        }

        // case 3: stove is OFF — start cooking if the chef has a cookable item
        if (!heldItem.equals("none") && RecipeManager.isCookable(heldItem)) {
            cookingItem = heldItem;      // put the item on the stove
            cookTimer   = 0f;           // reset the cooking timer
            state       = StoveState.COOKING; // start cooking
            Log.d("STOVE", "Started cooking: " + heldItem);
            return "none"; // chef's hands are now empty
        }

        return heldItem; // the item cannot be cooked — nothing happens
    }

    // ─────────────────────────────────────────────
    //  Getters
    // ─────────────────────────────────────────────

    public StoveState getState()       { return state; }                                         // current stove state
    public boolean    isOff()          { return state == StoveState.OFF; }                       // true when stove is empty and off
    public boolean    isCooking()      { return state == StoveState.COOKING; }                   // true while cooking
    public boolean    isDone()         { return state == StoveState.DONE; }                      // true when food is ready
    public boolean    isOn()           { return state == StoveState.COOKING || state == StoveState.DONE; } // stove sprite is ON while cooking or done
    public String     getCookingItem() { return cookingItem; }                                   // item currently being cooked
    public String     getCookedItem()  { return cookedItem; }                                    // item ready for pickup

    // returns cooking progress as a value between 0.0 (just started) and 1.0 (done)
    // used by LevelIconManager to fill the clock icon above the stove
    public float getProgress() {
        if (state == StoveState.OFF) return 0f;               // not cooking — no progress
        return Math.min(cookTimer / COOK_DURATION, 1f);       // clamp to 1.0 maximum
    }
}