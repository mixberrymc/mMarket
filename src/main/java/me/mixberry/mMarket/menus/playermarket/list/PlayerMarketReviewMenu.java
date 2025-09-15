package me.mixberry.mMarket.menus.playermarket.list;

import me.mixberry.mMarket.mMarket;
import me.mixberry.mMarket.data.MarketDatabase;
import me.mixberry.mMarket.utils.PlayerMarketUtils;
import me.mixberry.mMarket.utils.Settings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonReturnBack;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.Lang;

import java.sql.SQLException;

public final class PlayerMarketReviewMenu extends Menu {

    MarketDatabase database = mMarket.getMarketDatabase();


    @Position(13)
    Button item;

    @Position(31)
    Button confirm;

    public PlayerMarketReviewMenu(Player player, int id) throws SQLException {
        super(new PlayerMarketListMenu(player, null));
        setTitle("Player Market > Review Order");
        setViewer(player);
        setSize(9 * 6);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

        this.item = new Button() {

            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
            }

            @Override
            public ItemStack getItem() {
                try {
                    return ItemCreator.of(database.getItemStack(id)).make();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        this.confirm = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {

                player.closeInventory();


                try {
                    if (HookManager.getBalance(player) >= database.getPrice(id)) {

                        if (PlayerMarketUtils.isItemExpired(id)) {
                            player.closeInventory();
                            player.sendMessage(Common.colorize(Lang.of("item_expired")));
                            return;
                        }

                        if (player.getInventory().firstEmpty() <= -1) {
                            player.closeInventory();
                            player.sendMessage(Common.colorize(Lang.of("inventory_full")));
                            return;
                        }

                        ItemStack stack = database.getItemStack(id);

                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1F, 2F);
                        player.playSound(player, Sound.ENTITY_VILLAGER_YES, 1F, 2F);
                        player.sendMessage(Common.colorize(Lang.of("purchase_success")
                                .replace("{itemName}", stack.getItemMeta().getDisplayName().isEmpty() ? stack.getType().name() : stack.getItemMeta().getDisplayName()))
                                .replace("{itemAmount}", Integer.toString(stack.getAmount()))
                                .replace("{price}", Double.toString(database.getPrice(id)))
                        );

                        player.getInventory().addItem(database.getItemStack(id));

                        HookManager.withdraw(player, database.getPrice(id));

                        Player seller = Bukkit.getPlayer(database.getSeller(id));
                        if (seller != null && seller.isOnline()) {
                            seller.sendMessage(Common.colorize(Lang.of("sold_message").replace("{price}", Double.toString(database.getPrice(id)))));
                            HookManager.deposit(seller, database.getPrice(id));
                        } else {
                            database.addPendingPayout(database.getSeller(id), database.getPrice(id));
                        }

                        database.deleteListing(id);

                    } else {
                        player.sendMessage(Common.colorize(Lang.of("not_enough_money")));
                        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 0.5F);
                    }


                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }


            }

            @Override
            public ItemStack getItem() {

                try {
                    if (HookManager.getBalance(player) >= database.getPrice(id)) {
                        return ItemCreator.of(CompMaterial.RAW_GOLD,
                                Lang.of("Player_Market_Menu.confirm_purchase.confirm_order.name"),
                                Lang.of("Player_Market_Menu.confirm_purchase.confirm_order.lore")).make();

                    } else {
                        return ItemCreator.of(CompMaterial.POISONOUS_POTATO,
                                Lang.of("Player_Market_Menu.confirm_purchase.not_enough_money.name"),
                                Lang.of("Player_Market_Menu.confirm_purchase.not_enough_money.lore")).make();
                    }

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

            }
        };

    }

    @Override
    public ItemStack getItemAt(int slot) {
        return ItemCreator.of(Settings.SECONDARY_COLOR_ITEM, " ").make();
    }



    @Override
    protected int getReturnButtonPosition() {
        return 49;
    }

    static {
        ButtonReturnBack.setMaterial(CompMaterial.ARROW);
        ButtonReturnBack.setTitle(Common.colorize("Return Back"));
    }
}
