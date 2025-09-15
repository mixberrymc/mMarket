package me.mixberry.mMarket.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
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

public final class MarketDatabase implements AutoCloseable {

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS listings (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " seller_uuid TEXT NOT NULL," +
                    " price REAL NOT NULL CHECK(price >= 0)," +
                    " item_json TEXT NOT NULL," +
                    " created_at INTEGER NOT NULL" +
                    ");";

    private static final String CREATE_IDX_SELLER =
            "CREATE INDEX IF NOT EXISTS idx_listings_seller ON listings (seller_uuid);";

    private static final String CREATE_IDX_CREATED =
            "CREATE INDEX IF NOT EXISTS idx_listings_created ON listings (created_at);";

    private static final String CREATE_TABLE_PAYOUTS =
            "CREATE TABLE IF NOT EXISTS pending_payouts (" +
                    " uuid TEXT PRIMARY KEY," +
                    " amount REAL NOT NULL CHECK(amount >= 0)" +
                    ");";

    private static final Type MAP_STRING_OBJECT = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Gson GSON = new Gson();

    private final String jdbcUrl;
    private Connection conn;

    public MarketDatabase(File dataFolder, String fileName) {
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }
        File db = new File(dataFolder, fileName);
        this.jdbcUrl = "jdbc:sqlite:" + db.getAbsolutePath();
    }

    public void open() throws SQLException {
        this.conn = DriverManager.getConnection(jdbcUrl);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL;");
            st.execute("PRAGMA synchronous = NORMAL;");
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute(CREATE_TABLE_SQL);
            st.execute(CREATE_IDX_SELLER);
            st.execute(CREATE_IDX_CREATED);
            st.execute(CREATE_TABLE_PAYOUTS);
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignore) {}
    }

    // --- Data model ---
    public static final class Listing {
        public final long id;
        public final UUID sellerUuid;
        public final double price;
        public final ItemStack item;
        public final long createdAtMillis;

        public Listing(long id, UUID sellerUuid, double price, ItemStack item, long createdAtMillis) {
            this.id = id;
            this.sellerUuid = sellerUuid;
            this.price = price;
            this.item = item;
            this.createdAtMillis = createdAtMillis;
        }
    }

    // --- Legacy JSON helpers ---
    private static String itemToJson(ItemStack stack) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = stack.serialize();
        return GSON.toJson(map);
    }

    private static ItemStack itemFromJson(String json) {
        Map<String, Object> map = GSON.fromJson(json, MAP_STRING_OBJECT);
        return ItemStack.deserialize(map);
    }

    // --- Base64 helpers ---
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
            Object obj = ois.readObject();
            return (ItemStack) obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize ItemStack", e);
        }
    }

    // --- Unified reader ---
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
    public long insertListing(UUID sellerUuid, double price, ItemStack item) throws SQLException {
        final String sql = "INSERT INTO listings (seller_uuid, price, item_json, created_at) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sellerUuid.toString());
            ps.setDouble(2, price);
            ps.setString(3, itemToBase64(item)); // now stores Base64
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public Listing getListing(long id) throws SQLException {
        final String sql = "SELECT id, seller_uuid, price, item_json, created_at FROM listings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Listing(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("seller_uuid")),
                        rs.getDouble("price"),
                        itemFromStorage(rs.getString("item_json")),
                        rs.getLong("created_at")
                );
            }
        }
    }

    public List<Listing> listRecent(int limit) throws SQLException {
        final String sql = "SELECT id, seller_uuid, price, item_json, created_at " +
                "FROM listings ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Listing> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Listing(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getDouble("price"),
                            itemFromStorage(rs.getString("item_json")),
                            rs.getLong("created_at")
                    ));
                }
                return out;
            }
        }
    }

    public int deleteListing(long id) throws SQLException {
        final String sql = "DELETE FROM listings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    public List<Listing> findBySeller(UUID sellerUuid, int limit) throws SQLException {
        final String sql = "SELECT id, seller_uuid, price, item_json, created_at " +
                "FROM listings WHERE seller_uuid = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerUuid.toString());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Listing> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Listing(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getDouble("price"),
                            itemFromStorage(rs.getString("item_json")),
                            rs.getLong("created_at")
                    ));
                }
                return out;
            }
        }
    }

    public List<Integer> getAll() throws SQLException {
        final String sql = "SELECT id FROM listings ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
            return ids;
        }
    }

    public ItemStack getItemStack(int id) throws SQLException {
        final String sql = "SELECT item_json FROM listings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return itemFromStorage(rs.getString("item_json"));
            }
        }
    }

    public double getPrice(int id) throws SQLException {
        final String sql = "SELECT price FROM listings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("price") : -1.0;
            }
        }
    }

    public UUID getSeller(int id) throws SQLException {
        final String sql = "SELECT seller_uuid FROM listings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("seller_uuid")) : null;
            }
        }
    }

    public long getCreatedDate(int id) throws SQLException {
        final String sql = "SELECT created_at FROM listings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("created_at") : -1L;
            }
        }
    }

    // --- Search by keyword (material & display name) ---
    public List<Integer> searchIds(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }

        final String sql = "SELECT id, item_json FROM listings ORDER BY id ASC";
        List<Integer> ids = new ArrayList<>();
        String lower = keyword.toLowerCase(Locale.ROOT);

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                ItemStack stack = itemFromStorage(rs.getString("item_json"));
                if (stack == null) continue;

                String matName = stack.getType().name().toLowerCase(Locale.ROOT);
                String matHuman = matName.replace('_', ' ');
                String display = "";
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                    display = stack.getItemMeta().getDisplayName()
                            .replaceAll("§[0-9a-fk-or]", "")
                            .toLowerCase(Locale.ROOT);
                }

                if (matName.contains(lower) ||
                        matHuman.contains(lower) ||
                        display.contains(lower)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }


    public void addPendingPayout(UUID uuid, double amount) throws SQLException {
        if (amount <= 0) return;
        final String sql =
                "INSERT INTO pending_payouts (uuid, amount) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET amount = amount + excluded.amount";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.executeUpdate();
        }
    }


    public double getPendingPayout(UUID uuid) throws SQLException {
        final String sql = "SELECT amount FROM pending_payouts WHERE uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("amount") : 0.0D;
            }
        }
    }


    public double getAndClearPendingPayout(UUID uuid) throws SQLException {
        boolean oldAuto = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            double amt = 0.0D;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT amount FROM pending_payouts WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) amt = rs.getDouble("amount");
                }
            }

            if (amt > 0.0D) {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM pending_payouts WHERE uuid = ?")) {
                    del.setString(1, uuid.toString());
                    del.executeUpdate();
                }
            }

            conn.commit();
            return amt;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAuto);
        }
    }



    public List<Integer> getIDListFromSeller(Player seller) throws SQLException {
        if (seller == null) return Collections.emptyList();
        final String sql = "SELECT id FROM listings WHERE seller_uuid = ? ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seller.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
                return ids;
            }
        }
    }

    public List<Integer> getIDListFromSeller(Player seller, int limit) throws SQLException {
        if (seller == null) return Collections.emptyList();
        final String sql = "SELECT id FROM listings WHERE seller_uuid = ? ORDER BY id ASC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seller.getUniqueId().toString());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
                return ids;
            }
        }
    }


}
