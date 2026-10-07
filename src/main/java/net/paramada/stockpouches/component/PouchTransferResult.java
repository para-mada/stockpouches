package net.paramada.stockpouches.component;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Result of a pure, atomic transfer between a single-type pouch and one stack. */
public record PouchTransferResult(
        SingleTypePouchContents contents,
        ItemStack otherStack,
        Outcome outcome
) {

    public PouchTransferResult {
        Objects.requireNonNull(contents);
        otherStack = Objects.requireNonNull(otherStack).copy();
        Objects.requireNonNull(outcome);
    }

    @Override
    public ItemStack otherStack() {
        return otherStack.copy();
    }

    public boolean moved() {
        return outcome == Outcome.MOVED;
    }

    public enum Outcome {
        MOVED,
        EMPTY_SOURCE,
        EMPTY_POUCH,
        REJECTED_ITEM,
        INCOMPATIBLE_ITEM,
        POUCH_FULL,
        DESTINATION_FULL,
        INVALID_CONTENTS
    }
}
