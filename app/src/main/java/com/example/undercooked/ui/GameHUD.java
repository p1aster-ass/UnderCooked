package com.example.undercooked.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;

import com.example.undercooked.data.OrderManager;
import com.example.undercooked.data.OrderManager.Order;

import java.util.List;
import java.util.Map;

/**
 * Draws the game HUD:
 * - Top bar: money (left) and timer (right)
 * - Pause button (top left)
 * - Right panel: list of dishes with checkmarks
 * - Delivery notification (shown for 2 seconds using Handler)
 */
public class GameHUD {

    // Panel layout
    private static final float PANEL_WIDTH    = 320f;
    private static final float ROW_HEIGHT     = 80f;
    private static final float ICON_SIZE      = 56f;
    private static final float PADDING        = 16f;
    private static final float TOP_BAR_HEIGHT = 80f;

    // Pause button
    private static final float PAUSE_BTN_X = 16f;
    private static final float PAUSE_BTN_Y = 100f;
    private static final float PAUSE_BTN_W = 70f;
    private static final float PAUSE_BTN_H = 70f;

    // Colors
    private static final int COLOR_PANEL_BG  = Color.argb(220, 40,  30,  20);
    private static final int COLOR_ROW_BG    = Color.argb(180, 60,  50,  35);
    private static final int COLOR_ROW_DONE  = Color.argb(180, 40,  80,  40);
    private static final int COLOR_GOLD      = Color.parseColor("#FFD700");
    private static final int COLOR_ORANGE    = Color.parseColor("#FF8C00");
    private static final int COLOR_TIMER_LOW = Color.parseColor("#FF4444");

    private final Paint paintBg   = new Paint();
    private final Paint paintText = new Paint();
    private final Paint paintIcon = new Paint();

    private final Typeface font;
    private final int      screenW;
    private final int      screenH;

    // Pause button touch area
    private final RectF pauseRect = new RectF(
            PAUSE_BTN_X, PAUSE_BTN_Y,
            PAUSE_BTN_X + PAUSE_BTN_W,
            PAUSE_BTN_Y + PAUSE_BTN_H);

    // ── Handler for delivery notifications ────────────────
    // Shows "+$X  Dish Name" for 2 seconds after delivery
    private final Handler  notifHandler = new Handler(Looper.getMainLooper());
    private       String   notifText    = null;  // null = nothing to show
    private       float    notifAlpha   = 1.0f;

    private static final long NOTIF_DURATION_MS = 2000L; // show for 2 seconds

    public GameHUD(Typeface font, int screenW, int screenH) {
        this.font    = font;
        this.screenW = screenW;
        this.screenH = screenH;

        paintText.setAntiAlias(true);
        paintText.setTypeface(font != null ? font : Typeface.DEFAULT);

        paintBg.setAntiAlias(true);
        paintIcon.setAntiAlias(true);
    }

    // ─────────────────────────────────────────────
    //  Delivery notification (uses Handler)
    // ─────────────────────────────────────────────

    /**
     * Call this when a dish is successfully delivered.
     * Shows "+$price  dishName" on screen for 2 seconds.
     * Uses Handler to clear the message after the delay.
     */
    public void showDeliveryNotif(String dishName, int price) {
        notifText  = "+$" + price + "   " + dishName + "!";
        notifAlpha = 1.0f;

        // Cancel any pending hide
        notifHandler.removeCallbacksAndMessages(null);

        // Hide after 2 seconds
        notifHandler.postDelayed(() -> {
            notifText  = null;
            notifAlpha = 0f;
        }, NOTIF_DURATION_MS);
    }

    // ─────────────────────────────────────────────
    //  Main draw
    // ─────────────────────────────────────────────

    public void draw(Canvas canvas,
                     OrderManager orderManager,
                     Map<String, Bitmap> foodSprites,
                     Bitmap checkmarkSprite,
                     Bitmap pauseButtonSprite) {

        drawTopBar(canvas, orderManager);
        drawPauseButton(canvas, pauseButtonSprite);
        drawOrderPanel(canvas, orderManager, foodSprites, checkmarkSprite);
        if (notifText != null) drawNotification(canvas);
    }

    // ─────────────────────────────────────────────
    //  Top bar
    // ─────────────────────────────────────────────

    private void drawTopBar(Canvas canvas, OrderManager orderManager) {
        paintBg.setColor(COLOR_PANEL_BG);
        canvas.drawRect(0, 0, screenW, TOP_BAR_HEIGHT, paintBg);

        float midY = TOP_BAR_HEIGHT / 2f + 13f;

        // Money — gold color, game font
        paintText.setTextSize(34f);
        paintText.setColor(COLOR_GOLD);
        canvas.drawText("$" + orderManager.getMoney(), screenW / 2f - 160f, midY, paintText);

        // Timer — red when < 15s, game font
        float timeLeft = orderManager.getTimeLeft();
        paintText.setColor(timeLeft < 15f ? COLOR_TIMER_LOW : Color.WHITE);
        canvas.drawText(orderManager.getTimeString(), screenW / 2f + 20f, midY, paintText);
    }

