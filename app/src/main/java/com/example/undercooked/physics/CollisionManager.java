package com.example.undercooked.physics; // this file belongs to the physics package

import android.graphics.RectF;   // RectF is a rectangle defined by four float coordinates
import java.util.ArrayList;       // ArrayList is a list that can grow and shrink dynamically

/**
 * CollisionManager — prevents the chef from walking through walls and furniture.
 *
 * Every solid object on the map is stored as a rectangle (RectF).
 * Each frame we check whether the chef's next position would overlap any of these
 * rectangles. If it would, we block the movement or slide the chef along the wall.
 */
public class CollisionManager {

    // how far from the top of a table we shift the collision box up — makes it feel natural
    private final float TABLE_TOP_OFFSET = -55f;

    // how far we shrink the chef's hitbox on each side — makes it easier to walk in tight spaces
    private final float CHEF_HITBOX_TOP   = 30f;
    private final float CHEF_HITBOX_LEFT  = 30f;
    private final float CHEF_HITBOX_RIGHT = 30f;

    private ArrayList<RectF> solidObjects; // the list of all solid rectangles in the current level
    private RectF chefHitbox;              // reused rectangle that represents the chef's body

    // sets up the empty collision lists
    public CollisionManager() {
        solidObjects = new ArrayList<>(); // start with an empty list — filled by loadLevelCollisions
        chefHitbox   = new RectF();       // create an empty rectangle that we will reuse every frame
    }

    // fills the solid object list with all the walls and furniture for the given level
    public void loadLevelCollisions(int levelNumber) {
        solidObjects.clear(); // remove any walls from a previous level

        if (levelNumber == 1) {

            // Kitchen walls and counters
            addSolidObject(257,  306,  340,  631);  // left wall of the kitchen
            addSolidObject(319,  210,  943,  308);  // top wall of the kitchen
            addSolidObject(468,  428,  790,  538);  // center island / prep counter
            addSolidObject(925,  208, 1011,  565);  // right wall of the kitchen
            addSolidObject(247,  626,  755,  681);  // bottom wall of the kitchen
            addSolidObject(722,  678, 1173,  765);  // second bottom wall of the kitchen

            addSolidObject(1006, 427, 1175,  594);  // hallway between kitchen and ingredients area

            // Ingredient hallway objects
            addSolidObject(1176, 348, 1259,  497);  // onion crate
            addSolidObject(1174, 267, 1679,  426);  // meat fridge
            addSolidObject(1146, 731, 1245,  925);  // white/purple cupboard
            addSolidObject(1429, 598, 1516,  751);  // cucumber / pickle crate
            addSolidObject(1515, 514, 1547, 1382);  // right wall of the ingredient hallway
            addSolidObject(1142,1170, 1543, 1382);  // fish tank station at the bottom
            addSolidObject(1427, 847, 1512, 1088);  // tomato crate
            addSolidObject(1431,1085, 1513, 1238);  // water bucket
            addSolidObject(1010,1017, 1176, 1469);  // left wall of the ingredient hallway / right wall of restaurant

            addSolidObject(1513, 512, 1679,  623);  // bottom hallway connecting ingredients to bathroom

            // Bathroom
            addSolidObject(1661, 199, 1933,  298);  // upper bathroom wall
            addSolidObject(1764, 287, 1848,  387);  // actual toilet
            addSolidObject(1933,  95, 1966,  623);  // right wall of the bathroom
            addSolidObject(1648, 595, 1966,  623);  // bottom wall of the bathroom
            addSolidObject(1862, 515, 1963,  623);  // wet floor sign

            // Restaurant area
            addSolidObject(750,  795, 1177,  929);  // upper wall between restaurant and ingredient area
            addSolidObject(219,  768,  254, 1466);  // left wall of the restaurant
            addSolidObject(221, 1436,  501, 1635);  // left side of the exit door
            addSolidObject(755, 1436, 1049, 1651);  // right side of the exit door
            addSolidObject(245,  791,  753,  976);  // bookshelf along the wall
            addSolidObject(760, 1038,  921, 1140);  // cashier counter
            addSolidObject(223, 1060,  690, 1179);  // refrigerated display case
            addSolidObject(247, 1269,  331, 1440);  // small menu sign
            addSolidObject(847, 1350, 1037, 1459);  // green suitcase
            addSolidObject(952, 1200, 1040, 1461);  // orange bench / seating
        }
    }

