package me.mixberry.mMarket.utils;

import me.mixberry.mMarket.data.MarketDatabase;
import me.mixberry.mMarket.mMarket;
import org.bukkit.entity.Player;
import org.mineacademy.fo.TimeUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerMarketUtils {

    private static List<UUID> sellMode = new ArrayList<>();

    public static void setSellMode(Player player, boolean bool) {
        if (!bool && sellMode.contains(player.getUniqueId())) {
            sellMode.remove(player.getUniqueId());
        } else if (bool && !sellMode.contains(player.getUniqueId())) {
            sellMode.add(player.getUniqueId());
        }
    }

    public static boolean isPlayerInSellMode(Player player) {
        return sellMode.contains(player.getUniqueId());
    }

    public static String getItemRemainTime(int id) throws SQLException {
        MarketDatabase marketData = mMarket.getMarketDatabase();
        return TimeUtil.formatTimeShort(((marketData.getCreatedDate(id) + TimeUtil.toMilliseconds(Settings.PLAYERMARKET_EXPIRATION_DURATION)) - System.currentTimeMillis()) / 1000);
    }

    public static boolean isItemExpired(int id) {


        try {
            MarketDatabase marketData = mMarket.getMarketDatabase();
            long time = ((marketData.getCreatedDate(id) + TimeUtil.toMilliseconds(Settings.PLAYERMARKET_EXPIRATION_DURATION)) - System.currentTimeMillis()) / 1000;

            if (time >= 1) {
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

}