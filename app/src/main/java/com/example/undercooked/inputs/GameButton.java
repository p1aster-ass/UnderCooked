// ══════════════════════════════════════════════════
// FILE: GameButton.java
// ══════════════════════════════════════════════════
package com.example.undercooked.inputs; // this file belongs to the inputs package

import android.graphics.Canvas; // Canvas is the surface we draw onto
import android.graphics.Color;  // built-in Android colour constants
import android.graphics.Paint;  // Paint defines colour and style for drawing
import android.graphics.RectF;  // RectF is a rectangle with float coordinates

/**
 * GameButton — a simple on-screen button that can be pressed.
 * Used for the USE and DROP buttons during gameplay.
 */
public class GameButton {

    private RectF  bounds;    // the rectangular clickable area of the button on screen
    private String label;     // the text shown on the button (e.g. "USE" or "DROP")
    private Paint  paint;     // the paint used to draw the button background
    private Paint  textPaint; // the paint used to draw the button label
    private int    color;     // the background colour of the button

    // creates a new button with a label, position, size and colour
    public GameButton(String label, float x, float y, float width, float height, int color) {
        this.label  = label;  // save the label text
        this.bounds = new RectF(x, y, x + width, y + height); // define the clickable area
        this.color  = color;  // save the background colour

        paint = new Paint();
        paint.setColor(color);   // set the background colour
        paint.setAlpha(180);     // semi-transparent so the map shows through slightly

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);             // white text so it is readable on any background
        textPaint.setTextSize(40);                   // text size in pixels
        textPaint.setTextAlign(Paint.Align.CENTER);  // centre the text horizontally
    }

    // draws the button on the canvas
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(bounds, 20, 20, paint); // draw a rounded rectangle as the background
        // draw the label in the centre of the button — +15 offsets for vertical centering
        canvas.drawText(label, bounds.centerX(), bounds.centerY() + 15, textPaint);
    }

    // returns true if the given touch coordinates are inside this button
    public boolean isPressed(float touchX, float touchY) {
        return bounds.contains(touchX, touchY); // RectF.contains checks if the point is inside
    }
}