    // ─────────────────────────────────────────────
    //  Pause button
    // ─────────────────────────────────────────────

    private void drawPauseButton(Canvas canvas, Bitmap pauseBtn) {
        if (pauseBtn != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(pauseBtn,
                    (int) PAUSE_BTN_W, (int) PAUSE_BTN_H, true);
            canvas.drawBitmap(scaled, PAUSE_BTN_X, PAUSE_BTN_Y, paintIcon);
        } else {
            paintBg.setColor(Color.argb(200, 80, 60, 40));
            canvas.drawRoundRect(pauseRect, 8, 8, paintBg);
            paintText.setTextSize(26f);
            paintText.setColor(Color.WHITE);
            canvas.drawText("||", pauseRect.centerX() - 10f, pauseRect.centerY() + 10f, paintText);
        }
    }

    // ─────────────────────────────────────────────
    //  Order panel
    // ─────────────────────────────────────────────

    private void drawOrderPanel(Canvas canvas,
                                OrderManager orderManager,
                                Map<String, Bitmap> foodSprites,
                                Bitmap checkmark) {
        List<Order> orders = orderManager.getOrders();
        float panelX = screenW - PANEL_WIDTH;
        float panelY = TOP_BAR_HEIGHT;
        float panelH = orders.size() * ROW_HEIGHT + PADDING;

        paintBg.setColor(COLOR_PANEL_BG);
        canvas.drawRect(panelX, panelY, screenW, panelY + panelH, paintBg);

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            float rowY  = panelY + PADDING / 2f + i * ROW_HEIGHT;

            // Row background
            paintBg.setColor(order.completed ? COLOR_ROW_DONE : COLOR_ROW_BG);
            canvas.drawRoundRect(
                    panelX + PADDING / 2f, rowY,
                    screenW - PADDING / 2f, rowY + ROW_HEIGHT - 4f,
                    8, 8, paintBg);

            // Food icon
            Bitmap icon = foodSprites.get(order.itemName);
            if (icon != null) {
                canvas.drawBitmap(
                        Bitmap.createScaledBitmap(icon, (int) ICON_SIZE, (int) ICON_SIZE, true),
                        panelX + PADDING,
                        rowY + (ROW_HEIGHT - ICON_SIZE) / 2f,
                        paintIcon);
            }

            float textX = panelX + PADDING + ICON_SIZE + PADDING;
            float textY = rowY + ROW_HEIGHT / 2f - 4f;

            // Dish name — game font
            paintText.setTextSize(22f);
            paintText.setColor(order.completed ? Color.LTGRAY : Color.WHITE);
            canvas.drawText(order.displayName, textX, textY, paintText);

            // Price — game font, colored
            paintText.setTextSize(18f);
            paintText.setColor(order.price >= 20 ? COLOR_ORANGE : COLOR_GOLD);
            canvas.drawText("$" + order.price, textX, textY + 24f, paintText);

            // Checkmark
            if (order.completed) {
                if (checkmark != null) {
                    float ckSize = 40f;
                    canvas.drawBitmap(
                            Bitmap.createScaledBitmap(checkmark, (int) ckSize, (int) ckSize, true),
                            screenW - PADDING - ckSize,
                            rowY + (ROW_HEIGHT - ckSize) / 2f,
                            paintIcon);
                } else {
                    paintText.setTextSize(32f);
                    paintText.setColor(Color.GREEN);
                    canvas.drawText("✓", screenW - PADDING - 30f,
                            rowY + ROW_HEIGHT / 2f + 13f, paintText);
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Delivery notification popup
    // ─────────────────────────────────────────────

    private void drawNotification(Canvas canvas) {
        if (notifText == null) return;

        float cx = screenW / 2f;
        float cy = screenH * 0.30f; // upper-center of screen

        // Pill background
        paintText.setTextSize(30f);
        float textW = paintText.measureText(notifText);
        float pad   = 30f;
        RectF pill  = new RectF(cx - textW / 2f - pad, cy - 40f,
                cx + textW / 2f + pad, cy + 20f);
        paintBg.setColor(Color.argb(200, 30, 80, 30));
        canvas.drawRoundRect(pill, 20f, 20f, paintBg);

        // Text — game font, gold
        paintText.setColor(COLOR_GOLD);
        canvas.drawText(notifText, cx - textW / 2f, cy, paintText);
    }

    // ─────────────────────────────────────────────
    //  Touch
    // ─────────────────────────────────────────────

    public boolean isPauseButtonTouched(float tx, float ty) {
        return pauseRect.contains(tx, ty);
    }
}