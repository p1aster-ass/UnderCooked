package com.example.undercooked.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.undercooked.R;
import com.example.undercooked.audio.SoundManager;
import com.example.undercooked.data.SettingsManager;

/**
 * Settings dialog — opened from the pause menu.
 * Lets the player toggle music and sound effects on/off.
 * Uses the game font (m6x11) for all text.
 */
public class SettingsDialog extends Dialog {

    private final SettingsManager settingsManager;
    private final SoundManager    soundManager;
    private       Typeface        gameFont;

    public SettingsDialog(Context context,
                          SettingsManager settingsManager,
                          SoundManager soundManager) {
        super(context);
        this.settingsManager = settingsManager;
        this.soundManager    = soundManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Load game font
        try {
            gameFont = ResourcesCompat.getFont(getContext(), R.font.m6x11);
        } catch (Exception e) {
            gameFont = Typeface.DEFAULT;
        }

        // ── Root layout ───────────────────────────────────────
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(80, 80, 80, 80);
        root.setBackgroundResource(R.drawable.window_box);

        // ── Title ─────────────────────────────────────────────
        root.addView(makeText("SETTINGS", 22f, true));
        root.addView(makeGap(28));

        // ── Music toggle ──────────────────────────────────────
        root.addView(makeToggleRow(
                "Music",
                settingsManager.isMusicEnabled(),
                enabled -> {
                    settingsManager.setMusicEnabled(enabled);
                    if (enabled) soundManager.playMusic();
                    else         soundManager.stopMusic();
                }
        ));

        root.addView(makeGap(16));

        // ── Sound effects toggle ──────────────────────────────
        root.addView(makeToggleRow(
                "Sound FX",
                settingsManager.isSfxEnabled(),
                enabled -> {
                    settingsManager.setSfxEnabled(enabled);
                    soundManager.setSfxEnabled(enabled);
                }
        ));

        root.addView(makeGap(32));

        // ── Back button ───────────────────────────────────────
        ImageButton backBtn = makeImageButton(R.drawable.exit, R.drawable.exit_clicked);
        backBtn.setOnClickListener(v -> dismiss());
        root.addView(backBtn);

        setContentView(root);
        setCanceledOnTouchOutside(true);
        setCancelable(true);
    }

    // ─────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────

    private TextView makeText(String text, float sizeSp, boolean isTitle) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(sizeSp);
        tv.setTypeface(gameFont);
        tv.setGravity(Gravity.CENTER);
        if (isTitle) tv.setPadding(0, 0, 0, 0);
        return tv;
    }

    private LinearLayout makeToggleRow(String label, boolean initialValue, OnToggle onToggle) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, 0);

        // Label with game font
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16f);
        tv.setTypeface(gameFont);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        row.addView(tv);

        // Checkbox
        final boolean[] isOn = {initialValue};
        ImageView checkbox = new ImageView(getContext());
        checkbox.setImageResource(isOn[0] ? R.drawable.check_box_checked : R.drawable.check_box);
        int size = dpToPx(36);
        checkbox.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        checkbox.setOnClickListener(v -> {
            isOn[0] = !isOn[0];
            checkbox.setImageResource(isOn[0] ? R.drawable.check_box_checked : R.drawable.check_box);
            onToggle.onToggle(isOn[0]);
        });
        row.addView(checkbox);
        return row;
    }

    private ImageButton makeImageButton(int normalRes, int pressedRes) {
        ImageButton btn = new ImageButton(getContext());
        btn.setImageResource(normalRes);
        btn.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(130), dpToPx(38));
        params.gravity = Gravity.CENTER;
        btn.setLayoutParams(params);
        btn.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:   btn.setImageResource(pressedRes); break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: btn.setImageResource(normalRes);  break;
            }
            return false;
        });
        return btn;
    }

    private View makeGap(int dp) {
        View gap = new View(getContext());
        gap.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dp)));
        return gap;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    interface OnToggle { void onToggle(boolean enabled); }
}