package me.mixberry.mMarket.commands;

import me.mixberry.mMarket.mMarket;
import me.mixberry.mMarket.utils.Settings;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.settings.Lang;

import java.util.List;

@AutoRegister
public final class SellConfirmCommand extends SimpleCommand {

    public SellConfirmCommand() {
        super("confirmsell");
        setPermission(null);
    }

    @Override
    protected void onCommand() {

        try {

            if (getPlayer().getInventory().getItemInMainHand() == null || getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR) {
                getPlayer().sendMessage(Common.colorize(Lang.of("not_holding_item")));
                return;
            }

            if (HookManager.getBalance(getPlayer()) < Settings.PLAYERMARKET_FEE) {
                getPlayer().sendMessage(Common.colorize(Lang.of("not_enough_fee")).replace("{fee}", Double.toString(Settings.PLAYERMARKET_FEE)));
                return;
            }

            double price = Double.parseDouble(args[0]);

            mMarket.getMarketDatabase().insertListing(getPlayer().getUniqueId(), price, getPlayer().getInventory().getItemInMainHand());
            getPlayer().getInventory().setItem(getPlayer().getInventory().getHeldItemSlot(), null);
            HookManager.withdraw(getPlayer(), Settings.PLAYERMARKET_FEE);

            getPlayer().playSound(getPlayer(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 2F);

        } catch (Exception e) {
            getPlayer().sendMessage(Common.colorize("&cThis command is not intended to use directly, please contact administrator of the server if you think this is an error."));
        }


    }

    @Override
    protected List<String> tabComplete() {
        return NO_COMPLETE;
    }
}
