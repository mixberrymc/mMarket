package me.mixberry.mMarket.utils;

import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;

public class Settings extends SimpleSettings {

    // Player Market settings
    public static Double PLAYERMARKET_MINIMUM_PRICE;
    public static Double PLAYERMARKET_FEE;
    public static Boolean PLAYERMARKET_EXPIRATION;
    public static String PLAYERMARKET_EXPIRATION_DURATION;

    // Decoration settings
    public static CompMaterial PRIMARY_COLOR_ITEM;
    public static CompMaterial SECONDARY_COLOR_ITEM;
    public static CompMaterial ACCENT_COLOR_ITEM;

    private static void init() {
        // Load player market settings
        setPathPrefix("Player_Market");
        PLAYERMARKET_MINIMUM_PRICE = getDouble("minimum_price");
        PLAYERMARKET_FEE = getDouble("fee");
        PLAYERMARKET_EXPIRATION = getBoolean("expiration.enable");
        PLAYERMARKET_EXPIRATION_DURATION = getString("expiration.duration");

        // Load decoration settings
        setPathPrefix("Decoration");
        PRIMARY_COLOR_ITEM = getMaterial("primary_color_item");
        SECONDARY_COLOR_ITEM = getMaterial("secondary_color_item");
        ACCENT_COLOR_ITEM = getMaterial("accent_color_item");
    }
}