package com.example.undercooked.ui.icons;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import com.example.undercooked.stations.StoveStation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelIconManager {

    public enum StationState { IDLE, IN_PROGRESS, DONE }

    // Each station has TWO positions:
    // drawX/drawY  → where the icon sprite is drawn on the map
    // interactX/Y  → where the chef needs to stand to interact (from MAP_TAP)
    public static class MapIcon {
        public final String type;
        public final float  drawX,     drawY;
        public final float  interactX, interactY;

        // Use when draw == interact
        public MapIcon(String type, float x, float y) {
            this(type, x, y, x, y);
        }

        // Use when draw != interact
        public MapIcon(String type, float drawX, float drawY, float interactX, float interactY) {
            this.type      = type;
            this.drawX     = drawX;
            this.drawY     = drawY;
            this.interactX = interactX;
            this.interactY = interactY;
        }
    }

    private final List<MapIcon>             icons           = new ArrayList<>();
    private final Map<String, StationState> stationStates   = new HashMap<>();
    private final Map<String, Float>        stationProgress = new HashMap<>();

    // ─────────────────────────────────────────────
    //  Level setup
    // ─────────────────────────────────────────────

    public void loadLevelIcons(int level) {
        icons.clear();
        stationStates.clear();
        stationProgress.clear();

        if (level == 1) {
            // format: addIcon(type, drawX, drawY, interactX, interactY)
            // drawX/Y    = where the floating icon is shown on the map
            // interactX/Y = where the chef needs to stand (from MAP_TAP)

            // Rice cookers — confirmed MAP_TAP
            addIcon("rice",     795,  105,  753,  235);
            addIcon("rice",     880,  105,  838,  232);

            // Onion — confirmed MAP_TAP
            addIcon("onion",    1220, 315,  1215, 381);

            // Meat — confirmed MAP_TAP
            addIcon("meat",     1430, 245,  1387, 342);

            // Knife — confirmed MAP_TAP
            addIcon("knife",    585,  400,  576,  466);

            // Pickle — estimated offset, use MAP_TAP to confirm
            addIcon("pickle",   1470, 570,  1430, 640);

            // Tomato — estimated offset, use MAP_TAP to confirm
            addIcon("tomato",   1470, 900,  1430, 970);

            // Fish — estimated offset, use MAP_TAP to confirm
            addIcon("fish",     1300, 1150);
            // Dollar / delivery — estimated offset, use MAP_TAP to confirm
            addIcon("dollar", 795, 677);
            addIcon("dollar", 800, 926);

            // Placemat stations — chef stands right at the station
            addIcon("placemat", 295,  370);
            addIcon("placemat", 965,  370);
            addIcon("placemat", 295,  538);
            addIcon("placemat", 965,  538);

            // Stoves — confirmed MAP_TAP (order must match stoves list in GameView!)
            addIcon("stove",    548,  246,  506,  224);
            addIcon("stove",    628,  246,  589,  232);
            addIcon("stove",    714,  246,  672,  241);
        }
    }

    private void addIcon(String type, float x, float y) {
        icons.add(new MapIcon(type, x, y));
    }

    private void addIcon(String type, float drawX, float drawY, float interactX, float interactY) {
        icons.add(new MapIcon(type, drawX, drawY, interactX, interactY));
        Log.d("ICON_LOAD", type + " draw=(" + (int)drawX + "," + (int)drawY
                + ") interact=(" + (int)interactX + "," + (int)interactY + ")");
    }

    // ─────────────────────────────────────────────
    //  Knife clock state
    // ─────────────────────────────────────────────

    public void setChoppingProgress(float progress) {
        stationStates.put("knife", StationState.IN_PROGRESS);
        stationProgress.put("knife", progress);
    }

    public void setChoppingDone() {
        stationStates.put("knife", StationState.DONE);
        stationProgress.put("knife", 1f);
    }

    public void resetKnife() {
        stationStates.put("knife", StationState.IDLE);
        stationProgress.put("knife", 0f);
    }

    // ─────────────────────────────────────────────
    //  Draw stoves BEFORE the chef
    // ─────────────────────────────────────────────

    public void drawStovesOnly(Canvas canvas,
                               Map<String, Bitmap>   staticSprites,
                               List<StoveStation>    stoves,
                               int globalFrame,
                               float cameraX, float cameraY) {
        int stoveIndex = 0;
        for (MapIcon icon : icons) {
            if (!icon.type.equals("stove")) continue;
            if (stoveIndex >= stoves.size()) break;

            StoveStation stove = stoves.get(stoveIndex);
            float cx = cameraX + icon.drawX;
            float cy = cameraY + icon.drawY;

            String spriteName = stove.isOn() ? "stove_on" : "stove_off";
            Bitmap bmp = staticSprites.get(spriteName);
            if (bmp != null) {
                canvas.drawBitmap(bmp, cx - bmp.getWidth() / 2f, cy - bmp.getHeight() / 2f, null);
            }
            stoveIndex++;
        }
    }

    // ─────────────────────────────────────────────
    //  Draw everything except stoves AFTER the chef
    // ─────────────────────────────────────────────

    public void drawIconsExceptStoves(Canvas canvas,
                                      Map<String, Bitmap[]> iconAnimations,
                                      int globalFrame,
                                      float cameraX, float cameraY) {
        for (MapIcon icon : icons) {
            if (icon.type.equals("stove"))    continue;
            if (icon.type.equals("placemat")) continue;

            Bitmap bmp = resolveFrame(icon.type, iconAnimations, globalFrame);
            if (bmp == null) continue;

            float drawX = cameraX + icon.drawX;
            float drawY = cameraY + icon.drawY;
            canvas.drawBitmap(bmp, drawX - bmp.getWidth() / 2f, drawY - bmp.getHeight() / 2f, null);
        }
    }

    // ─────────────────────────────────────────────
    //  Draw stove clocks — always visible, on top of everything
    // ─────────────────────────────────────────────

    public void drawStoveClocks(Canvas canvas,
                                Map<String, Bitmap[]> iconAnimations,
                                Map<String, Bitmap>   staticSprites,
                                List<StoveStation>    stoves,
                                int globalFrame,
                                float cameraX, float cameraY) {
        int stoveIndex = 0;
        for (MapIcon icon : icons) {
            if (!icon.type.equals("stove")) continue;
            if (stoveIndex >= stoves.size()) break;

            StoveStation stove    = stoves.get(stoveIndex);
            float        cx       = cameraX + icon.drawX;
            float        cy       = cameraY + icon.drawY;
            Bitmap       stoveBmp = staticSprites.get("stove_off");
            float        stoveH   = stoveBmp != null ? stoveBmp.getHeight() : 64f;
            float        clockY   = cy - stoveH / 2f - 4f;

            if (stove.isOff()) {
                Bitmap[] blankFrames = iconAnimations.get("clock_blank");
                if (blankFrames != null && blankFrames[0] != null) {
                    Bitmap blank = blankFrames[0];
                    canvas.drawBitmap(blank,
                            cx - blank.getWidth() / 2f,
                            clockY - blank.getHeight(), null);
                }
            } else if (stove.isCooking()) {
                Bitmap[] clockFrames = iconAnimations.get("clock");
                if (clockFrames != null) {
                    float  progress = stove.getProgress();
                    int    idx      = Math.min((int)(progress * clockFrames.length), clockFrames.length - 1);
                    Bitmap clock    = clockFrames[idx];
                    if (clock != null) {
                        canvas.drawBitmap(clock,
                                cx - clock.getWidth() / 2f,
                                clockY - clock.getHeight(), null);
                    }
                }
            } else if (stove.isDone()) {
                Bitmap[] doneFrames = iconAnimations.get("clock_done");
                if (doneFrames != null) {
                    Bitmap clock = doneFrames[globalFrame % doneFrames.length];
                    if (clock != null) {
                        canvas.drawBitmap(clock,
                                cx - clock.getWidth() / 2f,
                                clockY - clock.getHeight(), null);
                    }
                }
            }
            stoveIndex++;
        }
    }

    // ─────────────────────────────────────────────
    //  Frame resolution helpers
    // ─────────────────────────────────────────────

    private Bitmap resolveFrame(String type, Map<String, Bitmap[]> anims, int globalFrame) {
        if (type.equals("knife")) return resolveKnifeFrame(anims, globalFrame);
        Bitmap[] frames = anims.get(type);
        if (frames == null || frames.length == 0) return null;
        return frames[globalFrame % frames.length];
    }

    private Bitmap resolveKnifeFrame(Map<String, Bitmap[]> anims, int globalFrame) {
        StationState state = stationStates.getOrDefault("knife", StationState.IDLE);
        switch (state) {
            case IN_PROGRESS: {
                Bitmap[] clockFrames = anims.get("clock");
                if (clockFrames == null) break;
                float progress = stationProgress.getOrDefault("knife", 0f);
                int   idx      = Math.min((int)(progress * clockFrames.length), clockFrames.length - 1);
                return clockFrames[idx];
            }
            case DONE: {
                Bitmap[] doneFrames = anims.get("clock_done");
                if (doneFrames == null) break;
                return doneFrames[globalFrame % doneFrames.length];
            }
            default: break;
        }
        Bitmap[] chopFrames = anims.get("knife");
        if (chopFrames == null || chopFrames.length == 0) return null;
        return chopFrames[globalFrame % chopFrames.length];
    }

    // ─────────────────────────────────────────────
    //  Interaction — uses interactX/Y, not drawX/Y
    // ─────────────────────────────────────────────

    public String getNearbyStation(float chefMapX, float chefMapY, float radius) {
        String closest = null;
        float  minDist = radius;

        for (MapIcon icon : icons) {
            float dx   = chefMapX - icon.interactX;
            float dy   = chefMapY - icon.interactY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist) {
                minDist = dist;
                closest = icon.type;
            }
        }

        Log.d("NEARBY", "Closest station: " + closest);
        return closest;
    }

    public List<MapIcon> getIcons() { return icons; }
}