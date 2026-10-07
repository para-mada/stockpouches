package net.paramada.stockpouches.component;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure planner for replacing the offhand from a Totem Pouch without partial moves. */
public final class TotemSwapTransactions {

    private TotemSwapTransactions() {}

    public static TotemSwapResult extractToOffhand(
            SingleTypePouchContents contents,
            ItemStack offhand,
            List<ItemStack> inventory,
            int excludedSlot,
            int inventoryStackLimit,
            int pouchCapacity
    ) {
        Objects.requireNonNull(contents);
        Objects.requireNonNull(offhand);
        Objects.requireNonNull(inventory);
        inventory.forEach(Objects::requireNonNull);

        PouchTransferResult extraction = PouchTransactions.extractOne(
                contents,
                ItemStack.EMPTY,
                pouchCapacity,
                1
        );
        if (!extraction.moved()) {
            TotemSwapResult.Outcome outcome = extraction.outcome() == PouchTransferResult.Outcome.EMPTY_POUCH
                    ? TotemSwapResult.Outcome.EMPTY_POUCH
                    : TotemSwapResult.Outcome.INVALID_CONTENTS;
            return failed(contents, offhand, outcome);
        }

        ItemStack extracted = extraction.otherStack();
        if (!extracted.is(Items.TOTEM_OF_UNDYING) || extracted.getCount() != 1) {
            return failed(contents, offhand, TotemSwapResult.Outcome.INVALID_CONTENTS);
        }

        Map<Integer, ItemStack> updatedSlots = new LinkedHashMap<>();
        int remaining = offhand.getCount();

        if (remaining > 0) {
            if (inventoryStackLimit <= 0) {
                return failed(contents, offhand, TotemSwapResult.Outcome.INVENTORY_FULL);
            }

            for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
                if (slot == excludedSlot) {
                    continue;
                }
                ItemStack target = inventory.get(slot);
                if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, offhand)) {
                    continue;
                }

                int limit = Math.min(inventoryStackLimit, target.getMaxStackSize());
                int moved = Math.min(remaining, Math.max(0, limit - target.getCount()));
                if (moved > 0) {
                    ItemStack updated = target.copy();
                    updated.grow(moved);
                    updatedSlots.put(slot, updated);
                    remaining -= moved;
                }
            }

            int perEmptySlot = Math.min(inventoryStackLimit, offhand.getMaxStackSize());
            for (int slot = 0; slot < inventory.size() && remaining > 0 && perEmptySlot > 0; slot++) {
                if (slot == excludedSlot || !inventory.get(slot).isEmpty()) {
                    continue;
                }

                int moved = Math.min(remaining, perEmptySlot);
                updatedSlots.put(slot, offhand.copyWithCount(moved));
                remaining -= moved;
            }
        }

        if (remaining > 0) {
            return failed(contents, offhand, TotemSwapResult.Outcome.INVENTORY_FULL);
        }

        List<TotemSwapResult.SlotChange> changes = new ArrayList<>(updatedSlots.size());
        updatedSlots.forEach((slot, stack) -> changes.add(new TotemSwapResult.SlotChange(slot, stack)));
        return new TotemSwapResult(
                extraction.contents(),
                extracted,
                changes,
                TotemSwapResult.Outcome.MOVED
        );
    }

    private static TotemSwapResult failed(
            SingleTypePouchContents contents,
            ItemStack offhand,
            TotemSwapResult.Outcome outcome
    ) {
        return new TotemSwapResult(contents, offhand, List.of(), outcome);
    }
}
