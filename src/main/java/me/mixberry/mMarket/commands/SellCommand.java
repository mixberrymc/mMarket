package me.mixberry.mMarket.commands;

import me.mixberry.mMarket.utils.Settings;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.settings.Lang;

import java.util.List;

@AutoRegister
public final class SellCommand extends SimpleCommand {

    public SellCommand() {
        super("sell");
        setPermission(null);
    }

    @Override
    protected void onCommand() {
        if (args.length == 1) {
            try {

                if (getPlayer().getInventory().getItemInMainHand() == null || getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR) {
                    getPlayer().sendMessage(Common.colorize(Lang.of("not_holding_item")));
                    return;
                }

                double price = Double.parseDouble(args[0]);
                getPlayer().playSound(getPlayer(), Sound.UI_BUTTON_CLICK, 0.5F, 0.5F);

                List<String> messages = Lang.ofList("confirm_sell_messages");
                for (String message : messages) {
                    Common.tellNoPrefix(getPlayer(), message
                            .replace("{item}", getPlayer().getInventory().getItemInMainHand().getType().name())
                            .replace("{itemAmount}", Integer.toString(getPlayer().getInventory().getItemInMainHand().getAmount()))
                            .replace("{fee}", Double.toString(Settings.PLAYERMARKET_FEE))
                            .replace("{price}", Double.toString(price))
                    );
                }

                SimpleComponent.of(Lang.of("confirm_sell_button"))
                        .onHover(Lang.of("confirm_sell_hover"))
                        .onClickRunCmd("confirmsell " + price)
                        .send(getPlayer());

            } catch (Exception e) {
                getPlayer().sendMessage(Common.colorize("&#e01436Usage: &f/sell <price>"));
                e.printStackTrace();
            }
        } else {
            getPlayer().sendMessage(Common.colorize("&#e01436Usage: &f/sell <price>"));
        }
    }

    @Override
    protected List<String> tabComplete() {
        return NO_COMPLETE;
    }
}