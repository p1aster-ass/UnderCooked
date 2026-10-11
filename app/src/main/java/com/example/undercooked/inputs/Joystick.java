package com.example.undercooked.inputs; // this file belongs to the inputs package

import android.graphics.Canvas; // Canvas is the surface we draw onto
import android.graphics.Color;  // built-in Android colours
import android.graphics.Paint;  // Paint defines colour, size and style for drawing

/**
 * Joystick — a virtual joystick displayed on screen.
 *
 * Made of two circles:
 * - outer circle (base) — the fixed ring the player touches
 * - inner circle (thumb) — moves with the player's finger
 *
 * When the finger goes outside the base, the base itself drags along —
 * exactly like in Overcooked.
 *
 * actuatorX and actuatorY give values between -1 and 1 for each direction.
 */
public class Joystick {

    // the home position the joystick returns to when released
    private final int originalOuterCircleX;
    private final int originalOuterCircleY;

    private int outerCircleX;      // current x position of the base circle
    private int outerCircleY;      // current y position of the base circle
    private int innerCircleX;      // current x position of the thumb circle
    private int innerCircleY;      // current y position of the thumb circle
    private int outerCircleRadius; // radius of the outer base circle
    private int innerCircleRadius; // radius of the inner thumb circle

    private Paint outerPaint; // the paint (style) used to draw the base
    private Paint innerPaint; // the paint (style) used to draw the thumb

    private double  actuatorX; // how far the joystick is pushed left/right (-1 to 1)
    private double  actuatorY; // how far the joystick is pushed up/down (-1 to 1)
    private boolean isPressed; // true while the player's finger is on the joystick

    // creates a joystick with a centre position and radii for both circles
    public Joystick(int centerX, int centerY, int outerRadius, int innerRadius) {
        this.originalOuterCircleX = centerX; // save the home position
        this.originalOuterCircleY = centerY;

        this.outerCircleX = centerX; // start at the home position
        this.outerCircleY = centerY;
        this.innerCircleX = centerX; // thumb starts at the centre
        this.innerCircleY = centerY;
        this.outerCircleRadius = outerRadius; // save the radii
        this.innerCircleRadius = innerRadius;

        outerPaint = new Paint();
        outerPaint.setColor(Color.LTGRAY); // light grey colour
        outerPaint.setAlpha(25);           // almost fully transparent so it does not distract

        innerPaint = new Paint();
        innerPaint.setColor(Color.LTGRAY); // same colour as the base
        innerPaint.setAlpha(40);           // slightly less transparent than the base
    }

    // draws both circles onto the canvas
    public void draw(Canvas canvas) {
        canvas.drawCircle(outerCircleX, outerCircleY, outerCircleRadius, outerPaint); // draw base
        canvas.drawCircle(innerCircleX, innerCircleY, innerCircleRadius, innerPaint); // draw thumb
    }

    // updates the thumb position based on the current actuator values
    public void update() {
        // move the thumb by actuator * radius pixels from the base centre
        innerCircleX = (int) (outerCircleX + actuatorX * outerCircleRadius);
        innerCircleY = (int) (outerCircleY + actuatorY * outerCircleRadius);
    }

    // called when the player moves their finger — updates the actuator direction
    public void setActuator(double touchX, double touchY) {
        double deltaX    = touchX - outerCircleX; // horizontal distance from base centre
        double deltaY    = touchY - outerCircleY; // vertical distance from base centre
        double distance  = Math.sqrt(deltaX * deltaX + deltaY * deltaY); // total distance (Pythagoras)

        if (distance < outerCircleRadius) {
            // finger is inside the base — calculate actuator as a fraction of the radius
            actuatorX = deltaX / outerCircleRadius; // between -1 and 1
            actuatorY = deltaY / outerCircleRadius;
        } else {
            // finger went outside the base — drag the base toward the finger
            double excessDistance = distance - outerCircleRadius; // how far past the edge
            double directionX     = deltaX / distance; // normalised direction (-1 to 1)
            double directionY     = deltaY / distance;

            // move the base circle toward the finger — same mechanic as Overcooked
            outerCircleX += (int) (directionX * excessDistance);
            outerCircleY += (int) (directionY * excessDistance);

            // thumb is always at the edge — actuator is at maximum (1.0)
            actuatorX = directionX;
            actuatorY = directionY;
        }
    }

    // called when the player lifts their finger — resets everything to the home position
    public void resetActuator() {
        actuatorX    = 0; // stop horizontal movement
        actuatorY    = 0; // stop vertical movement
        outerCircleX = originalOuterCircleX; // snap base back to its home position
        outerCircleY = originalOuterCircleY;
    }

    // returns true if the given screen touch point is inside the joystick base
    public boolean isTouched(float x, float y) {
        // calculate the distance between the touch and the base centre
        double distance = Math.sqrt(
                Math.pow(outerCircleX - x, 2) +
                        Math.pow(outerCircleY - y, 2));
        return distance < outerCircleRadius; // touched if inside the circle
    }

    public boolean getIsPressed()               { return isPressed; }     // is the joystick being held
    public void    setIsPressed(boolean pressed) { isPressed = pressed; }  // set whether it is held
    public double  getActuatorX()               { return actuatorX; }     // horizontal push (-1 to 1)
    public double  getActuatorY()               { return actuatorY; }     // vertical push (-1 to 1)
}