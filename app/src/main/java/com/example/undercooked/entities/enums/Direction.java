package com.example.undercooked.entities.enums; // this file belongs to the enums package

/**
 * Direction — represents which way the chef is facing.
 *
 * The direction determines which set of animation sprites is used.
 * For example SOUTH means the chef is looking toward the camera (down).
 */
public enum Direction {
    NORTH, // the chef is facing up (away from the camera)
    SOUTH, // the chef is facing down (toward the camera) — this is the default
    EAST,  // the chef is facing right
    WEST   // the chef is facing left
}