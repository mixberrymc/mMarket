package me.mixberry.mMarket.commands;

import me.mixberry.mMarket.menus.playermarket.list.PlayerMarketListMenu;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommand;

import java.sql.SQLException;
import java.util.List;

@AutoRegister
public final class PlayerMarketCommand extends SimpleCommand {

    public PlayerMarketCommand() {
        super("playermarket/market");
        setPermission(null);

    }

    @Override
    protected void onCommand() {
        try {
            new PlayerMarketListMenu(getPlayer(), null, PlayerMarketListMenu.SortingType.NEWEST).displayTo(getPlayer());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected List<String> tabComplete() {
        return super.tabComplete();
    }


}
