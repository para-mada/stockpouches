package net.paramada.stockpouches.component;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Result of planning an atomic Totem Pouch extraction into the offhand. */
public record TotemSwapResult(
        SingleTypePouchContents contents,
        ItemStack offhand,
        List<SlotChange> inventoryChanges,
        Outcome outcome
) {

    public TotemSwapResult {
        Objects.requireNonNull(contents);
        offhand = Objects.requireNonNull(offhand).copy();
        inventoryChanges = List.copyOf(Objects.requireNonNull(inventoryChanges));
        Objects.requireNonNull(outcome);
    }

    @Override
    public ItemStack offhand() {
        return offhand.copy();
    }

    public boolean moved() {
        return outcome == Outcome.MOVED;
    }

    public record SlotChange(int slot, ItemStack stack) {

        public SlotChange {
            if (slot < 0) {
                throw new IllegalArgumentException("Inventory slot cannot be negative");
            }
            stack = Objects.requireNonNull(stack).copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    public enum Outcome {
        MOVED,
        EMPTY_POUCH,
        INVENTORY_FULL,
        INVALID_CONTENTS
    }
}
