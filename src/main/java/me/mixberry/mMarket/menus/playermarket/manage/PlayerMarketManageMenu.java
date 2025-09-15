package me.mixberry.mMarket.menus.playermarket.manage;

import me.mixberry.mMarket.data.MarketDatabase;
import me.mixberry.mMarket.mMarket;
import me.mixberry.mMarket.menus.playermarket.list.PlayerMarketListMenu;
import me.mixberry.mMarket.utils.MenuPaged;
import me.mixberry.mMarket.utils.PlayerMarketUtils;
import me.mixberry.mMarket.utils.Settings;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.ButtonReturnBack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class PlayerMarketManageMenu extends MenuPaged<Integer> {


    MarketDatabase database = mMarket.marketDatabase;

    private static final ArrayList<Integer> materialSlots = new ArrayList<>();

    static {
        Integer[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
        materialSlots.addAll(Arrays.asList(slots));
    }

    public PlayerMarketManageMenu(Player player) throws SQLException {
        super(new PlayerMarketListMenu(player, null, PlayerMarketListMenu.SortingType.NEWEST), 9 * 6, materialSlots, mMarket.getMarketDatabase().getIDListFromSeller(player, materialSlots.size()));

        setTitle(Lang.of("Player_Market_Menu.title"));
        setSound(new SimpleSound(Sound.ITEM_BOOK_PAGE_TURN, 1, 1));
        setViewer(player);
        displayTo(player);


    }


    @Override
    protected ItemStack convertToItemStack(Integer id) throws SQLException {
        ItemStack stack = database.getItemStack(id);

        if (stack == null) {
            return null;
        }

        return ItemCreator.of(stack)
                .lore(
                        Lang.of("Player_Market_Menu.market_item.lore")
                                .replace("{price}", Double.toString(mMarket.getMarketDatabase().getPrice(id)))
                                .replace("{seller}", getViewer().getName())
                                .replace("{date}", PlayerMarketUtils.isItemExpired(id) ? Lang.of("Player_Market_Menu.manage_item.expire_status") : PlayerMarketUtils.getItemRemainTime(id)),
                        PlayerMarketUtils.isItemExpired(id) ? Lang.of("Player_Market_Menu.manage_item.expired_message") : Lang.of("Player_Market_Menu.manage_item.cancel_message")
                ).make();
    }

    @Override
    public ItemStack getItemAt(int slot) {
        ItemStack item = super.getItemAt(slot);

        if (slot <= 44 && !materialSlots.contains(slot)) {
            return item != null
                    ? item
                    : ItemCreator.of(Settings.SECONDARY_COLOR_ITEM, " ").make();
        }

        if (slot >= 45 && item == null) {
            return ItemCreator.of(Settings.ACCENT_COLOR_ITEM, " ").make();
        }

        if (materialSlots.contains(slot)) {
            return item != null
                    ? item
                    : ItemCreator.of(CompMaterial.LIGHT_GRAY_STAINED_GLASS_PANE, Lang.of("Player_Market_Menu.manage_item.items.empty_slot.name"),
                    Lang.of("Player_Market_Menu.manage_item.items.empty_slot.lore"))
                    .make();
        }

        return super.getItemAt(slot);
    }

    @Override
    protected void onPageClick(Player player, Integer id, ClickType clickType) throws SQLException {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(database.getItemStack(id));
            database.deleteListing(id);
            restartMenu();
        }

    }

    @Override
    protected void onPageClick(Player player, Material material, ClickType clickType) {

    }


    @Override
    protected void onPostDisplay(Player viewer) {
        animateAsync(20, new MenuRunnable() {
            @Override
            public void run() {
                restartMenu();
            }
        });
    }

    @Override
    protected int getReturnButtonPosition() {
        return 49;
    }

    static {
        ButtonReturnBack.setMaterial(CompMaterial.ARROW);
        ButtonReturnBack.setTitle(Common.colorize("Return Back"));

        inactivePageButton = ItemCreator.of(Settings.ACCENT_COLOR_ITEM, " ").make();

    }


}