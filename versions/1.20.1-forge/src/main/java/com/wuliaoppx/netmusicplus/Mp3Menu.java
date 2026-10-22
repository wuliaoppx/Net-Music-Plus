package com.wuliaoppx.netmusicplus;

import com.wuliaoppx.netmusicplus.compat.CuriosCompat;
import com.wuliaoppx.netmusicplus.net.Mp3OpenData;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The MP3 player's disc slots, its storage-expansion slot and the mode / output / transport buttons.
 * <p>
 * The slots are a {@link SimpleContainer} view over the {@link Mp3State} stored in the item's NBT: every change is
 * written back to the item stack, so the discs live in the stack itself (and therefore in the inventory, a curio slot
 * or a shulker box) instead of in a block entity.
 * <p>
 * Installing or removing a storage expansion changes the slot count, which a live menu cannot do, so the menu re-opens
 * itself one tick later. The capacity therefore has to travel in the open packet instead of being read from the item:
 * the client's copy of the item is synced by a different packet and may still be the old one, which would leave the
 * two sides with a different number of slots and kick the player with a protocol error.
 */
public class Mp3Menu extends AbstractContainerMenu {
    public static final int BUTTON_MODE = 0;
    public static final int BUTTON_OUTPUT = 1;
    public static final int BUTTON_PREV = 2;
    public static final int BUTTON_NEXT = 3;
    public static final int BUTTON_PLAY = 4;
    /** Takes one installed storage expansion back out of the player. */
    public static final int BUTTON_REMOVE_EXPANSION = 5;

    public static final int COLUMNS = 10;
    public static final int PANEL_WIDTH = 196;
    /** x of the storage expansion slot, below the disc grid. */
    public static final int UPGRADE_X = 8;
    public static final int GRID_Y = 20;

    private static final int DATA_MODE = 0;
    private static final int DATA_EXTERNAL = 1;
    private static final int DATA_INDEX = 2;
    private static final int DATA_PLAYING = 3;
    private static final int DATA_UPGRADES = 4;

    private final SimpleContainer discs;
    private final SimpleContainer upgrade;
    private final ItemStack playerStack;
    private final int playerSlot;
    private final int capacity;
    private final Player owner;
    private final int[] data = new int[5];
    private boolean loading;
    private boolean consuming;

    /** Server side: the data is already known, so the menu is built from it directly. */
    public Mp3Menu(int containerId, Inventory inventory, int playerSlot, int capacity) {
        this(containerId, inventory, playerSlot, capacity, inventory.player);
    }

