package com.example.undercooked.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.undercooked.R;
import com.example.undercooked.audio.SoundManager;
import com.example.undercooked.data.SettingsManager;

/**
 * Pause dialog — resume and settings buttons.
 * Settings button opens SettingsDialog.
 */
public class PauseDialog extends Dialog {

    private final Runnable        onResume;
    private final SettingsManager settingsManager;
    private final SoundManager    soundManager;

    public PauseDialog(Context context,
                       Runnable onResume,
                       SettingsManager settingsManager,
                       SoundManager soundManager) {
        super(context);
        this.onResume        = onResume;
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

        // ── Root layout ───────────────────────────────────────
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(80, 80, 80, 80);
        root.setBackgroundResource(R.drawable.window_box);

        // ── "PAUSED" title ─────────────────────────────────────
        TextView title = new TextView(getContext());
        title.setText("PAUSED");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        // ── Resume button ──────────────────────────────────────
        ImageButton resumeBtn = makeImageButton(R.drawable.play, R.drawable.play_clicked);
        resumeBtn.setOnClickListener(v -> {
            dismiss();
            if (onResume != null) onResume.run();
        });
        root.addView(resumeBtn);

        root.addView(makeGap(14));

        // ── Settings button ────────────────────────────────────
        ImageButton settingsBtn = makeImageButton(R.drawable.settings, R.drawable.settings_clicked);
        settingsBtn.setOnClickListener(v -> {
            // Open settings dialog on top of this one
            new SettingsDialog(getContext(), settingsManager, soundManager).show();
        });
        root.addView(settingsBtn);

        setContentView(root);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private ImageButton makeImageButton(int normalRes, int pressedRes) {
        ImageButton btn = new ImageButton(getContext());
        btn.setImageResource(normalRes);
        btn.setBackgroundColor(Color.TRANSPARENT);

        int w = dpToPx(130);
        int h = dpToPx(38);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, h);
        params.gravity = Gravity.CENTER;
        btn.setLayoutParams(params);
        btn.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    btn.setImageResource(pressedRes);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    btn.setImageResource(normalRes);
                    break;
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
}