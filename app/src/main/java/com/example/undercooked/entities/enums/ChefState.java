package com.example.undercooked.entities.enums; // this file belongs to the enums package

/**
 * ChefState — represents what the chef is currently doing.
 *
 * An enum is a fixed set of named values — instead of using numbers
 * (0 = idle, 1 = running...) we use readable names.
 * The state controls which animation sprite is displayed.
 */
public enum ChefState {
    IDLE,        // the chef is standing still and doing nothing
    RUNNING,     // the chef is walking or running across the kitchen
    LIFTING,     // the chef is lifting an item (reserved for future use)
    DROPPING,    // the chef is dropping an item (reserved for future use)
    INTERACTING, // the chef is interacting with a station (reserved for future use)
    CHOPPING     // the chef is chopping an ingredient — shows the chopping animation
}