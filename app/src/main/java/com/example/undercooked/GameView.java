package com.example.undercooked; // this file belongs to the main package

import android.app.Activity;           // needed to show dialogs and call runOnUiThread
import android.content.Context;        // gives access to app resources
import android.graphics.Bitmap;        // Bitmap is an image
import android.graphics.BitmapFactory; // loads images from the drawable folder
import android.graphics.Canvas;        // Canvas is the surface we draw onto every frame
import android.graphics.Color;         // built-in colour constants
import android.graphics.Paint;         // Paint defines colour and style for drawing text and shapes
import android.graphics.RectF;         // RectF is a rectangle with float coordinates — used for buttons
import android.graphics.Typeface;      // Typeface is a font
import android.util.Log;               // Log prints messages for debugging
import android.view.MotionEvent;       // MotionEvent describes a touch on the screen
import android.view.SurfaceHolder;     // SurfaceHolder gives us a safe Canvas to draw on from a thread
import android.view.SurfaceView;       // SurfaceView is a view that supports drawing from a background thread

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat; // loads fonts from the res/font folder

import com.example.undercooked.audio.SoundManager;
import com.example.undercooked.data.OrderManager;
import com.example.undercooked.data.RecipeManager;
import com.example.undercooked.data.SettingsManager;
import com.example.undercooked.entities.characters.Chef1;
import com.example.undercooked.entities.enums.ChefState;
import com.example.undercooked.entities.enums.Direction;
import com.example.undercooked.inputs.Joystick;
import com.example.undercooked.particles.PoofManager;
import com.example.undercooked.physics.CollisionManager;
import com.example.undercooked.stations.PlacematStation;
import com.example.undercooked.stations.StoveStation;
import com.example.undercooked.ui.ActionButton;
import com.example.undercooked.ui.GameHUD;
import com.example.undercooked.ui.PauseDialog;
import com.example.undercooked.ui.icons.LevelIconManager;

import java.util.ArrayList; // ArrayList is a list that can grow dynamically
import java.util.HashMap;   // HashMap stores key-value pairs like a dictionary
import java.util.List;      // the base interface for all list types
import java.util.Map;       // the base interface for all map types

