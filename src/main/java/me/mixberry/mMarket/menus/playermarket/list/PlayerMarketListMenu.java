package me.mixberry.mMarket.menus.playermarket.list;

import me.mixberry.mMarket.mMarket;
import me.mixberry.mMarket.menus.playermarket.manage.PlayerMarketManageMenu;
import me.mixberry.mMarket.utils.MenuPaged;
import me.mixberry.mMarket.utils.PlayerMarketUtils;
import me.mixberry.mMarket.utils.Settings;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.Lang;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerMarketListMenu extends MenuPaged<Integer> {

    public enum SortingType {
        NEWEST(Lang.of("Player_Market_Menu.sorting.prefix") + Lang.of("Player_Market_Menu.sorting.sorting_newest")),
        LOWEST(Lang.of("Player_Market_Menu.sorting.prefix") + Lang.of("Player_Market_Menu.sorting.sorting_lowest_price")),
        HIGHEST(Lang.of("Player_Market_Menu.sorting.prefix") + Lang.of("Player_Market_Menu.sorting.sorting_highest_price")),
        OLDEST(Lang.of("Player_Market_Menu.sorting.prefix") + Lang.of("Player_Market_Menu.sorting.sorting_oldest"));

        private final String display;

        SortingType(String display) { this.display = display; }

        public String display() { return display; }

        public SortingType next() {
            int i = (this.ordinal() + 1) % values().length;
            return values()[i];
        }
    }

    private static final ArrayList<Integer> materialSlots = new ArrayList<>();
    static {
        Integer[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };
        materialSlots.addAll(Arrays.asList(slots));
    }


    @Position(49)
    private Button search;

    @Position(50)
    private Button manage;

    // Reuse slot 48 for sorting
    @Position(48)
    private Button sorting;

    // keep current context so buttons can reopen correctly
    private final @Nullable String searchItem;
    private final SortingType sortingType;

    public PlayerMarketListMenu(Player player, @Nullable String searchItem) throws SQLException {
        this(player, searchItem, null);
    }

    public PlayerMarketListMenu(Player player, @Nullable String searchItem, @Nullable SortingType sortingType) throws SQLException {
        super(
                9 * 6,
                materialSlots,
                sortedIds(
                        (searchItem == null || searchItem.isEmpty())
                                ? mMarket.getMarketDatabase().getAll()
                                : mMarket.getMarketDatabase().searchIds(searchItem),
                        sortingType
                )
        );


        this.searchItem = (searchItem != null && searchItem.isEmpty()) ? null : searchItem;
        this.sortingType = sortingType == null ? SortingType.NEWEST : sortingType;

        setTitle(Lang.of("Player_Market_Menu.title"));
        setSound(new SimpleSound(Sound.ITEM_BOOK_PAGE_TURN, 1, 1));
        setViewer(player);

        // --- Sorting button ---
        this.sorting = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                try {
                    new PlayerMarketListMenu(player, PlayerMarketListMenu.this.searchItem, PlayerMarketListMenu.this.sortingType.next()).displayTo(player);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }


            @Override
            public ItemStack getItem() {

                String prefix = Lang.of("Player_Market_Menu.sorting.prefix");
                String newest = Lang.of("Player_Market_Menu.sorting.sorting_newest");
                String oldest = Lang.of("Player_Market_Menu.sorting.sorting_oldest");
                String lowest = Lang.of("Player_Market_Menu.sorting.sorting_lowest_price");
                String highest = Lang.of("Player_Market_Menu.sorting.sorting_highest_price");

                return ItemCreator.of(CompMaterial.HOPPER, Lang.of("Player_Market_Menu.sorting.title"))
                        .lore(
                                sortingType == SortingType.NEWEST ? Common.colorize("&f" + prefix + newest) : Common.colorize("&#9498a4" + prefix + newest),
                                sortingType == SortingType.LOWEST ? Common.colorize("&f" + prefix + lowest) : Common.colorize("&#9498a4" + prefix + lowest),
                                sortingType == SortingType.HIGHEST ? Common.colorize("&f" + prefix + highest) : Common.colorize("&#9498a4" + prefix + highest),
                                sortingType == SortingType.OLDEST ? Common.colorize("&f" + prefix + oldest) : Common.colorize("&#9498a4" + prefix + oldest)
                        ).make();
            }
        };

        // --- Search button (toggle search / reset) ---
        this.search = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
                if (PlayerMarketListMenu.this.searchItem == null) {
                    new PlayerMarketSearchMenu(player, PlayerMarketListMenu.class);
                } else {
                    try {
                        new PlayerMarketListMenu(player, null, PlayerMarketListMenu.this.sortingType).displayTo(player);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public ItemStack getItem() {
                if (PlayerMarketListMenu.this.searchItem == null) {
                    return ItemCreator.of(CompMaterial.SPYGLASS, Lang.of("Player_Market_Menu.search.default.name"), Lang.of("Player_Market_Menu.search.default.lore")).make();
                } else {
                    return ItemCreator.of(CompMaterial.BARRIER, Lang.of("Player_Market_Menu.search.reset.name"), Lang.of("Player_Market_Menu.search.reset.lore")).make();
                }
            }
        };

        this.manage = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
                try {
                    new PlayerMarketManageMenu(player);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.of(CompMaterial.BUNDLE, Common.colorize(Lang.of("Player_Market_Menu.manage_item.name")), Lang.of("Player_Market_Menu.manage_item.lore")).make();
            }
        };
    }

    // ---- Sorting core ----
    private static List<Integer> sortedIds(List<Integer> originalIds, @Nullable SortingType sorting) throws SQLException {
        final SortingType sort = (sorting == null) ? SortingType.NEWEST : sorting;

        // Prefetch values to avoid checked exceptions inside comparator
        Map<Integer, Long> created = new HashMap<>();
        Map<Integer, Double> prices = new HashMap<>();

        for (Integer id : originalIds) {
            // If any of these throws, bubble up as SQLException which constructor already declares
            created.put(id, mMarket.getMarketDatabase().getCreatedDate(id)); // epoch millis or similar
            prices.put(id, mMarket.getMarketDatabase().getPrice(id));        // assume double
        }

        Comparator<Integer> cmp;
        switch (sort) {
            case NEWEST:
                cmp = Comparator.<Integer, Long>comparing(created::get).reversed();
                break;
            case OLDEST:
                cmp = Comparator.comparing(created::get);
                break;
            case LOWEST:
                cmp = Comparator.<Integer, Double>comparing(prices::get)
                        .thenComparing(Comparator.comparing(created::get).reversed()); // tie-break by newest
                break;
            case HIGHEST:
                cmp = Comparator.<Integer, Double>comparing(prices::get).reversed()
                        .thenComparing(Comparator.comparing(created::get).reversed());
                break;
            default:
                cmp = Comparator.<Integer, Long>comparing(created::get).reversed();
        }

        return originalIds.stream().sorted(cmp).collect(Collectors.toList());
    }

    // ---- Paged content & clicks ----

    @Override
    protected ItemStack convertToItemStack(Integer id) throws SQLException {

        if (PlayerMarketUtils.isItemExpired(id)) {
            return null;
        }

        ItemStack stack = mMarket.getMarketDatabase().getItemStack(id);

        UUID sellerId = mMarket.getMarketDatabase().getSeller(id);
        String sellerName = null;
        if (sellerId != null) {
            var op = Remain.getOfflinePlayerByUUID(sellerId);
            sellerName = (op != null ? op.getName() : null);
        }
        String sellerDisplay = sellerName != null ? sellerName : (sellerId != null ? sellerId.toString() : "Unknown");

        return ItemCreator.of(stack)
                .lore(
                        Lang.of("Player_Market_Menu.market_item.lore")
                                .replace("{price}", Double.toString(mMarket.getMarketDatabase().getPrice(id)))
                                .replace("{seller}", sellerDisplay)
                                .replace("{date}", PlayerMarketUtils.getItemRemainTime(id)),
                        Lang.of("Player_Market_Menu.market_item.active_message")
        ).make();
    }

    @Override
    protected void onPageClick(Player player, Integer id, ClickType click) throws SQLException {
        new PlayerMarketReviewMenu(player, id).displayTo(player);
    }

    @Override
    public ItemStack getItemAt(int slot) {
        ItemStack item = super.getItemAt(slot);

        if (slot <= 44 && !materialSlots.contains(slot)) {
            return item != null
                    ? item
                    : ItemCreator.of(Settings.PRIMARY_COLOR_ITEM, " ").make();
        }

        if (slot >= 45 && item == null) {
            return ItemCreator.of(Settings.ACCENT_COLOR_ITEM, " ").make();
        }

        return super.getItemAt(slot);
    }

    @Override
    protected int getNextButtonPosition() { return 53; }

    @Override
    protected int getPreviousButtonPosition() { return 45; }

    @Override
    protected void onPageClick(Player player, Material material, ClickType clickType) {
    }

    @Override
    protected boolean addReturnButton() { return false; }

    private void resetSearch(Player player) {
        player.getInventory().close();
        restartMenu();
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

}
