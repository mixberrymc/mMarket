package me.mixberry.mMarket.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

/**
 * Global shop (admin-curated) with stock limit per product.
 * Storage: ItemStack in Base64; read supports legacy JSON for compatibility.
 *
 * Columns:
 * - id            INTEGER PK AUTOINCREMENT
 * - name          TEXT NOT NULL            (admin placeholder / product key)
 * - category      TEXT NOT NULL
 * - buy_price     REAL NOT NULL CHECK >= 0 (players pay this to buy from shop)
 * - sell_price    REAL NOT NULL CHECK >= 0 (players receive this when selling to shop)
 * - limit_amount  INTEGER NOT NULL CHECK >=0 (current stock)
 * - item_data     TEXT NOT NULL            (serialized ItemStack)
 */
public final class GlobalMarketDatabase implements AutoCloseable {

    // --- Schema ---
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS global_shop (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  name TEXT NOT NULL," +
                    "  category TEXT NOT NULL," +
                    "  buy_price REAL NOT NULL CHECK(buy_price >= 0)," +
                    "  sell_price REAL NOT NULL CHECK(sell_price >= 0)," +
                    "  limit_amount INTEGER NOT NULL CHECK(limit_amount >= 0)," +
                    "  item_data TEXT NOT NULL" +
                    ");";

    private static final String CREATE_IDX_CATEGORY =
            "CREATE INDEX IF NOT EXISTS idx_global_shop_category ON global_shop(category);";
    private static final String CREATE_IDX_NAME =
            "CREATE INDEX IF NOT EXISTS idx_global_shop_name ON global_shop(name);";
    private static final String CREATE_IDX_STOCK =
            "CREATE INDEX IF NOT EXISTS idx_global_shop_stock ON global_shop(limit_amount);";

    private static final Gson GSON = new Gson();
    private static final Type MAP_STRING_OBJECT = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final String jdbcUrl; // jdbc:sqlite:/abs/path/to.db
    private Connection conn;

    public GlobalMarketDatabase(File dataFolder, String fileName) {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File db = new File(dataFolder, fileName);
        this.jdbcUrl = "jdbc:sqlite:" + db.getAbsolutePath();
    }

    // --- Lifecycle ---
    public void open() throws SQLException {
        this.conn = DriverManager.getConnection(jdbcUrl);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL;");
            st.execute("PRAGMA synchronous = NORMAL;");
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute(CREATE_TABLE_SQL);
            st.execute(CREATE_IDX_CATEGORY);
            st.execute(CREATE_IDX_NAME);
            st.execute(CREATE_IDX_STOCK);
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignore) {
        }
    }

    // --- Model ---
    public static final class GlobalItem {
        public final long id;
        public final String name;       // admin placeholder / product key
        public final String category;
        public final double buyPrice;   // players pay to buy
        public final double sellPrice;  // players get when selling
        public final int limitAmount;   // current stock
        public final ItemStack item;