    // creates a solid rectangle and adds it to the list
    // TABLE_TOP_OFFSET shifts the top edge up so the chef can walk behind furniture naturally
    public void addSolidObject(float left, float top, float right, float bottom) {
        solidObjects.add(new RectF(left, top + TABLE_TOP_OFFSET, right, bottom));
    }

    /**
     * Smart sliding collision — tries to let the chef move as much as possible.
     *
     * 1. Try full movement (both X and Y)
     * 2. If blocked — try X only (slides along horizontal surfaces)
     * 3. If still blocked — try Y only (slides along vertical surfaces)
     * 4. If completely blocked — return zero velocity
     *
     * This is the same technique used in most top-down games.
     */
    public float[] getAdjustedVelocity(float currentX, float currentY,
                                       float vx, float vy,
                                       float sw, float sh,
                                       float mw, float mh) {

        float nextX = currentX + vx; // where the chef would be after moving X
        float nextY = currentY + vy; // where the chef would be after moving Y

        // step 1 — try full diagonal movement
        if (canMoveTo(nextX, nextY, sw, sh, mw, mh)) {
            return new float[]{vx, vy}; // no collision — move normally
        }

        // step 2 — try moving only along X (slide along a vertical wall)
        if (canMoveTo(nextX, currentY, sw, sh, mw, mh)) {
            return new float[]{vx, 0}; // move right/left but not up/down
        }

        // step 3 — try moving only along Y (slide along a horizontal wall)
        if (canMoveTo(currentX, nextY, sw, sh, mw, mh)) {
            return new float[]{0, vy}; // move up/down but not right/left
        }

        // step 4 — completely blocked in every direction
        return new float[]{0, 0};
    }

    // checks whether the chef can move to the given map offset without hitting a wall
    public boolean canMoveTo(float targetMapX, float targetMapY,
                             float screenW, float screenH,
                             float mapW, float mapH) {

        float chefRadius = 32f; // the chef occupies roughly a 64x64 area

        // calculate the maximum and minimum map offsets so the chef cannot walk off the map edges
        float maxMapX = (screenW / 2f) - chefRadius; // how far right the map can shift
        float minMapX = (screenW / 2f) + chefRadius - mapW; // how far left the map can shift
        float maxMapY = (screenH / 2f) - chefRadius; // how far down the map can shift
        float minMapY = (screenH / 2f) + chefRadius - mapH; // how far up the map can shift

        // reject positions that would put the chef outside the map boundaries
        if (targetMapX > maxMapX || targetMapX < minMapX) return false;
        if (targetMapY > maxMapY || targetMapY < minMapY) return false;

        // calculate where the chef would be on the map in pixels
        float chefOnMapX = (screenW / 2f) - targetMapX;
        float chefOnMapY = (screenH / 2f) - targetMapY;

        // build the chef's hitbox — slightly smaller than the sprite so movement feels fair
        chefHitbox.set(
                chefOnMapX - CHEF_HITBOX_LEFT,  // left edge
                chefOnMapY + CHEF_HITBOX_TOP,   // top edge (pushed down so feet are checked)
                chefOnMapX + CHEF_HITBOX_RIGHT, // right edge
                chefOnMapY + 32                 // bottom edge
        );

        // check if the chef's hitbox overlaps any solid object
        for (int i = 0; i < solidObjects.size(); i++) {
            if (RectF.intersects(chefHitbox, solidObjects.get(i))) {
                return false; // collision detected — cannot move here
            }
        }

        return true; // no collision — the chef can move here
    }
}