/**
 * GameView — the main game screen.
 *
 * Extends SurfaceView so we can draw to it from a background thread.
 * Implements Runnable so the game loop runs in its own thread.
 * Implements SurfaceHolder.Callback so we know when the surface is ready.
 *
 * The game loop runs at full speed: update logic → draw frame → repeat.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // ─────────────────────────────────────────────
    //  Constants — tunable game values
    // ─────────────────────────────────────────────
    private static final float PLAYER_SPEED    = 12.0f; // pixels per frame the chef moves
    private static final float ZOOM            = 0.8f;  // scale factor for the entire game world
    private static final int   ANIM_DELAY_MS   = 100;   // milliseconds between station icon frames
    private static final float INTERACT_RADIUS = 100f;  // how close the chef must be to use a station
    private static final float CHOP_DURATION   = 2.0f;  // seconds to chop an ingredient
    private static final float DONE_LINGER     = 1.0f;  // seconds the "done" animation stays visible

    // ─────────────────────────────────────────────
    //  Core systems
    // ─────────────────────────────────────────────
    private Thread           gameThread;          // the background thread that runs the game loop
    private boolean          isRunning = false;   // controls whether the game loop keeps running
    private volatile boolean isPaused  = false;   // volatile = changes are seen immediately by the game thread
    private final SurfaceHolder holder;           // gives us access to the canvas

    private final Chef1            chef1;            // the chef the player controls
    private final Joystick         joystick;          // the on-screen joystick for movement
    private final SoundManager     soundManager;      // manages all game sounds
    private final CollisionManager collisionManager;  // prevents the chef from walking through walls
    private final LevelIconManager levelIconManager;  // draws the floating icons above stations
    private final OrderManager     orderManager;      // manages the dish list, money and timer
    private final SettingsManager  settingsManager;   // loads and saves settings (SharedPreferences)

    private GameHUD      gameHUD;    // the heads-up display (money, timer, order list)
    private PoofManager  poofManager; // plays the poof particle effect
    private ActionButton useButton;   // the USE button on screen
    private ActionButton dropButton;  // the DROP button on screen

    private Typeface gameFont; // the pixel font used for all text in the game

    // ─────────────────────────────────────────────
    //  Map layers — drawn in order to create depth
    // ─────────────────────────────────────────────
    private final Bitmap layerBlackWall; // the collision/shadow layer
    private final Bitmap layerWall;      // the wall tiles
    private final Bitmap layerFurniture; // furniture that sits behind the chef
    private final Bitmap layerBehind;    // objects that appear behind the chef
    private final Bitmap layerOnTop;     // objects that appear in front of the chef
    private final Bitmap layerOnTop2;    // a second on-top layer for roofs and ceilings

    // ─────────────────────────────────────────────
    //  Image caches — loaded once, reused every frame
    // ─────────────────────────────────────────────
    private final Map<String, Bitmap[]> iconAnimations = new HashMap<>(); // animated station icons
    private final Map<String, Bitmap>   foodSprites    = new HashMap<>(); // food item images
    private final Map<String, Bitmap>   staticSprites  = new HashMap<>(); // non-animated images (stove on/off)

    private Bitmap checkmarkSprite;        // green checkmark shown on completed orders
    private Bitmap pauseButtonSprite;      // the pause button image
    private Bitmap playAgainSprite;        // the "play again" button on the win/lose screen
    private Bitmap playAgainClickedSprite; // the "play again" button when pressed
    private Bitmap exitSprite;             // the "exit" button on the win/lose screen
    private Bitmap exitClickedSprite;      // the "exit" button when pressed

    // rectangles for detecting taps on the win/lose overlay buttons
    private final RectF playAgainRect = new RectF(); // tap area for "play again"
    private final RectF exitRect      = new RectF(); // tap area for "exit"
    private boolean playAgainPressed  = false; // true while the play-again button is held down
    private boolean exitPressed       = false; // true while the exit button is held down

    // ─────────────────────────────────────────────
    //  Stations
    // ─────────────────────────────────────────────
    private final List<PlacematStation> placemats = new ArrayList<>(); // the mixing stations
    private final List<StoveStation>    stoves    = new ArrayList<>(); // the cooking stoves

    // ─────────────────────────────────────────────
    //  Game state
    // ─────────────────────────────────────────────
    private float mapX = 0f; // horizontal offset of the map — changes as the chef moves
    private float mapY = 0f; // vertical offset of the map — changes as the chef moves
    private int   animFrame    = 0;    // current frame index for station icon animations (0 to 7)
    private long  lastAnimTime = 0;    // when the last animation frame was shown (milliseconds)

    private boolean isUseHeld  = false; // true while the player holds the USE button
    private float   chopTimer  = 0f;    // how many seconds the chef has been chopping
    private float   doneTimer  = 0f;    // how long the "done" animation has been showing
    private boolean isChopping = false; // true while the chef is actively chopping
    private boolean isChopDone = false; // true while the done animation is playing

    // ─────────────────────────────────────────────
    //  Constructor — sets everything up before the game starts
    // ─────────────────────────────────────────────
    public GameView(Context context) {
        super(context); // call the SurfaceView constructor
        holder = getHolder();          // get the surface holder we will draw onto
        holder.addCallback(this);      // tell Android to notify us when the surface is ready

        // load all six map layers from the drawable folder
        layerBlackWall = decode(R.drawable.blackwall_1);
        layerWall      = decode(R.drawable.wall_1);
        layerFurniture = decode(R.drawable.furnitures_1);
        layerBehind    = decode(R.drawable.behind_player_1);
        layerOnTop     = decode(R.drawable.on_top_1);
        layerOnTop2    = decode(R.drawable.on_top_2);

        // load the animated station icons — each has 8 frames
        // true = frames are named "icon_01", "icon_02" ... (two-digit, one-based)
        // false = frames are named "icon_0", "icon_1" ... (zero-based)
        loadIconAnim("rice",        "rice_icon",        8, false);
        loadIconAnim("knife",       "chop_icon",        8, true);
        loadIconAnim("onion",       "onion_icon",       8, false);
        loadIconAnim("meat",        "meat_icon",        8, false);
        loadIconAnim("pickle",      "pickle_icon",      8, false);
        loadIconAnim("tomato",      "tomato_icon",      8, false);
        loadIconAnim("fish",        "fish_icon",        8, false);
        loadIconAnim("dollar",      "counter_icon",     8, false);
        loadIconAnim("clock",       "clock_icon",       8, true);
        loadIconAnim("clock_blank", "clock_icon_blank", 8, true);
        loadIconAnim("clock_done",  "clock_icon_done",  8, false);

        // load the stove images — these are static (not animated)
        loadStatic("stove_off", R.drawable.stoveoff); // stove when empty
        loadStatic("stove_on",  R.drawable.stoveon);  // stove when cooking or done

        // load food sprites — raw ingredients
        loadFood("rice",   R.drawable.rice);
        loadFood("onion",  R.drawable.onion);
        loadFood("meat",   R.drawable.meat);
        loadFood("pickle", R.drawable.pickle);
        loadFood("tomato", R.drawable.tomato);
        loadFood("fish",   R.drawable.fish);

        // load food sprites — after chopping
        loadFood("chopped_meat",   R.drawable.chopped_meat);
        loadFood("chopped_fish",   R.drawable.chopped_fish);
        loadFood("chopped_onion",  R.drawable.chopped_onion);
        loadFood("chopped_tomato", R.drawable.chopped_tomato);
        loadFood("chopped_pickle", R.drawable.chopped_pickle);

        // load food sprites — after cooking
        loadFood("cooked_meat",         R.drawable.cooked_meat);
        loadFood("cooked_fish",         R.drawable.cooked_fish);
        loadFood("cooked_chopped_meat", R.drawable.cooked_chopped_meat);
        loadFood("cooked_chopped_fish", R.drawable.cooked_chopped_fish);
        loadFood("cooked_meat_onion",   R.drawable.cooked_meat_onion);
        loadFood("cooked_fish_onion",   R.drawable.cooked_fish_onion);
        loadFood("cooked_meat_tomato",  R.drawable.cooked_meat_tomato);
        loadFood("cooked_fish_tomato",  R.drawable.cooked_fish_tomato);

        // load food sprites — finished meals ready to sell
        loadFood("meat_meal",        R.drawable.meat_meal);
        loadFood("fish_meal",        R.drawable.fish_meal);
        loadFood("meat_meal_tomato", R.drawable.meat_meal_tomato);
        loadFood("meat_meal_onion",  R.drawable.meat_meal_onion);
        loadFood("fish_meal_tomato", R.drawable.fish_meal_tomato);
        loadFood("fish_meal_onion",  R.drawable.fish_meal_onion);
        loadFood("tomato_soup",      R.drawable.tomato_soup);
        loadFood("struggle_meal",    R.drawable.struggle_meal);

        // load UI sprites
        checkmarkSprite        = decode(R.drawable.check_box_checked);  // green tick for completed orders
        pauseButtonSprite      = decode(R.drawable.pause_button_00);    // pause button icon
        playAgainSprite        = decode(R.drawable.play);               // play-again button (normal)
        playAgainClickedSprite = decode(R.drawable.play_clicked);       // play-again button (pressed)
        exitSprite             = decode(R.drawable.exit);               // exit button (normal)
        exitClickedSprite      = decode(R.drawable.exit_clicked);       // exit button (pressed)

        // create the four placemat mixing stations and place them on the map
        placemats.add(new PlacematStation(295, 370)); // top-left placemat
        placemats.add(new PlacematStation(965, 370)); // top-right placemat
        placemats.add(new PlacematStation(295, 538)); // bottom-left placemat
        placemats.add(new PlacematStation(965, 538)); // bottom-right placemat

        // create the three stove stations — order must match LevelIconManager
        stoves.add(new StoveStation(548, 246)); // left stove
        stoves.add(new StoveStation(628, 246)); // middle stove
        stoves.add(new StoveStation(714, 246)); // right stove

        // create all the manager objects
        joystick         = new Joystick(250, 750, 130, 70); // joystick at bottom-left of screen
        chef1            = new Chef1(context, 0, 0);          // chef starts at position (0,0) — moved in surfaceCreated
        soundManager     = new SoundManager(context);         // handles all sounds
        collisionManager = new CollisionManager();            // prevents walking through walls
        levelIconManager = new LevelIconManager();            // draws floating icons above stations
        orderManager     = new OrderManager();                // manages dish list and timer
        settingsManager  = new SettingsManager(context);      // loads saved settings

        // apply the saved settings immediately
        soundManager.setSfxEnabled(settingsManager.isSfxEnabled());

        // load the data for level 1
        collisionManager.loadLevelCollisions(1);
        levelIconManager.loadLevelIcons(1);
        orderManager.loadLevel(1);
    }

    // ─────────────────────────────────────────────
    //  Asset loading helpers
    // ─────────────────────────────────────────────

    // loads a bitmap image from the drawable folder using its resource ID
    private Bitmap decode(int resId) {
        return BitmapFactory.decodeResource(getResources(), resId);
    }

    // loads a food sprite and stores it in the foodSprites map with the given key
    private void loadFood(String key, int resId) {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);
        if (bmp != null) foodSprites.put(key, bmp);    // store if loading succeeded
        else             Log.w("FOOD", "Missing: " + key); // warn if the file is missing
    }

    // loads a static (non-animated) sprite into the staticSprites map
    private void loadStatic(String key, int resId) {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);
        if (bmp != null) staticSprites.put(key, bmp);
        else             Log.w("STATIC", "Missing: " + key);
    }

    // loads an animated icon — a sequence of numbered images — into the iconAnimations map
    private void loadIconAnim(String key, String baseName, int count, boolean twoDigitOneBased) {
        Bitmap[] frames = new Bitmap[count]; // array to hold all the frames

        for (int i = 0; i < count; i++) { // loop through every frame
            // build the file name — either "name_01" style or "name_0" style
            String name = twoDigitOneBased
                    ? String.format("%s_%02d", baseName, i + 1) // e.g. "chop_icon_01"
                    : baseName + "_" + i;                        // e.g. "rice_icon_0"

            // ask Android to find the resource ID for this file name
            int id = getResources().getIdentifier(name, "drawable", getContext().getPackageName());
            if (id != 0) frames[i] = BitmapFactory.decodeResource(getResources(), id); // load the frame
            else         Log.w("ICON_ANIM", "Missing: " + name); // warn if missing
        }

        iconAnimations.put(key, frames); // store all frames together under the key
    }

    // ─────────────────────────────────────────────
    //  Game loop — runs on a background thread
    // ─────────────────────────────────────────────

    // this method runs on the game thread — it is the core loop of the game
    @Override
    public void run() {
        long lastNano = System.nanoTime(); // record the time at the start of the loop

        while (isRunning) { // keep looping while the game is active
            long  now = System.nanoTime();                    // current time in nanoseconds
            float dt  = (now - lastNano) / 1_000_000_000f;   // time since last frame in seconds
            lastNano  = now;                                  // update the last-frame time

            if (!isPaused) update(dt); // only update game logic when not paused
            drawFrame();               // always draw — even when paused (so the pause dialog shows)
        }
    }

    // updates all game logic for one frame — called every loop iteration when not paused
    private void update(float dt) {
        if (orderManager.isLevelOver()) return; // stop updating once the level has ended

        orderManager.update(dt); // count down the level timer

        joystick.update(); // recalculate the thumb position
        float   actX   = (float) joystick.getActuatorX(); // horizontal joystick value (-1 to 1)
        float   actY   = (float) joystick.getActuatorY(); // vertical joystick value (-1 to 1)
        boolean moving = (actX != 0 || actY != 0);        // true if the joystick is being pushed

        if (moving && !isChopping) { // only move if the joystick is pushed and not chopping
            // ask the collision manager how far we can actually move
            float[] vel = collisionManager.getAdjustedVelocity(
                    mapX, mapY,
                    -actX * PLAYER_SPEED, // invert X because moving the chef right = moving the map left
                    -actY * PLAYER_SPEED, // invert Y because moving the chef down = moving the map up
                    getWidth(), getHeight(),
                    layerBlackWall.getWidth(), layerBlackWall.getHeight());

            mapX += vel[0]; // apply the allowed horizontal movement
            mapY += vel[1]; // apply the allowed vertical movement

            if (chef1.getState() != ChefState.RUNNING) { // only start footstep sound if we just started moving
                soundManager.startFootsteps();
                chef1.setState(ChefState.RUNNING); // switch to the running animation
            }

            // scale footstep volume by how hard the joystick is pushed
            soundManager.setFootstepVolume(
                    Math.min(1f, (float) Math.sqrt(actX * actX + actY * actY)));

            updateDirection(actX, actY); // update the chef's facing direction

        } else if (!isChopping) { // joystick is not pushed and not chopping
            if (chef1.getState() != ChefState.IDLE) { // only change state if we were not already idle
                soundManager.stopFootsteps();
                chef1.setState(ChefState.IDLE); // switch to the idle animation
            }
        }

        updateChopping(dt); // handle the chopping timer and animation

        // update every stove — cooking happens automatically without the chef being nearby
        for (int i = 0; i < stoves.size(); i++) {
            StoveStation stove = stoves.get(i);
            boolean wasCooking = stove.isCooking(); // check state before update
            stove.update(dt);                       // advance the cooking timer
            if (wasCooking && stove.isDone()) {     // if cooking just finished this frame
                soundManager.playBell();            // play the bell sound
                triggerPoof();                      // play the poof particle effect
            }
        }

        chef1.update(dt); // advance the chef's animation frame

        // advance the station icon animation every ANIM_DELAY_MS milliseconds
        if (System.currentTimeMillis() - lastAnimTime > ANIM_DELAY_MS) {
            animFrame    = (animFrame + 1) % 8; // cycle 0→1→2→...→7→0
            lastAnimTime = System.currentTimeMillis(); // reset the timer
        }
    }

    // ─────────────────────────────────────────────
    //  Chopping logic
    // ─────────────────────────────────────────────

    // handles all chopping state transitions and timing each frame
    private void updateChopping(float dt) {

        // phase 2: the done animation is playing — wait for it to finish, then reset
        if (isChopDone) {
            doneTimer += dt;
            if (doneTimer >= DONE_LINGER) { // done animation has shown long enough
                isChopDone = false;         // stop the done state
                doneTimer  = 0f;            // reset the timer
                levelIconManager.resetKnife(); // put the knife icon back to normal
            }
            return; // do not run the rest of the chopping logic
        }

        if (!isChopping) return; // nothing to do if we are not chopping

        // cancel chopping if the player released USE or walked away from the knife
        String nearby = levelIconManager.getNearbyStation(
                getChefMapX(), getChefMapY(), INTERACT_RADIUS);
        if (!isUseHeld || !"knife".equals(nearby)) {
            cancelChopping();
            return;
        }

        chopTimer += dt; // advance the chopping timer
        // update the clock icon fill (0.0 = empty, 1.0 = full)
        levelIconManager.setChoppingProgress(Math.min(chopTimer / CHOP_DURATION, 1f));

        if (chopTimer >= CHOP_DURATION) finishChopping(); // chopping complete
    }

    // called when the player presses USE near the knife with a choppable item
    private void startChopping() {
        isChopping = true; // enter the chopping state
        chopTimer  = 0f;   // reset the timer
        chef1.setState(ChefState.CHOPPING);  // show the chopping animation
        soundManager.startChopping();        // play the chopping sound
        levelIconManager.setChoppingProgress(0f); // start the clock at zero
    }

    // called when the player releases USE or walks away before finishing
    private void cancelChopping() {
        isChopping = false; // leave the chopping state
        chopTimer  = 0f;    // reset the timer
        chef1.setState(ChefState.IDLE); // go back to standing
        soundManager.stopChopping();    // stop the chopping sound
        levelIconManager.resetKnife();  // put the knife icon back to normal
    }

    // called when the chopping timer reaches the full duration
    private void finishChopping() {
        String item   = chef1.getHeldItem();   // what the chef was chopping
        String result = RecipeManager.chop(item); // look up the chopped result
        if (result != null) chef1.setHeldItem(result); // give the chef the chopped item

        // transition out of the chopping state and into the done state
        isChopping = false;
        isChopDone = true;
        chopTimer  = 0f;
        doneTimer  = 0f;

        chef1.setState(ChefState.IDLE);  // go back to standing
        soundManager.stopChopping();     // stop the chopping sound
        soundManager.playBell();         // play the bell to signal completion
        levelIconManager.setChoppingDone(); // show the done animation on the knife icon
        triggerPoof();                   // play the poof particle effect
    }

    // updates the chef's facing direction based on the joystick position
    private void updateDirection(float actX, float actY) {
        // face the direction that has more input — X wins ties
        if (Math.abs(actX) >= Math.abs(actY))
            chef1.setDirection(actX > 0 ? Direction.EAST : Direction.WEST); // face right or left
        else
            chef1.setDirection(actY > 0 ? Direction.SOUTH : Direction.NORTH); // face down or up
    }

    // ─────────────────────────────────────────────
    //  Rendering — draws one complete frame
    // ─────────────────────────────────────────────

    // draws the entire game frame onto the canvas — called every loop iteration
    private void drawFrame() {
        if (!holder.getSurface().isValid()) return; // do not draw if the surface is not ready
        Canvas canvas = holder.lockCanvas();        // lock the canvas so we can draw on it
        if (canvas == null) return;                 // if locking failed, skip this frame

        try {
            canvas.drawColor(Color.parseColor("#3a3a50")); // fill with a dark background colour

            canvas.save();                                          // save the current transform
            canvas.scale(ZOOM, ZOOM, getWidth() / 2f, getHeight() / 2f); // zoom out from the centre

            // draw map layers from back to front — order creates the illusion of depth
            canvas.drawBitmap(layerBlackWall, mapX, mapY, null); // shadows and collision hint
            canvas.drawBitmap(layerWall,      mapX, mapY, null); // walls
            canvas.drawBitmap(layerFurniture, mapX, mapY, null); // furniture behind chef
            canvas.drawBitmap(layerOnTop,     mapX, mapY, null); // objects in front of furniture

            // stoves are drawn BEFORE the chef so the chef appears on top of them
            levelIconManager.drawStovesOnly(canvas, staticSprites, stoves, animFrame, mapX, mapY);

            chef1.render(canvas);  // draw the chef
            drawHeldItem(canvas);  // draw the item the chef is holding above their head

            // roof layers drawn AFTER the chef so they appear on top
            canvas.drawBitmap(layerBehind, mapX, mapY, null);
            canvas.drawBitmap(layerOnTop2, mapX, mapY, null);

            // draw the floating station icons (knife, rice, fish etc.) after the chef
            levelIconManager.drawIconsExceptStoves(canvas, iconAnimations, animFrame, mapX, mapY);

            // draw the clock icons above stoves — always visible even when the stove is off
            levelIconManager.drawStoveClocks(canvas, iconAnimations, staticSprites,
                    stoves, animFrame, mapX, mapY);

            // draw the item sitting on each placemat
            for (int i = 0; i < placemats.size(); i++) {
                placemats.get(i).draw(canvas, foodSprites, mapX, mapY);
            }

            if (poofManager != null) poofManager.drawAndUpdate(canvas); // draw poof particle

            canvas.restore(); // restore the transform — removes the zoom so UI is drawn at full size

            // draw the HUD on top of the zoomed game world
            if (gameHUD != null) {
                gameHUD.draw(canvas, orderManager, foodSprites, checkmarkSprite, pauseButtonSprite);
            }

            if (useButton  != null) useButton.draw(canvas);  // draw USE button
            if (dropButton != null) dropButton.draw(canvas); // draw DROP button
            joystick.draw(canvas);                           // draw the joystick

            // if the level is over, draw the win/lose overlay on top of everything
            if (orderManager.isLevelOver()) drawLevelOverlay(canvas);

        } finally {
            holder.unlockCanvasAndPost(canvas); // always unlock — even if an error occurred
        }
    }

    // draws the item the chef is holding above their head
    private void drawHeldItem(Canvas canvas) {
        String item = chef1.getHeldItem();
        if (item == null || item.equals("none")) return; // nothing to draw

        Bitmap bmp = foodSprites.get(item); // try to find the food sprite
        if (bmp == null) { // if not in foodSprites, try icon animations as a fallback
            Bitmap[] frames = iconAnimations.get(item);
            if (frames != null && frames.length > 0) bmp = frames[0]; // use the first frame
        }
        if (bmp == null) return; // image not found anywhere — do nothing

        // draw centred above the chef (the chef is always at the centre of the screen)
        canvas.drawBitmap(bmp,
                getWidth()  / 2f - bmp.getWidth()  / 2f, // centred horizontally
                getHeight() / 2f - 155f,                  // 155 pixels above the screen centre
                null);
    }

    // draws the win or lose overlay with the score and two buttons
    private void drawLevelOverlay(Canvas canvas) {
        Paint p = new Paint();
        p.setAntiAlias(true);

        // semi-transparent dark background so the map is still visible behind it
        p.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), p);

        // draw the YOU WIN or YOU LOSE image in the centre
        Bitmap resultImg = decode(orderManager.didPlayerWin()
                ? R.drawable.youwin_text   // player won all orders
                : R.drawable.youlose_text); // timer ran out
        if (resultImg != null) {
            canvas.drawBitmap(resultImg,
                    getWidth()  / 2f - resultImg.getWidth()  / 2f, // centred horizontally
                    getHeight() / 2f - resultImg.getHeight() / 2f - 120f, // above centre
                    null);
        }

        // draw the final score using the game font
        p.setColor(Color.parseColor("#FFD700")); // gold colour
        p.setTextSize(42f);
        p.setTypeface(gameFont != null ? gameFont : Typeface.DEFAULT); // use game font if loaded
        p.setTextAlign(Paint.Align.CENTER); // centre the text around the x coordinate
        canvas.drawText("Score: $" + orderManager.getMoney(),
                getWidth() / 2f, getHeight() / 2f - 20f, p);

        // set the size and position of the two buttons
        int   btnW = 220;
        int   btnH = 70;
        float btnY = getHeight() / 2f + 60f; // below the score text
        float gap  = 40f;                    // gap between the two buttons

        // play again button — left of centre
        float playX = getWidth() / 2f - btnW - gap / 2f;
        playAgainRect.set(playX, btnY, playX + btnW, btnY + btnH); // update the touch area
        Bitmap playBmp = playAgainPressed ? playAgainClickedSprite : playAgainSprite; // pressed or normal
        if (playBmp != null) {
            canvas.drawBitmap(Bitmap.createScaledBitmap(playBmp, btnW, btnH, true),
                    playX, btnY, null);
        }

        // exit button — right of centre
        float exitX = getWidth() / 2f + gap / 2f;
        exitRect.set(exitX, btnY, exitX + btnW, btnY + btnH); // update the touch area
        Bitmap exitBmp = exitPressed ? exitClickedSprite : exitSprite; // pressed or normal
        if (exitBmp != null) {
            canvas.drawBitmap(Bitmap.createScaledBitmap(exitBmp, btnW, btnH, true),
                    exitX, btnY, null);
        }
    }

    // ─────────────────────────────────────────────
    //  Touch input
    // ─────────────────────────────────────────────

    // receives every touch event from Android and routes it to the right handler
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int   action = event.getActionMasked(); // what type of touch (down, move, up)
        float tx     = event.getX();            // x coordinate of the touch
        float ty     = event.getY();            // y coordinate of the touch

        // when the level is over, only the overlay buttons should respond
        if (orderManager.isLevelOver()) {
            handleOverlayTouch(tx, ty, action);
            return true; // consume the event
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            handlePress(tx, ty); // a new finger touched the screen
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (joystick.getIsPressed()) joystick.setActuator(tx, ty); // drag the joystick
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            // a finger was lifted — release all buttons
            if (useButton  != null) useButton.setPressed(false);
            if (dropButton != null) dropButton.setPressed(false);
            if (isUseHeld) { // if the USE button was held, release it
                isUseHeld = false;
                if (isChopping) cancelChopping(); // cancel chopping if USE was released
            }
            joystick.setIsPressed(false); // release the joystick
            joystick.resetActuator();     // snap the joystick back to its home position
        }

        return true; // always return true to indicate we handled the event
    }

    // handles touches on the win/lose overlay buttons
    private void handleOverlayTouch(float tx, float ty, int action) {
        if (action == MotionEvent.ACTION_DOWN) { // finger pressed down
            playAgainPressed = playAgainRect.contains(tx, ty); // is the play-again button being pressed?
            exitPressed      = exitRect.contains(tx, ty);      // is the exit button being pressed?

        } else if (action == MotionEvent.ACTION_UP) { // finger lifted
            if (playAgainPressed && playAgainRect.contains(tx, ty)) {
                // restart the activity — this resets the entire game
                ((Activity) getContext()).runOnUiThread(
                        () -> ((Activity) getContext()).recreate());
            }
            if (exitPressed && exitRect.contains(tx, ty)) {
                // finish the activity — goes back to the previous screen (main menu)
                ((Activity) getContext()).runOnUiThread(
                        () -> ((Activity) getContext()).finish());
            }
            playAgainPressed = false; // release the button visual
            exitPressed      = false;
        }
    }

    // handles a new finger press on the game screen
    private void handlePress(float tx, float ty) {
        Log.d("TOUCH", "Press at (" + (int)tx + ", " + (int)ty + ")");

        // check if the pause button was tapped
        if (gameHUD != null && gameHUD.isPauseButtonTouched(tx, ty)) {
            isPaused = true; // pause the game loop immediately
            // show the pause dialog on the UI thread (dialogs must run on the UI thread)
            ((Activity) getContext()).runOnUiThread(() ->
                    new PauseDialog(
                            getContext(),
                            () -> isPaused = false, // this runs when the player presses Resume
                            settingsManager,
                            soundManager
                    ).show()
            );
            return; // do not process any other buttons
        }

        if (isPaused) return; // ignore all input while paused

        // check if the USE button was tapped
        if (useButton != null && useButton.isTouched(tx, ty)) {
            useButton.setPressed(true); // show the pressed state
            isUseHeld = true;           // remember that USE is being held
            handleUse();                // perform the USE action
            return;
        }

        // check if the DROP button was tapped
        if (dropButton != null && dropButton.isTouched(tx, ty)) {
            dropButton.setPressed(true);
            if (!chef1.getHeldItem().equals("none")) { // only drop if holding something
                chef1.setHeldItem("none"); // empty the chef's hands
                triggerPoof();             // show the poof effect
            }
            return;
        }

        // check if the joystick was tapped
        if (joystick.isTouched(tx, ty)) {
            joystick.setIsPressed(true); // the joystick is now active

        } else {
            // tapped somewhere on the map — log the map coordinates for debugging station positions
            float tapMapX = (tx / ZOOM) - mapX + (getWidth()  / 2f) * (1 - 1 / ZOOM);
            float tapMapY = (ty / ZOOM) - mapY + (getHeight() / 2f) * (1 - 1 / ZOOM);
            Log.d("MAP_TAP", "X=" + (int) tapMapX + "  Y=" + (int) tapMapY);
        }
    }

    // decides what happens when the player presses USE depending on what station is nearby
    private void handleUse() {
        if (isPaused || orderManager.isLevelOver()) return; // do nothing if paused or game over

        float  chefMapX = getChefMapX(); // chef's x position on the map
        float  chefMapY = getChefMapY(); // chef's y position on the map

        // find the closest station within INTERACT_RADIUS
        String nearby = levelIconManager.getNearbyStation(chefMapX, chefMapY, INTERACT_RADIUS);

        Log.d("USE", "nearby=" + nearby + "  held=" + chef1.getHeldItem());
        if (nearby == null) return; // no station close enough — do nothing

        // perform the action for whichever station is nearby
        if (nearby.equals("knife")) {
            // start chopping if holding a choppable item and not already chopping
            if (!isChopping && !isChopDone && RecipeManager.isChoppable(chef1.getHeldItem()))
                startChopping();

        } else if (nearby.equals("stove")) {
            // interact with the closest stove
            StoveStation nearestStove = getNearestStove(chefMapX, chefMapY);
            if (nearestStove != null) {
                String result = nearestStove.interact(chef1.getHeldItem()); // put item on or pick up from stove
                chef1.setHeldItem(result); // update what the chef holds
                triggerPoof();
            }

        } else if (nearby.equals("placemat")) {
            // interact with the closest placemat — place, pick up or combine ingredients
            PlacematStation nearestPlacemat = getNearestPlacemat(chefMapX, chefMapY);
            if (nearestPlacemat != null) {
                String result = nearestPlacemat.interact(chef1.getHeldItem());
                chef1.setHeldItem(result);
                triggerPoof();
            }

        } else if (nearby.equals("dollar")) {
            // deliver the held dish to the dollar station to earn money
            String held = chef1.getHeldItem();
            if (!held.equals("none")) { // only deliver if holding something
                // find the matching order before marking it complete (needed for the notification)
                OrderManager.Order matchedOrder = orderManager.findMatchingOrder(held);
                boolean matched = orderManager.deliver(held); // mark as complete and add money
                if (matched) {
                    soundManager.playBell(); // play a success sound
                    // show a "+$20  Meat Meal Tomato!" notification using the Handler in GameHUD
                    if (gameHUD != null && matchedOrder != null) {
                        gameHUD.showDeliveryNotif(matchedOrder.displayName, matchedOrder.price);
                    }
                }
                chef1.setHeldItem("none"); // empty the chef's hands after delivering
                triggerPoof();
                Log.d("ORDER", "Delivered: " + held + " matched=" + matched);
            }

        } else {
            // any other station is an ingredient source — just pick up the ingredient
            chef1.setHeldItem(nearby); // the station name IS the ingredient name
            triggerPoof();
        }
    }

    // ─────────────────────────────────────────────
    //  Helper methods
    // ─────────────────────────────────────────────

    // returns the chef's current x position on the map (not screen)
    private float getChefMapX() { return getWidth()  / 2f - mapX; }

    // returns the chef's current y position on the map (not screen)
    private float getChefMapY() { return getHeight() / 2f - mapY; }

    // finds the nearest stove within INTERACT_RADIUS — returns null if none is close enough
    private StoveStation getNearestStove(float chefMapX, float chefMapY) {
        StoveStation nearest = null;
        float minDist = INTERACT_RADIUS; // start with the maximum allowed distance
        for (int i = 0; i < stoves.size(); i++) {
            StoveStation s = stoves.get(i);
            // calculate distance using Pythagoras
            float dist = (float) Math.sqrt(
                    Math.pow(chefMapX - s.mapX, 2) + Math.pow(chefMapY - s.mapY, 2));
            if (dist < minDist) { // this stove is closer than the previous closest
                minDist = dist;
                nearest = s;
            }
        }
        return nearest;
    }

    // finds the nearest placemat within INTERACT_RADIUS — returns null if none is close enough
    private PlacematStation getNearestPlacemat(float chefMapX, float chefMapY) {
        PlacematStation nearest = null;
        float minDist = INTERACT_RADIUS;
        for (int i = 0; i < placemats.size(); i++) {
            PlacematStation p = placemats.get(i);
            float dist = (float) Math.sqrt(
                    Math.pow(chefMapX - p.mapX, 2) + Math.pow(chefMapY - p.mapY, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // triggers the poof particle effect at a fixed position above the screen centre
    private void triggerPoof() {
        if (poofManager != null)
            poofManager.triggerPoof(getWidth() / 2f, getHeight() / 2f - 70f);
    }

    // ─────────────────────────────────────────────
    //  Surface lifecycle — called by Android automatically
    // ─────────────────────────────────────────────

    // called when the SurfaceView is ready — we create the game objects and start the loop here
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder h) {
        chef1.setPosition(getWidth() / 2f, getHeight() / 2f); // place the chef at the screen centre
        poofManager = new PoofManager(getContext());            // create the particle manager

        // load the pixel font for use throughout the game
        try { gameFont = ResourcesCompat.getFont(getContext(), R.font.m6x11); }
        catch (Exception e) { Log.e("UI", "Font load failed"); gameFont = Typeface.DEFAULT; }

        // create the HUD now that we know the screen dimensions
        gameHUD = new GameHUD(gameFont, getWidth(), getHeight());

        // position the USE and DROP buttons in the bottom-right area
        float btnW  = 300f, btnH = 150f, gap = 60f;
        float btnY  = getHeight() - btnH - gap;   // near the bottom of the screen
        float dropX = getWidth()  - btnW - gap;   // DROP on the right
        float useX  = dropX - btnW - gap;         // USE to the left of DROP

        useButton  = new ActionButton("USE",  useX,  btnY, btnW, btnH, gameFont);
        dropButton = new ActionButton("DROP", dropX, btnY, btnW, btnH, gameFont);

        isRunning = true; // allow the game loop to run

        // only play music if the setting is enabled
        if (settingsManager.isMusicEnabled()) soundManager.playMusic();

        // start the game loop on a new background thread
        gameThread = new Thread(this);
        gameThread.start();
    }

    // called when the surface changes size — not used but required by the interface
    @Override public void surfaceChanged(@NonNull SurfaceHolder h, int f, int w, int hi) {}

    // called when the SurfaceView is destroyed — we stop the game loop here
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder h) {
        isRunning = false;        // tell the game loop to stop
        soundManager.destroy();   // free all audio resources
        try { gameThread.join(); } // wait for the game thread to finish cleanly
        catch (InterruptedException ignored) {}
    }
}