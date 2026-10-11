package com.example.undercooked.stations; // this file belongs to the stations package

import android.graphics.Bitmap;  // Bitmap is an image
import android.graphics.Canvas;  // Canvas is the surface we draw onto
import android.util.Log;         // Log prints messages to help us debug

import com.example.undercooked.data.RecipeManager; // used to look up combine recipes

import java.util.HashMap; // HashMap stores key-value pairs like a dictionary
import java.util.Map;     // the base interface for HashMap

/**
 * PlacematStation — a mixing station where the chef combines two ingredients.
 *
 * The placemat holds exactly ONE item at a time.
 *
 * - Chef holding item + USE near empty placemat  → places item, hands free
 * - Chef holding item + USE near placemat with item → tries to combine both
 *     → recipe found:  placemat holds the result, chef's hands are free
 *     → no recipe:     chef keeps their item, nothing changes
 * - Chef empty-handed + USE near placemat        → picks up the item
 */
public class PlacematStation {

    // some ingredient sprites are not perfectly centred — this map fine-tunes the Y position
    // positive value = shift the image down, negative value = shift the image up
    private static final Map<String, Float> Y_OFFSET = new HashMap<>();
    static {
        Y_OFFSET.put("meat", 10f);  // meat sprite sits a bit too high — push it down
        Y_OFFSET.put("rice", -5f);  // rice sprite sits a bit too low — push it up
    }

    public final float mapX; // the x position of this placemat on the map
    public final float mapY; // the y position of this placemat on the map

    private String heldItem = null; // the item currently on this placemat — null means empty

    // creates a new placemat at the given map position
    public PlacematStation(float mapX, float mapY) {
        this.mapX = mapX; // save the x position
        this.mapY = mapY; // save the y position
    }

    // ─────────────────────────────────────────────
    //  Interaction — called when chef presses USE nearby
    // ─────────────────────────────────────────────

    // handles what happens when the chef interacts with this placemat
    // chefItem = what the chef is currently holding ("none" = empty hands)
    // returns = what the chef should be holding after the interaction
    public String interact(String chefItem) {

        boolean chefIsEmpty   = chefItem.equals("none"); // true if the chef has nothing in hand
        boolean placematEmpty = heldItem == null;        // true if the placemat has no item

        // case 1: chef has empty hands — pick up whatever is on the placemat
        if (chefIsEmpty) {
            if (!placematEmpty) { // only pick up if there is actually something here
                String pickup = heldItem; // save the item before clearing it
                heldItem = null;          // the placemat is now empty
                Log.d("PLACEMAT", "Picked up: " + pickup);
                return pickup; // chef now holds this item
            }
            Log.d("PLACEMAT", "Nothing to pick up");
            return "none"; // nothing to pick up — chef stays empty
        }

        // case 2: chef is holding something and the placemat is empty — place the item
        if (placematEmpty) {
            heldItem = chefItem;  // put the item on the placemat
            Log.d("PLACEMAT", "Placed on empty placemat: " + chefItem);
            return "none"; // chef's hands are now empty
        }

        // case 3: chef is holding something and the placemat already has an item
        // try to combine the two items using RecipeManager
        Log.d("PLACEMAT", "Trying to combine: [" + heldItem + "] + [" + chefItem + "]");
        String result = RecipeManager.combine2(heldItem, chefItem); // look up the recipe

        if (result != null) { // a recipe was found for these two items
            Log.d("PLACEMAT", "Recipe found! " + heldItem + " + " + chefItem + " = " + result);
            heldItem = result; // replace the placemat item with the combined result
            return "none";     // chef's hands are free
        }

        // no recipe exists for these two items — nothing changes
        Log.d("PLACEMAT", "No recipe for [" + heldItem + "] + [" + chefItem + "]");
        return chefItem; // chef keeps their item
    }

    // ─────────────────────────────────────────────
    //  Drawing — shows the item sitting on the placemat
    // ─────────────────────────────────────────────

    // draws the item on the placemat (if there is one)
    public void draw(Canvas canvas, Map<String, Bitmap> foodSprites,
                     float cameraX, float cameraY) {

        if (heldItem == null) return; // nothing on the placemat — nothing to draw

        // calculate the screen position from the map position and camera offset
        float cx = cameraX + mapX;          // screen x
        float cy = cameraY + mapY - 80f;    // screen y — shifted up so it sits on top of the station

        Bitmap bmp = foodSprites.get(heldItem); // get the image for this item
        if (bmp == null) {
            Log.w("PLACEMAT", "No sprite for: " + heldItem); // log a warning if image is missing
            return;
        }

        // apply the per-item Y correction (some sprites are not perfectly centred)
        float yOffset = Y_OFFSET.containsKey(heldItem) ? Y_OFFSET.get(heldItem) : 0f;

        // draw the image centred on the placemat position
        canvas.drawBitmap(bmp,
                cx - bmp.getWidth()  / 2f,          // shift left by half the image width
                cy - bmp.getHeight() / 2f + yOffset, // shift up by half the image height + correction
                null);
    }

    // ─────────────────────────────────────────────
    //  Getters
    // ─────────────────────────────────────────────

    public boolean isEmpty()                { return heldItem == null; }           // true if nothing is on the placemat
    public String  getHeldItem()            { return heldItem; }                   // returns the item on the placemat
    public String  getContentsDescription() { return heldItem != null ? heldItem : "empty"; } // for debug logs
}