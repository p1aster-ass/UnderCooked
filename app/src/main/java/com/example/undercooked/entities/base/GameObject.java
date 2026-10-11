package com.example.undercooked.entities.base; // this file belongs to the base entities package

import android.graphics.Canvas; // Canvas is the surface we draw everything onto
import android.graphics.PointF; // PointF holds a pair of float coordinates (x, y)

/**
 * GameObject — the base class for every physical object in the game.
 *
 * Every object in the game (chef, station, particles) extends this class.
 * It stores position, width and height, and forces every child class
 * to implement update() and render().
 *
 * abstract means you cannot create a GameObject directly —
 * you can only create classes that extend it.
 */
public abstract class GameObject {

    protected PointF position; // the current position of this object on screen (x, y)
    protected float  width;    // the width of the object in pixels
    protected float  height;   // the height of the object in pixels

    // creates a new object with a position and size
    public GameObject(float x, float y, float width, float height) {
        this.position = new PointF(x, y); // create a position point with the given coordinates
        this.width    = width;             // save the width
        this.height   = height;            // save the height
    }

    // abstract — every class that extends GameObject MUST implement this method
    // update is called every frame to run the object's logic (animation, movement, etc.)
    // deltaTime is the time in seconds since the last frame
    public abstract void update(float deltaTime);

    // abstract — every class that extends GameObject MUST implement this method
    // render is called every frame to draw the object onto the canvas
    public abstract void render(Canvas canvas);

    // returns the current position of this object
    public PointF getPosition() {
        return position;
    }

    // moves this object to a new position
    public void setPosition(float x, float y) {
        this.position.set(x, y); // set() updates the x and y values inside the existing PointF
    }
}