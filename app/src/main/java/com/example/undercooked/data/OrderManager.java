package com.example.undercooked.data; // this file belongs to the data package

import java.util.ArrayList; // ArrayList is a list that can grow and shrink dynamically
import java.util.List;      // the base interface for all list types in Java

/**
 * OrderManager — manages the dish list, money and timer for each level.
 *
 * Each level has a list of dishes the player needs to cook and deliver.
 * When a dish is delivered the order is marked as complete and money is added.
 * When the timer runs out the level ends.
 */
public class OrderManager {

    /**
     * Order — represents a single dish in the order list.
     * This is an inner class — it only makes sense inside OrderManager.
     */
    public static class Order {
        public final String  itemName;    // the internal name used to find the sprite (e.g. "cooked_meat")
        public final String  displayName; // the name shown to the player on screen (e.g. "Cooked Meat")
        public final int     price;       // how many coins the player earns for delivering this dish
        public       boolean completed;   // whether this dish has been delivered — starts as false

        // creates a new order with the given details
        public Order(String itemName, String displayName, int price) {
            this.itemName    = itemName;    // save the internal name
            this.displayName = displayName; // save the display name
            this.price       = price;       // save the price
            this.completed   = false;       // the order is not completed yet
        }
    }

    private final List<Order> orders     = new ArrayList<>(); // the list of all dishes for this level
    private       int         totalMoney = 0;     // total coins earned so far
    private       float       timeLeft   = 0f;    // seconds remaining — counts down to zero
    private       boolean     levelOver  = false; // true when the timer hits zero or all orders are done
    private       boolean     playerWon  = false; // true only if all orders were completed before time ran out

    // ─────────────────────────────────────────────
    //  Level Setup
    // ─────────────────────────────────────────────

    // initialises the level — clears old data and adds the dishes for the given level number
    public void loadLevel(int level) {
        orders.clear();    // remove any orders from a previous level
        totalMoney = 0;    // reset money to zero
        levelOver  = false; // the level has not ended yet
        playerWon  = false; // the player has not won yet

        if (level == 1) { // only set up level 1 for now
            timeLeft = 180f; // 3 minutes = 180 seconds

            // 2-ingredient dishes — worth 10 coins each
            addOrder("cooked_meat", "Cooked Meat", 10); // meat cooked on the stove
            addOrder("tomato_soup", "Tomato Soup", 10); // tomato cooked on the stove

            // 3-ingredient dishes — worth 20 coins because they are harder to make
            addOrder("meat_meal_tomato", "Meat Meal Tomato", 20);
            addOrder("meat_meal_onion",  "Meat Meal Onion",  20);
            addOrder("fish_meal_tomato", "Fish Meal Tomato", 20);
            addOrder("fish_meal_onion",  "Fish Meal Onion",  20);
        }
    }

    // helper — creates a new Order and adds it to the list
    private void addOrder(String itemName, String displayName, int price) {
        orders.add(new Order(itemName, displayName, price)); // create and add in one line
    }

    // ─────────────────────────────────────────────
    //  Update — called every frame
    // ─────────────────────────────────────────────

    // counts down the timer — dt is the time in seconds since the last frame
    public void update(float dt) {
        if (levelOver) return; // if the level is already over, do nothing

        timeLeft -= dt; // subtract the time that passed this frame

        if (timeLeft <= 0f) { // if the timer reached zero
            timeLeft  = 0f;   // clamp to zero so it does not go negative
            levelOver = true; // the level has ended
            playerWon = allCompleted(); // win only if every dish was delivered
        }
    }

    // ─────────────────────────────────────────────
    //  Delivery
    // ─────────────────────────────────────────────

    // finds the first incomplete order that matches the given item name
    // call this BEFORE deliver() if you need the order details (e.g. for a notification)
    public Order findMatchingOrder(String itemName) {
        for (int i = 0; i < orders.size(); i++) { // loop through all orders
            Order order = orders.get(i);           // get the current order
            if (!order.completed && order.itemName.equals(itemName)) { // if not done and name matches
                return order; // return the matching order
            }
        }
        return null; // no matching order was found
    }

    // marks the matching order as complete and adds money — returns true if a match was found
    public boolean deliver(String itemName) {
        for (int i = 0; i < orders.size(); i++) { // loop through all orders
            Order order = orders.get(i);           // get the current order
            if (!order.completed && order.itemName.equals(itemName)) { // if not done and name matches
                order.completed = true;        // mark as completed — green checkmark will appear
                totalMoney     += order.price; // add the price to the player's total money
                if (allCompleted()) {          // check if every single order is now done
                    levelOver = true;          // end the level
                    playerWon = true;          // the player wins
                }
                return true; // delivery was successful
            }
        }
        return false; // no matching order — delivery failed
    }

    // ─────────────────────────────────────────────
    //  Getters
    // ─────────────────────────────────────────────

    public List<Order> getOrders()    { return orders; }      // returns the full list of orders
    public int         getMoney()     { return totalMoney; }  // returns total coins earned
    public float       getTimeLeft()  { return timeLeft; }    // returns seconds remaining
    public boolean     isLevelOver()  { return levelOver; }   // returns whether the level has ended
    public boolean     didPlayerWin() { return playerWon; }   // returns whether the player won

    // returns the time as a "MM:SS" string — for example "02:45"
    public String getTimeString() {
        int mins = (int)(timeLeft / 60); // how many full minutes are left
        int secs = (int)(timeLeft % 60); // how many seconds are left after the minutes
        return String.format("%02d:%02d", mins, secs); // pad with zeros — e.g. "02:05"
    }

    // returns true only if every order in the list has been completed
    private boolean allCompleted() {
        for (int i = 0; i < orders.size(); i++) {    // loop through all orders
            if (!orders.get(i).completed) return false; // found an incomplete order — return false
        }
        return true; // every order is complete
    }
}