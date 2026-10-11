package com.example.undercooked.data; // this file belongs to the data package

import java.util.Arrays;  // used to sort arrays — we use this to create consistent recipe keys
import java.util.HashMap; // HashMap stores key-value pairs, like a dictionary
import java.util.Map;     // the base interface for HashMap

/**
 * RecipeManager — stores and looks up all recipes in the game.
 *
 * There are three types of actions:
 * 1. CHOP   — knife station (e.g. meat → chopped meat)
 * 2. COOK   — stove        (e.g. meat → cooked meat)
 * 3. COMBINE — placemat    (e.g. cooked meat + rice → meat meal)
 *
 * All recipes are stored in a HashMap so lookups are very fast.
 * The class is never instantiated — all methods are static.
 */
public class RecipeManager {

    // CHOP: key = ingredient to chop, value = result after chopping
    private static final Map<String, String> CHOP = new HashMap<>();

    // COOK: key = ingredient to cook, value = result after cooking
    private static final Map<String, String> COOK = new HashMap<>();

    // COMBINE2: key = two sorted ingredient names joined by "+", value = the finished dish
    private static final Map<String, String> COMBINE2 = new HashMap<>();

    // static block — runs once when the class is loaded into memory, fills all the maps
    static {

        // ── Chop recipes (knife station) ──────────
        CHOP.put("meat",   "chopped_meat");   // meat → chopped meat
        CHOP.put("fish",   "chopped_fish");   // fish → chopped fish
        CHOP.put("tomato", "chopped_tomato"); // tomato → chopped tomato
        CHOP.put("onion",  "chopped_onion");  // onion → chopped onion
        CHOP.put("pickle", "chopped_pickle"); // pickle → chopped pickle

        // ── Cook recipes (stove) ──────────────────
        COOK.put("meat",         "cooked_meat");         // meat → cooked meat
        COOK.put("fish",         "cooked_fish");         // fish → cooked fish
        COOK.put("tomato",       "tomato_soup");         // tomato → tomato soup
        COOK.put("chopped_meat", "cooked_chopped_meat"); // chopped meat → cooked chopped meat
        COOK.put("chopped_fish", "cooked_chopped_fish"); // chopped fish → cooked chopped fish

        // ── Combine recipes (placemat — 2 ingredients) ────
        add2("cooked_meat", "rice",          "meat_meal");         // cooked meat + rice = meat meal
        add2("cooked_fish", "rice",          "fish_meal");         // cooked fish + rice = fish meal
        add2("cooked_meat", "chopped_onion", "cooked_meat_onion"); // cooked meat + chopped onion
        add2("cooked_fish", "chopped_onion", "cooked_fish_onion"); // cooked fish + chopped onion
        add2("cooked_meat", "tomato",        "cooked_meat_tomato"); // cooked meat + tomato
        add2("cooked_fish", "tomato",        "cooked_fish_tomato"); // cooked fish + tomato

        // ── Combine recipes (placemat — step 2 for full meals) ────
        // to get meat_meal_tomato: first combine cooked_meat + rice → meat_meal, then add tomato
        add2("meat_meal", "tomato", "meat_meal_tomato"); // meat meal + tomato = meat meal tomato
        add2("meat_meal", "onion",  "meat_meal_onion");  // meat meal + onion  = meat meal onion
        add2("fish_meal", "tomato", "fish_meal_tomato"); // fish meal + tomato = fish meal tomato
        add2("fish_meal", "onion",  "fish_meal_onion");  // fish meal + onion  = fish meal onion
    }

    // helper — adds a 2-ingredient recipe to the map
    // we sort the names so the order does not matter:
    // add2("rice","meat") and add2("meat","rice") produce the same key
    private static void add2(String a, String b, String result) {
        String[] p = {a, b}; // put both ingredients into an array
        Arrays.sort(p);       // sort alphabetically so order does not matter
        COMBINE2.put(p[0] + "+" + p[1], result); // store with key like "meat+rice"
    }

    // ─────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────

    // returns the result of chopping the given item — null if it cannot be chopped
    public static String chop(String item)  { return CHOP.get(item); }

    // returns the result of cooking the given item — null if it cannot be cooked
    public static String cook(String item)  { return COOK.get(item); }

    // returns true if the item can be chopped at the knife station
    public static boolean isChoppable(String item) { return CHOP.containsKey(item); }

    // returns true if the item can be cooked on the stove
    public static boolean isCookable(String item)  { return COOK.containsKey(item); }

    // tries to combine two items on the placemat — returns the dish name or null if no recipe exists
    public static String combine2(String a, String b) {
        String[] p = {a, b}; // put both names in an array
        Arrays.sort(p);       // sort so order does not matter
        return COMBINE2.get(p[0] + "+" + p[1]); // look up in the map
    }

    // returns true if the item is a finished dish that can be sold at the dollar station
    public static boolean isMeal(String item) {
        if (item.equals("meat_meal"))          return true;
        if (item.equals("fish_meal"))          return true;
        if (item.equals("meat_meal_tomato"))   return true;
        if (item.equals("meat_meal_onion"))    return true;
        if (item.equals("fish_meal_tomato"))   return true;
        if (item.equals("fish_meal_onion"))    return true;
        if (item.equals("cooked_meat"))        return true;
        if (item.equals("cooked_fish"))        return true;
        if (item.equals("cooked_chopped_meat"))return true;
        if (item.equals("cooked_chopped_fish"))return true;
        if (item.equals("cooked_meat_onion"))  return true;
        if (item.equals("cooked_fish_onion"))  return true;
        if (item.equals("cooked_meat_tomato")) return true;
        if (item.equals("cooked_fish_tomato")) return true;
        if (item.equals("tomato_soup"))        return true;
        if (item.equals("struggle_meal"))      return true;
        return false; // not a finished dish
    }
}