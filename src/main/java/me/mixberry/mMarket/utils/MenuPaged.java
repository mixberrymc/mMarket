//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.mixberry.mMarket.utils;

import lombok.Generated;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.InventoryDrawer;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import java.sql.SQLException;
import java.util.*;

public abstract class MenuPaged<T> extends Menu {
    public static ItemStack activePageButton;
    public static ItemStack inactivePageButton;

    static {

        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
        item.setItemMeta(meta);

        activePageButton = item;
        inactivePageButton = ItemCreator.of(CompMaterial.ARROW).make();
    }

    private final List<Integer> slots;
    private final Iterable<T> items;
    private final Integer manualPageSize;
    private final Map<Integer, List<T>> pages;
    @Getter
    public Menu parentMenu;
    private int currentPage;
    private Button nextButton;
    private Button prevButton;

    protected MenuPaged(T... items) {
        this(null, null, null, Arrays.asList(items), false);
        if (items == null) {
            throw new NullPointerException("items is marked non-null but is null");
        }
    }

    protected MenuPaged(Iterable<T> items) {
        this(null, null, null, items, false);
    }

    protected MenuPaged(int pageSize, List<Integer> slots, Iterable<T> items) {
        super(null);
        this.pages = new HashMap<>();
        this.currentPage = 1;

        this.setSize(pageSize);

        this.slots = (slots != null && !slots.isEmpty()) ? slots : new ArrayList<>();
        this.items = items;

        this.manualPageSize = pageSize;
        this.calculatePages();
        this.setButtons();
    }

    protected MenuPaged(Menu parent, T... items) {
        this(null, parent, null, Arrays.asList(items), false);
        if (items == null) {
            throw new NullPointerException("items is marked non-null but is null");
        }
    }

    protected MenuPaged(Menu parent, Iterable<T> items) {
        this(null, parent, null, items, false);
    }

    protected MenuPaged(List<Integer> slots, Iterable<T> items) {
        this(null, null, slots, items, false);
    }

    protected MenuPaged(Menu parent, List<Integer> slots, Iterable<T> items) {
        this(null, parent, slots, items, false);
    }

    protected MenuPaged(Menu parent, Iterable<T> items, boolean returnMakesNewInstance) {
        this(null, parent, null, items, returnMakesNewInstance);
    }

    protected MenuPaged(Menu parent, List<Integer> slots, Iterable<T> items, boolean returnMakesNewInstance) {
        this(null, parent, slots, items, returnMakesNewInstance);
    }

    protected MenuPaged(int pageSize, T... items) {
        this(pageSize, null, null, Arrays.asList(items), false);
        if (items == null) {
            throw new NullPointerException("items is marked non-null but is null");
        }
    }


    protected MenuPaged(int pageSize, Iterable<T> items) {
        this(pageSize, null, null, items, false);
    }

    protected MenuPaged(Menu parentMenu, int pageSize, List<Integer> slots, Iterable<T> items) {
        super(parentMenu); // Call super constructor without parent

        this.parentMenu = parentMenu;
        this.pages = new HashMap<>();
        this.currentPage = 1;

        // Instead of calculating size based on slots, force the menu size to 9 * 5
        this.setSize(pageSize);

        this.slots = (slots != null && !slots.isEmpty()) ? slots : new ArrayList<>();
        this.items = items;

        this.manualPageSize = pageSize;
        this.calculatePages();
        this.setButtons();
    }

    protected MenuPaged(int pageSize, Menu parent, T... items) {
        this(pageSize, parent, null, Arrays.asList(items), false);
        if (items == null) {
            throw new NullPointerException("items is marked non-null but is null");
        }
    }

    protected MenuPaged(int pageSize, Menu parent, Iterable<T> items) {
        this(pageSize, parent, null, items, false);
    }

    protected MenuPaged(int pageSize, Menu parent, Iterable<T> items, boolean returnMakesNewInstance) {
        this(pageSize, parent, null, items, returnMakesNewInstance);
    }

    private MenuPaged(Integer pageSize, Menu parent, List<Integer> slots, Iterable<T> items, boolean returnMakesNewInstance) {
        super(parent, returnMakesNewInstance);
        this.pages = new HashMap();
        this.currentPage = 1;
        this.slots = slots != null ? slots : new ArrayList();
        this.items = items;
        this.manualPageSize = pageSize;
        this.calculatePages();
        this.setButtons();
    }

    @Generated
    public static ItemStack getActivePageButton() {
        return activePageButton;
    }

    @Generated
    public static void setActivePageButton(ItemStack activePageButton) {
        MenuPaged.activePageButton = activePageButton;
    }

    @Generated
    public static ItemStack getInactivePageButton() {
        return inactivePageButton;
    }

    @Generated
    public static void setInactivePageButton(ItemStack inactivePageButton) {
        MenuPaged.inactivePageButton = inactivePageButton;
    }

    private void calculatePages() {
        int itemCount = this.getItemAmount(this.items);
        int availableSlots = this.slots.size();

        // Calculate pages based on available slots
        int autoPageSize = Math.min(availableSlots, manualPageSize != null ? manualPageSize : availableSlots);
        this.pages.clear();
        this.pages.putAll(Common.fillPages(autoPageSize, this.items));
    }

    private int getItemAmount(Iterable<T> pages) {
        int amount = 0;

        for (Iterator var3 = pages.iterator(); var3.hasNext(); ++amount) {
            T t = (T) var3.next();
        }

        return amount;
    }

    private void setButtons() {
        this.prevButton = this.canShowPreviousButton() ? this.formPreviousButton() : Button.makeEmpty();
        this.nextButton = this.canShowNextButton() ? this.formNextButton() : Button.makeEmpty();
    }

    protected boolean canShowPreviousButton() {
        return this.pages.size() >= 1;
    }