    /** Client side: the opening packet carries the same two values through an {@link ExtendedScreenHandlerType}. */
    public Mp3Menu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, Mp3OpenData.read(data));
    }

    private Mp3Menu(int containerId, Inventory inventory, Mp3OpenData data) {
        this(containerId, inventory, data.playerSlot(), data.capacity(), inventory.player);
    }

    private Mp3Menu(int containerId, Inventory inventory, int playerSlot, int capacity, Player owner) {
        super(NetMusicPlus.MP3_MENU.get(), containerId);
        this.playerSlot = playerSlot;
        this.owner = owner;
        this.playerStack = playerSlot >= 0
                ? inventory.getItem(playerSlot)
                : CuriosCompat.findEquipped(inventory.player);
        this.capacity = Math.max(Mp3State.BASE_DISCS, Math.min(capacity, Mp3State.MAX_DISCS));

        Mp3State state = Mp3Support.state(this.playerStack);
        this.discs = new SimpleContainer(this.capacity) {
            @Override
            public void setChanged() {
                super.setChanged();
                Mp3Menu.this.saveDiscs();
            }
        };
        this.upgrade = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                Mp3Menu.this.consumeUpgrade();
            }
        };
        this.loading = true;
        for (int i = 0; i < this.capacity && i < state.discs().size(); i++) {
            ItemStack disc = state.discs().get(i);
            this.discs.setItem(i, disc.isEmpty() ? ItemStack.EMPTY : disc.copyWithCount(1));
        }
        this.loading = false;

        int rows = rows(this.capacity);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = row * COLUMNS + column;
                if (index >= this.capacity) {
                    continue;
                }
                this.addSlot(new Slot(this.discs, index, 8 + column * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return Mp3Support.isDisc(stack);
                    }

                    /** One disc per slot, no stacks. */
                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }

        this.addSlot(new Slot(this.upgrade, 0, UPGRADE_X, upgradeY(this.capacity)) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(NetMusicPlus.EXPANDER.get())
                        && Mp3Support.state(Mp3Menu.this.playerStack).upgrades() < Mp3State.MAX_UPGRADES;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        int inventoryY = inventoryY(this.capacity);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 12 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, 12 + column * 18, inventoryY + 58));
        }

        this.data[DATA_MODE] = state.mode().ordinal();
        this.data[DATA_EXTERNAL] = state.external() ? 1 : 0;
        this.data[DATA_INDEX] = state.index();
        this.data[DATA_PLAYING] = state.playing() ? 1 : 0;
        this.data[DATA_UPGRADES] = state.upgrades();
        this.addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                return Mp3Menu.this.data[index];
            }

            @Override
            public void set(int index, int value) {
                Mp3Menu.this.data[index] = value;
            }

            @Override
            public int getCount() {
                return Mp3Menu.this.data.length;
            }
        });
    }

    /** Height of the whole panel for a given disc capacity; the screen uses the same numbers. */
    public static int imageHeight(int capacity) {
        return inventoryY(capacity) + 58 + 18 + 8;
    }

    public static int inventoryY(int capacity) {
        return buttonsY2(capacity) + 28;
    }

    public static int buttonsY1(int capacity) {
        return upgradeY(capacity) + 24;
    }

    public static int buttonsY2(int capacity) {
        return buttonsY1(capacity) + 24;
    }

    public static int gridBottom(int capacity) {
        return GRID_Y + rows(capacity) * 18;
    }

    public static int upgradeY(int capacity) {
        return gridBottom(capacity) + 6;
    }

    private static int rows(int capacity) {
        return (capacity + COLUMNS - 1) / COLUMNS;
    }

    /** Opens the disc tray for the MP3 player the player carries (curio slot first, then the inventory). */
    public static void open(ServerPlayer player) {
        ItemStack stack = Mp3Support.findActive(player);
        if (stack.isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                slot = i;
                break;
            }
        }
        if (slot < 0 && CuriosCompat.findEquipped(player) != stack) {
            return;
        }
        final int playerSlot = slot;
        final int capacity = Mp3Support.state(stack).capacity();
        final Component title = stack.getHoverName();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, inv, p) -> new Mp3Menu(containerId, inv, playerSlot, capacity), title), buf ->
                new Mp3OpenData(playerSlot, capacity).write(buf));
    }

    public int capacity() {
        return this.capacity;
    }

    public int imageHeight() {
        return imageHeight(this.capacity);
    }

    private boolean client() {
        return this.owner.level().isClientSide();
    }

    private void saveDiscs() {
        if (this.loading || this.client() || this.playerStack.isEmpty()) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>(this.capacity);
        for (int i = 0; i < this.capacity; i++) {
            ItemStack stack = this.discs.getItem(i);
            if (stack.isEmpty()) {
                stacks.add(ItemStack.EMPTY);
                continue;
            }
            // The vanilla merge path in AbstractContainerMenu#moveItemStackTo ignores Slot#getMaxStackSize, so an
            // identical disc can still end up stacked in here; keep one and hand the extra back to the player.
            if (stack.getCount() > 1) {
                ItemStack overflow = stack.copyWithCount(stack.getCount() - 1);
                stack.setCount(1);
                if (!this.owner.getInventory().add(overflow)) {
                    this.owner.drop(overflow, false);
                }
            }
            stacks.add(stack.copy());
        }
        Mp3State state = Mp3Support.state(this.playerStack);
        Mp3Support.setState(this.playerStack, state.withDiscs(stacks));
    }

    /**
     * A storage expansion was dropped into its slot: consume one item, remember it and re-open the menu so the extra
     * slots actually exist. A live menu cannot grow, hence the re-open.
     */
    private void consumeUpgrade() {
        if (this.loading || this.consuming || this.client() || this.playerStack.isEmpty()) {
            return;
        }
        ItemStack stack = this.upgrade.getItem(0);
        if (stack.isEmpty() || !stack.is(NetMusicPlus.EXPANDER.get())) {
            return;
        }
        Mp3State state = Mp3Support.state(this.playerStack);
        if (state.upgrades() >= Mp3State.MAX_UPGRADES) {
            return;
        }
        this.consuming = true;
        ItemStack leftover = stack.copy();
        leftover.shrink(1);
        this.upgrade.setItem(0, leftover);
        Mp3Support.setState(this.playerStack, state.withUpgrades(state.upgrades() + 1));
        this.data[DATA_UPGRADES] = state.upgrades() + 1;
        this.consuming = false;
        this.reopen();
    }

    /** Takes one installed expansion back out; refused while the ten slots it added still hold discs. */
    private void removeUpgrade(Player player) {
        if (this.client() || this.playerStack.isEmpty()) {
            return;
        }
        Mp3State state = Mp3Support.state(this.playerStack);
        if (state.upgrades() <= 0) {
            return;
        }
        int first = Math.max(0, this.capacity - Mp3State.DISCS_PER_UPGRADE);
        for (int i = first; i < this.capacity; i++) {
            if (!this.discs.getItem(i).isEmpty()) {
                return;
            }
        }
        Mp3Support.setState(this.playerStack, state.withUpgrades(state.upgrades() - 1));
        this.data[DATA_UPGRADES] = state.upgrades() - 1;
        ItemStack expansion = new ItemStack(NetMusicPlus.EXPANDER.get());
        if (!player.getInventory().add(expansion)) {
            player.drop(expansion, false);
        }
        this.reopen();
    }

    private void reopen() {
        if (this.owner instanceof ServerPlayer serverPlayer && serverPlayer.level().getServer() != null) {
            serverPlayer.level().getServer().execute(() -> {
                if (serverPlayer.containerMenu == this) {
                    open(serverPlayer);
                }
            });
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.discs) {
            this.saveDiscs();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Anything left in the expansion slot goes back to the player instead of vanishing with the menu.
        ItemStack leftover = this.upgrade.getItem(0);
        if (!leftover.isEmpty()) {
            if (!this.client()) {
                if (!player.getInventory().add(leftover)) {
                    player.drop(leftover, false);
                }
            }
            this.upgrade.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.playerStack.isEmpty()) {
            return false;
        }
        if (id == BUTTON_REMOVE_EXPANSION) {
            removeUpgrade(player);
            return true;
        }
        Mp3State state = Mp3Support.state(this.playerStack);
        switch (id) {
            case BUTTON_MODE -> state = state.withMode(state.mode().cycle());
            case BUTTON_OUTPUT -> state = state.withExternal(!state.external());
            case BUTTON_PREV -> state = state.withIndex(state.nextIndex(-1, false, player.getRandom()));
            case BUTTON_NEXT -> state = state.withIndex(state.nextIndex(1, false, player.getRandom()));
            case BUTTON_PLAY -> state = state.withPlaying(!state.playing());
            default -> {
                return false;
            }
        }
        Mp3Support.setState(this.playerStack, state);
        this.data[DATA_MODE] = state.mode().ordinal();
        this.data[DATA_EXTERNAL] = state.external() ? 1 : 0;
        this.data[DATA_INDEX] = state.index();
        this.data[DATA_PLAYING] = state.playing() ? 1 : 0;
        this.broadcastChanges();
        return true;
    }

    public PlayMode mode() {
        return PlayMode.values()[Math.floorMod(this.data[DATA_MODE], PlayMode.values().length)];
    }

    public boolean external() {
        return this.data[DATA_EXTERNAL] != 0;
    }

    public boolean playing() {
        return this.data[DATA_PLAYING] != 0;
    }

    public int selectedIndex() {
        return this.data[DATA_INDEX];
    }

    public int upgrades() {
        return this.data[DATA_UPGRADES];
    }

    public ItemStack selectedDisc() {
        return this.discs.getItem(Math.max(0, Math.min(this.selectedIndex(), this.capacity - 1)));
    }

    public int filledSlots() {
        int filled = 0;
        for (int i = 0; i < this.capacity; i++) {
            if (!this.discs.getItem(i).isEmpty()) {
                filled++;
            }
        }
        return filled;
    }

    /** Re-reads the item's state into the synced data slots (used when the keybind changes something). */
    public void refresh() {
        Mp3State state = Mp3Support.state(this.playerStack);
        this.data[DATA_MODE] = state.mode().ordinal();
        this.data[DATA_EXTERNAL] = state.external() ? 1 : 0;
        this.data[DATA_INDEX] = state.index();
        this.data[DATA_PLAYING] = state.playing() ? 1 : 0;
        this.data[DATA_UPGRADES] = state.upgrades();
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null) {
            return ItemStack.EMPTY;
        }
        int containerEnd = this.capacity + 1;
        if (index == this.capacity && !slot.hasItem()) {
            // Shift-click on the empty expansion slot takes one installed expansion back out.
            removeUpgrade(player);
            return ItemStack.EMPTY;
        }
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < containerEnd) {
            if (!this.moveItemStackTo(stack, containerEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(NetMusicPlus.EXPANDER.get())
                && Mp3Support.state(this.playerStack).upgrades() < Mp3State.MAX_UPGRADES
                && this.upgrade.getItem(0).isEmpty()) {
            if (!this.moveItemStackTo(stack, this.capacity, containerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.insertOneDisc(stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    /**
     * Moves a single disc into the first empty tray slot. Deliberately not {@code moveItemStackTo}: its merge branch
     * only looks at {@code ItemStack#getMaxStackSize} and would fill one slot with a whole stack of discs.
     */
    private boolean insertOneDisc(ItemStack stack) {
        if (!Mp3Support.isDisc(stack)) {
            return false;
        }
        for (int i = 0; i < this.capacity; i++) {
            if (this.discs.getItem(i).isEmpty()) {
                this.discs.setItem(i, stack.copyWithCount(1));
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.playerStack.isEmpty() || !this.playerStack.is(NetMusicPlus.MP3.get())) {
            return false;
        }
        if (this.playerSlot >= 0) {
            return player.getInventory().getItem(this.playerSlot) == this.playerStack;
        }
        return CuriosCompat.findEquipped(player) == this.playerStack;
    }
}