        public GlobalItem(long id, String name, String category, double buyPrice, double sellPrice, int limitAmount, ItemStack item) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.limitAmount = limitAmount;
            this.item = item;
        }
    }

    // --- Serialization helpers (Base64 first; legacy JSON fallback) ---
    private static String itemToBase64(ItemStack item) {
        if (item == null) return "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeObject(item);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize ItemStack", e);
        }
    }

    private static ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize ItemStack", e);
        }
    }

    private static String itemToJson(ItemStack stack) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = stack.serialize();
        return GSON.toJson(map);
    }

    private static ItemStack itemFromJson(String json) {
        Map<String, Object> map = GSON.fromJson(json, MAP_STRING_OBJECT);
        return ItemStack.deserialize(map);
    }

    private static ItemStack itemFromStorage(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return itemFromBase64(raw);
        } catch (Exception ex) {
            try {
                return itemFromJson(raw);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    // --- CRUD ---
    public long insertProduct(String name, String category, double buyPrice, double sellPrice, int limitAmount, ItemStack item) throws SQLException {
        final String sql = "INSERT INTO global_shop (name, category, buy_price, sell_price, limit_amount, item_data) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, Math.max(0, buyPrice));
            ps.setDouble(4, Math.max(0, sellPrice));
            ps.setInt(5, Math.max(0, limitAmount));
            ps.setString(6, itemToBase64(item));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int deleteProduct(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM global_shop WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    public GlobalItem getProduct(long id) throws SQLException {
        final String sql = "SELECT id, name, category, buy_price, sell_price, limit_amount, item_data FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new GlobalItem(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("buy_price"),
                        rs.getDouble("sell_price"),
                        rs.getInt("limit_amount"),
                        itemFromStorage(rs.getString("item_data"))
                );
            }
        }
    }

    public List<GlobalItem> listAll() throws SQLException {
        final String sql = "SELECT id, name, category, buy_price, sell_price, limit_amount, item_data FROM global_shop ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<GlobalItem> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new GlobalItem(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("buy_price"),
                        rs.getDouble("sell_price"),
                        rs.getInt("limit_amount"),
                        itemFromStorage(rs.getString("item_data"))
                ));
            }
            return out;
        }
    }

    public List<GlobalItem> listByCategory(String category) throws SQLException {
        final String sql = "SELECT id, name, category, buy_price, sell_price, limit_amount, item_data " +
                "FROM global_shop WHERE category = ? ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                List<GlobalItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new GlobalItem(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getDouble("buy_price"),
                            rs.getDouble("sell_price"),
                            rs.getInt("limit_amount"),
                            itemFromStorage(rs.getString("item_data"))
                    ));
                }
                return out;
            }
        }
    }

    // --- Search (by admin name, category, material enum/human, display name) ---
    public List<Long> searchIds(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            final String allSql = "SELECT id FROM global_shop ORDER BY id ASC";
            try (PreparedStatement ps = conn.prepareStatement(allSql);
                 ResultSet rs = ps.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong("id"));
                return ids;
            }
        }

        final String sql = "SELECT id, name, category, item_data FROM global_shop ORDER BY id ASC";
        final String lower = keyword.toLowerCase(Locale.ROOT);

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Long> ids = new ArrayList<>();
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String category = rs.getString("category");
                String itemRaw = rs.getString("item_data");
                ItemStack stack = itemFromStorage(itemRaw);

                boolean match = false;

                // admin placeholder name
                if (!match && name != null && name.toLowerCase(Locale.ROOT).contains(lower)) match = true;

                // category
                if (!match && category != null && category.toLowerCase(Locale.ROOT).contains(lower)) match = true;

                // item material & display name
                if (!match && stack != null) {
                    String matName = stack.getType().name().toLowerCase(Locale.ROOT); // e.g. diamond_sword
                    String matHuman = matName.replace('_', ' ');                       // e.g. diamond sword
                    String display = "";
                    if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                        display = stack.getItemMeta().getDisplayName()
                                .replaceAll("§[0-9a-fk-or]", "")
                                .toLowerCase(Locale.ROOT);
                    }
                    if (matName.contains(lower) || matHuman.contains(lower) || display.contains(lower)) {
                        match = true;
                    }
                }

                if (match) ids.add(id);
            }
            return ids;
        }
    }

    // --- Field getters ---
    public ItemStack getItemStack(long id) throws SQLException {
        final String sql = "SELECT item_data FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return itemFromStorage(rs.getString("item_data"));
            }
        }
    }

    public int getStock(long id) throws SQLException {
        final String sql = "SELECT limit_amount FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("limit_amount") : -1;
            }
        }
    }

    public double getBuyPrice(long id) throws SQLException {
        final String sql = "SELECT buy_price FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("buy_price") : -1.0;
            }
        }
    }

    public double getSellPrice(long id) throws SQLException {
        final String sql = "SELECT sell_price FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("sell_price") : -1.0;
            }
        }
    }

    public String getName(long id) throws SQLException {
        final String sql = "SELECT name FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("name") : null;
            }
        }
    }

    public String getCategory(long id) throws SQLException {
        final String sql = "SELECT category FROM global_shop WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("category") : null;
            }
        }
    }

    // --- Mutations ---
    public int setPrice(long id, double buyPrice, double sellPrice) throws SQLException {
        final String sql = "UPDATE global_shop SET buy_price = ?, sell_price = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, Math.max(0, buyPrice));
            ps.setDouble(2, Math.max(0, sellPrice));
            ps.setLong(3, id);
            return ps.executeUpdate();
        }
    }

    public int setCategory(long id, String category) throws SQLException {
        final String sql = "UPDATE global_shop SET category = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }

    public int setName(long id, String name) throws SQLException {
        final String sql = "UPDATE global_shop SET name = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }

    public int setStock(long id, int newStock) throws SQLException {
        final String sql = "UPDATE global_shop SET limit_amount = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, newStock));
            ps.setLong(2, id);
            return ps.executeUpdate();
        }
    }

}