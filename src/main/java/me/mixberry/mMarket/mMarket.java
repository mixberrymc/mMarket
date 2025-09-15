package me.mixberry.mMarket;

import lombok.Getter;
import me.mixberry.mMarket.data.MarketDatabase;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.Lang;

import java.sql.SQLException;

public final class mMarket extends SimplePlugin {

    @Getter
    public static MarketDatabase marketDatabase;

    @Getter
    public static mMarket instance;

    @Override
    protected void onPluginStart() {

        Lang.init("messages.yml");

        instance = this;

        try {
            marketDatabase = new MarketDatabase(getDataFolder(), "data.db");
            marketDatabase.open();

        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

    }

    @Override
    protected void onPluginStop() {
        if (marketDatabase != null) {
            marketDatabase.close();
        }
    }

}