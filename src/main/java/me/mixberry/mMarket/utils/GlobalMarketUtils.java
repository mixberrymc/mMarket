package me.mixberry.mMarket.utils;

import me.mixberry.mMarket.data.GlobalMarketDatabase;
import me.mixberry.mMarket.data.GlobalMarketDatabase.GlobalItem;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;

/**
 * QoL wrapper around GlobalMarketDatabase so callers don't need to catch SQLException.
 * All exceptions are wrapped into RuntimeException and will surface in console if they happen.
 */
public final class GlobalMarketUtils {

    private static GlobalMarketDatabase db;

    private GlobalMarketUtils() {
    }

    /**
     * Must be called once on plugin enable
     */
    public static void init(GlobalMarketDatabase database) {
        db = database;
    }

    private static void checkInit() {
        if (db == null) throw new IllegalStateException("GlobalMarketUtils not initialized with a database");
    }

    /* ------------------- Insert / Delete ------------------- */

    public static long addProduct(String name, String category, double buyPrice, double sellPrice, int stock, ItemStack item) {
        checkInit();
        try {
            return db.insertProduct(name, category, buyPrice, sellPrice, stock, item);
        } catch (SQLException e) {
            throw new RuntimeException("DB insertProduct failed", e);
        }
    }

    public static void removeProduct(long id) {
        checkInit();
        try {
            db.deleteProduct(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB deleteProduct failed", e);
        }
    }

    /* ------------------- Getters ------------------- */

    public static GlobalItem getProduct(long id) {
        checkInit();
        try {
            return db.getProduct(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB getProduct failed", e);
        }
    }

    public static List<GlobalItem> listAll() {
        checkInit();
        try {
            return db.listAll();
        } catch (SQLException e) {
            throw new RuntimeException("DB listAll failed", e);
        }
    }

    public static List<GlobalItem> listByCategory(String category) {
        checkInit();
        try {
            return db.listByCategory(category);
        } catch (SQLException e) {
            throw new RuntimeException("DB listByCategory failed", e);
        }
    }

    public static List<Long> searchIds(String keyword) {
        checkInit();
        try {
            return db.searchIds(keyword);
        } catch (SQLException e) {
            throw new RuntimeException("DB searchIds failed", e);
        }
    }

    /* ------------------- Field helpers ------------------- */

    public static String getName(long id) {
        checkInit();
        try {
            return db.getName(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB getName failed", e);
        }
    }

    public static String getCategory(long id) {
        checkInit();
        try {
            return db.getCategory(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB getCategory failed", e);
        }
    }

    public static double getBuyPrice(long id) {
        checkInit();
        try {
            return db.getBuyPrice(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB getBuyPrice failed", e);
        }
    }

    public static double getSellPrice(long id) {
        checkInit();
        try {
            return db.getSellPrice(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB getSellPrice failed", e);
        }
    }

    public static int getStock(long id) {
        checkInit();
        try {
            return db.getStock(id);
        } catch (SQLException e) {
            throw new RuntimeException("DB getStock failed", e);
        }
    }

    /* ------------------- Mutations ------------------- */

    public static void setPrice(long id, double buyPrice, double sellPrice) {
        checkInit();
        try {
            db.setPrice(id, buyPrice, sellPrice);
        } catch (SQLException e) {
            throw new RuntimeException("DB setPrices failed", e);
        }
    }

    public static void setName(long id, String name) {
        checkInit();
        try {
            db.setName(id, name);
        } catch (SQLException e) {
            throw new RuntimeException("DB setName failed", e);
        }
    }

    public static void setCategory(long id, String category) {
        checkInit();
        try {
            db.setCategory(id, category);
        } catch (SQLException e) {
            throw new RuntimeException("DB setCategory failed", e);
        }
    }

    public static void setStock(long id, int stock) {
        checkInit();
        try {
            db.setStock(id, stock);
        } catch (SQLException e) {
            throw new RuntimeException("DB setStock failed", e);
        }
    }
}