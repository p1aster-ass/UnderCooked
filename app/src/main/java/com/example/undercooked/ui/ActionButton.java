package com.example.undercooked.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

public class ActionButton {
    private final RectF rect;
    private final String text;
    private boolean isPressed = false;

    private final Paint paintBase;
    private final Paint paintSurface;
    private final Paint paintHighlight;
    private final Paint paintShadow;
    private final Paint paintText;

    private final float baseDepth = 16f;
    private final float borderWidth = 6f;

    public ActionButton(String text, float x, float y, float width, float height, Typeface customFont) {
        this.text = text;
        this.rect = new RectF(x, y, x + width, y + height);

        paintBase = new Paint();
        paintBase.setColor(Color.parseColor("#3a3a50"));
        paintBase.setStyle(Paint.Style.FILL);

        paintSurface = new Paint();
        paintSurface.setColor(Color.parseColor("#c8a580"));
        paintSurface.setStyle(Paint.Style.FILL);

        paintHighlight = new Paint();
        paintHighlight.setColor(Color.parseColor("#dfc2a0"));
        paintHighlight.setStyle(Paint.Style.FILL);

        paintShadow = new Paint();
        paintShadow.setColor(Color.parseColor("#a57b58"));
        paintShadow.setStyle(Paint.Style.FILL);

        paintText = new Paint();
        paintText.setColor(Color.parseColor("#6b5052"));

        // --- INCREASED TEXT SIZE FROM 40 TO 70 ---
        paintText.setTextSize(70);
        paintText.setTextAlign(Paint.Align.CENTER);

        if (customFont != null) {
            paintText.setTypeface(customFont);
        } else {
            paintText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }
    }

    public void draw(Canvas canvas) {
        float pressOffset = isPressed ? baseDepth - 4f : 0f;

        RectF baseRect = new RectF(rect.left, rect.top + 10, rect.right, rect.bottom);
        canvas.drawRoundRect(baseRect, 8, 8, paintBase);

        float surfaceTop = rect.top + pressOffset;
        float surfaceBottom = rect.bottom - baseDepth + pressOffset;
        RectF surfaceRect = new RectF(rect.left, surfaceTop, rect.right, surfaceBottom);

        canvas.drawRoundRect(surfaceRect, 8, 8, paintSurface);

        canvas.drawRect(surfaceRect.left + 8, surfaceRect.top, surfaceRect.right - 8, surfaceRect.top + borderWidth, paintHighlight);
        canvas.drawRect(surfaceRect.left, surfaceRect.top + 8, surfaceRect.left + borderWidth, surfaceRect.bottom - 8, paintHighlight);

        canvas.drawRect(surfaceRect.left + 8, surfaceRect.bottom - borderWidth, surfaceRect.right - 8, surfaceRect.bottom, paintShadow);
        canvas.drawRect(surfaceRect.right - borderWidth, surfaceRect.top + 8, surfaceRect.right, surfaceRect.bottom - 8, paintShadow);

        // The text will automatically center itself vertically based on the new size!
        float textY = surfaceTop + (surfaceRect.height() / 2f) - ((paintText.descent() + paintText.ascent()) / 2f);
        canvas.drawText(text, surfaceRect.centerX(), textY, paintText);
    }

    public boolean isTouched(float touchX, float touchY) {
        return rect.contains(touchX, touchY);
    }

    public void setPressed(boolean pressed) {
        this.isPressed = pressed;
    }
}