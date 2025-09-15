package me.mixberry.mMarket.events;

import me.mixberry.mMarket.data.MarketDatabase;
import me.mixberry.mMarket.mMarket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.settings.Lang;

import java.sql.SQLException;

public class PlayerMarketEvent implements Listener {

    MarketDatabase database = mMarket.getMarketDatabase();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) throws SQLException {
        Player player = e.getPlayer();

        double money = database.getAndClearPendingPayout(player.getUniqueId());
        if (money > 0.00) {
            HookManager.deposit(player, money);
            player.sendMessage(Common.colorize(Lang.of("sold_message").replace("{price}", Double.toString(money))));

        }

    }

}