    protected boolean canShowNextButton() {
        return this.pages.size() >= 1;
    }

    public Button formPreviousButton() {
        return new Button() {
            final boolean canGo = MenuPaged.this.getCurrentPage() > 1;

            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (this.canGo) {
                    MenuPaged.this.setCurrentPage(MathUtil.range(MenuPaged.this.getCurrentPage() - 1, 1, MenuPaged.this.getPages().size()));
                }

            }

            public ItemStack getItem() {
                int previousPage = MenuPaged.this.getCurrentPage() - 1;
                return ItemCreator.of(this.canGo ? MenuPaged.getActivePageButton() : MenuPaged.getInactivePageButton()).name(previousPage == 0 ? org.mineacademy.fo.settings.SimpleLocalization.Menu.PAGE_FIRST : org.mineacademy.fo.settings.SimpleLocalization.Menu.PAGE_PREVIOUS.replace("{page}", String.valueOf(previousPage))).make();
            }
        };
    }

    public Button formNextButton() {
        return new Button() {
            final boolean canGo = MenuPaged.this.getCurrentPage() < MenuPaged.this.getPages().size();

            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (this.canGo) {
                    MenuPaged.this.setCurrentPage(MathUtil.range(MenuPaged.this.getCurrentPage() + 1, 1, MenuPaged.this.getPages().size()));
                }

            }

            public ItemStack getItem() {
                boolean lastPage = MenuPaged.this.getCurrentPage() == MenuPaged.this.getPages().size();
                return ItemCreator.of(this.canGo ? MenuPaged.getActivePageButton() : MenuPaged.getInactivePageButton()).name(lastPage ? org.mineacademy.fo.settings.SimpleLocalization.Menu.PAGE_LAST : org.mineacademy.fo.settings.SimpleLocalization.Menu.PAGE_NEXT.replace("{page}", String.valueOf(MenuPaged.this.getCurrentPage() + 1))).make();
            }
        };
    }

    private void updatePage() {
        this.setButtons();
        this.restartMenu();
        Menu.getSound().play(this.getViewer());
        PlayerUtil.updateInventoryTitle(this.getViewer(), this.getTitleWithPageNumbers());
    }

    public String getTitleWithPageNumbers() {
        boolean canAddNumbers = this.addPageNumbers() && this.pages.size() >= 1;
        return "&0" + this.getTitle() + (canAddNumbers ? " &8(" + this.currentPage + "/" + this.pages.size() + ")" : "");
    }

    protected final void onPreDisplay(InventoryDrawer drawer) {
        drawer.setTitle(this.getTitleWithPageNumbers());
        this.onPostDisplay(drawer);
    }

    final void onRestartInternal() {
        this.calculatePages();
    }

    protected void onPostDisplay(InventoryDrawer drawer) {
    }

    protected abstract ItemStack convertToItemStack(T var1) throws SQLException;

    protected abstract void onPageClick(Player var1, T var2, ClickType var3) throws SQLException;

    protected boolean addPageNumbers() {
        return true;
    }

    protected boolean isEmpty() {
        return this.pages.isEmpty() || this.pages.get(0).isEmpty();
    }

    public ItemStack getItemAt(int slot) {
        if (this.slots.contains(slot) && this.slots.indexOf(slot) < this.getCurrentPageItems().size()) {
            T object = this.getCurrentPageItems().get(this.slots.indexOf(slot));
            if (object != null) {
                try {
                    return this.convertToItemStack(object);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (slot == this.getPreviousButtonPosition()) {
            return this.prevButton.getItem();
        } else {
            return slot == this.getNextButtonPosition() ? this.nextButton.getItem() : null;
        }
    }

    protected int getPreviousButtonPosition() {
        return this.getSize() - 9;
    }

    protected abstract void onPageClick(Player player, Material material, ClickType clickType);

    protected int getNextButtonPosition() {
        return this.getSize() - 1;
    }

    public final void onMenuClick(Player player, int slot, InventoryAction action, ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (this.slots.contains(slot) && this.slots.indexOf(slot) < this.getCurrentPageItems().size()) {
            T obj = this.getCurrentPageItems().get(this.slots.indexOf(slot));
            if (obj != null) {
                InventoryType prevType = Remain.invokeOpenInventoryMethod(player, "getType");
                try {
                    this.onPageClick(player, obj, click);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (prevType == Remain.invokeOpenInventoryMethod(player, "getType")) {
                    Inventory topInventory = Remain.getTopInventoryFromOpenInventory(player);
                    topInventory.setItem(slot, this.getItemAt(slot));
                }
            }
        }

    }

    public final void animateTitle(String title) {
        if (Menu.isTitleAnimationEnabled()) {
            PlayerUtil.updateInventoryTitle(this, this.getViewer(), title, this.getTitleWithPageNumbers(), Menu.getTitleAnimationDurationTicks());
        }

    }

    public final void onButtonClick(Player player, int slot, InventoryAction action, ClickType click, Button button) {
        super.onButtonClick(player, slot, action, click, button);
    }

    public final void onMenuClick(Player player, int slot, ItemStack clicked) {
        throw new FoException("Simplest click unsupported");
    }

    private List<T> getCurrentPageItems() {
        Valid.checkBoolean(this.pages.containsKey(this.currentPage - 1), "The menu has only " + this.pages.size() + " pages, not " + this.currentPage + "!");
        return this.pages.get(this.currentPage - 1);
    }

    @Generated
    public List<Integer> getSlots() {
        return this.slots;
    }

    @Generated
    public Map<Integer, List<T>> getPages() {
        return this.pages;
    }

    @Generated
    public int getCurrentPage() {
        return this.currentPage;
    }

    protected void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        this.updatePage();
    }
}
