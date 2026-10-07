package net.paramada.stockpouches.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.paramada.stockpouches.component.ModDataComponents;
import net.paramada.stockpouches.component.PouchTransactions;
import net.paramada.stockpouches.component.PouchTransferResult;
import net.paramada.stockpouches.component.SingleTypePouchContents;

import java.util.function.Predicate;

/** Shared click transactions for bounded pouches that store one exact variant. */
public abstract class SingleTypePouchItem extends Item {

    private final int capacity;
    private final Predicate<ItemStack> acceptedItems;

    protected SingleTypePouchItem(Properties properties, int capacity, Predicate<ItemStack> acceptedItems) {
        super(properties);
        if (capacity <= 0) {
            throw new IllegalArgumentException("Pouch capacity must be positive");
        }
        this.capacity = capacity;
        this.acceptedItems = acceptedItems;
    }

    public final int capacity() {
        return capacity;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) {
            return action == ClickAction.PRIMARY || action == ClickAction.SECONDARY;
        }

        ItemStack inSlot = slot.getItem();
        if (action == ClickAction.PRIMARY && !inSlot.isEmpty()) {
            PouchTransferResult result = PouchTransactions.insertOne(
                    contentsOf(pouch), inSlot, capacity, this::canStore
            );
            if (!result.moved()) {
                playInsertFailSound(player);
                return true;
            }

            slot.setByPlayer(result.otherStack());
            setContents(pouch, result.contents());
            playInsertSound(player);
            broadcastInventoryChanges(player);
            return true;
        }

        if (action == ClickAction.SECONDARY) {
            SingleTypePouchContents contents = contentsOf(pouch);
            ItemStack extracted = contents.createStack(1);
            if (extracted.isEmpty() || !slot.mayPlace(extracted)) {
                return true;
            }

            PouchTransferResult result = PouchTransactions.extractOne(
                    contents,
                    inSlot,
                    capacity,
                    slot.getMaxStackSize(extracted)
            );
            if (!result.moved()) {
                return true;
            }

            slot.setByPlayer(result.otherStack());
            setContents(pouch, result.contents());
            playRemoveSound(player);
            broadcastInventoryChanges(player);
            return true;
        }

        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack pouch,
            ItemStack cursorStack,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess cursorAccess
    ) {
        if (!slot.allowModification(player)) {
            return action == ClickAction.PRIMARY || action == ClickAction.SECONDARY;
        }

        if (action == ClickAction.PRIMARY && !cursorStack.isEmpty()) {
            PouchTransferResult result = PouchTransactions.insertOne(
                    contentsOf(pouch), cursorStack, capacity, this::canStore
            );
            if (!result.moved() || !cursorAccess.set(result.otherStack())) {
                playInsertFailSound(player);
                return true;
            }

            setContents(pouch, result.contents());
            playInsertSound(player);
            broadcastInventoryChanges(player);
            return true;
        }

        if (action == ClickAction.SECONDARY) {
            PouchTransferResult result = PouchTransactions.extractOne(
                    contentsOf(pouch), cursorStack, capacity, cursorLimit(cursorStack, contentsOf(pouch))
            );
            if (!result.moved() || !cursorAccess.set(result.otherStack())) {
                return true;
            }

            setContents(pouch, result.contents());
            playRemoveSound(player);
            broadcastInventoryChanges(player);
            return true;
        }

        return false;
    }

    @Override
    public final boolean canFitInsideContainerItems() {
        return false;
    }

    protected final SingleTypePouchContents contentsOf(ItemStack pouch) {
        return pouch.getOrDefault(ModDataComponents.SINGLE_TYPE_POUCH_CONTENTS, SingleTypePouchContents.EMPTY);
    }

    protected final void setContents(ItemStack pouch, SingleTypePouchContents contents) {
        pouch.set(ModDataComponents.SINGLE_TYPE_POUCH_CONTENTS, contents);
    }

    private boolean canStore(ItemStack candidate) {
        return !(candidate.getItem() instanceof SingleTypePouchItem)
                && !(candidate.getItem() instanceof StockPouchItem)
                && candidate.getItem().canFitInsideContainerItems()
                && acceptedItems.test(candidate);
    }

    private static int cursorLimit(ItemStack cursorStack, SingleTypePouchContents contents) {
        if (!cursorStack.isEmpty()) {
            return cursorStack.getMaxStackSize();
        }
        ItemStack extracted = contents.createStack(1);
        return extracted.isEmpty() ? 0 : extracted.getMaxStackSize();
    }

    protected static void broadcastInventoryChanges(Player player) {
        if (player.containerMenu != null) {
            player.containerMenu.slotsChanged(player.getInventory());
        }
    }

    private static void playInsertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    protected static void playRemoveSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }
}
