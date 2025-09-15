package me.mixberry.mMarket.menus.playermarket.list;


import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.settings.Lang;

import java.util.Collections;

public class PlayerMarketSearchMenu {


    SignGUI gui;
    Player player;
    Class<? extends Menu> parentMenu;

    public PlayerMarketSearchMenu(Player player, Class<? extends Menu> parentMenu) {

        try {
            getSignGui();
        } catch (SignGUIVersionException e) {
            throw new RuntimeException(e);
        }

        this.player = player;
        this.parentMenu = parentMenu;

        if (player == null) return;
        gui.open(player);
    }


    private void getSignGui() throws SignGUIVersionException {
        gui = SignGUI.builder()

                .setLine(0, Lang.of("Player_Market_Menu.search.sign_input.line1"))
                .setLine(1, Lang.of("Player_Market_Menu.search.sign_input.line2"))
                .setLine(2, Lang.of("Player_Market_Menu.search.sign_input.line3"))
                .setLine(3, Lang.of("Player_Market_Menu.search.sign_input.line4"))

                .setType(Material.BIRCH_SIGN)

                .setHandler((p, result) -> {
                    String line0 = result.getLine(0);

                    if (player != null) {
                        try {
                            Menu newMenu = parentMenu.getConstructor(Player.class, String.class)
                                    .newInstance(player, line0);
                            newMenu.displayTo(player);
                        } catch (Exception e) {
                            Common.warning("SearchMenu: Player or class not found!");
                        }
                    }
                    return Collections.emptyList();

                })
                .build();

    }

}