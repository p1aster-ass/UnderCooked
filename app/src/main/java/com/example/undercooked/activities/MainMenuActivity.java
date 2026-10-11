package com.example.undercooked.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.undercooked.MainActivity;
import com.example.undercooked.R;

/**
 * MainMenuActivity — the opening screen of the game.
 * Displays a single full-screen image (openscreen.png).
 * Tapping anywhere opens the game.
 */
public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title bar and make full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(new MenuView(this));
    }

    // ─────────────────────────────────────────────
    //  MenuView — draws the single opening image
    // ─────────────────────────────────────────────

    private class MenuView extends SurfaceView implements SurfaceHolder.Callback {

        private Bitmap openScreen; // the single opening screen image

        public MenuView(android.content.Context context) {
            super(context);
            getHolder().addCallback(this);

            // load the opening screen image
            openScreen = BitmapFactory.decodeResource(getResources(), R.drawable.openscreen);

            setClickable(true); // allow touch events
        }

        // tap anywhere to start the game
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
                startActivity(intent); // open the game
                finish();              // close the menu so back button does not return here
            }
            return true;
        }

        // draw the image stretched to fill the entire screen
        private void draw(SurfaceHolder holder) {
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) return;
            try {
                if (openScreen != null) {
                    // scale the image to fill the full screen exactly
                    canvas.drawBitmap(
                            Bitmap.createScaledBitmap(
                                    openScreen,
                                    canvas.getWidth(),
                                    canvas.getHeight(),
                                    true),
                            0, 0, null);
                }
            } finally {
                holder.unlockCanvasAndPost(canvas);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder)                          { draw(holder); }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int f, int w, int h)     { draw(holder); }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder)                        {}
    }
}