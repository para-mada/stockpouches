package net.paramada.stockpouches.component;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

/** Pure transfer operations. Inputs are never mutated and failures return unchanged copies. */
public final class PouchTransactions {

    private PouchTransactions() {}

    public static PouchTransferResult insertOne(
            SingleTypePouchContents contents,
            ItemStack source,
            int capacity,
            Predicate<ItemStack> accepts
    ) {
        Objects.requireNonNull(contents);
        Objects.requireNonNull(source);
        Objects.requireNonNull(accepts);

        if (!contents.isValidFor(capacity)) {
            return failed(contents, source, PouchTransferResult.Outcome.INVALID_CONTENTS);
        }
        if (source.isEmpty()) {
            return failed(contents, source, PouchTransferResult.Outcome.EMPTY_SOURCE);
        }
        if (!source.getItem().canFitInsideContainerItems() || !accepts.test(source)) {
            return failed(contents, source, PouchTransferResult.Outcome.REJECTED_ITEM);
        }
        if (contents.amount() >= capacity) {
            return failed(contents, source, PouchTransferResult.Outcome.POUCH_FULL);
        }
        if (!contents.isEmpty() && !contents.matches(source)) {
            return failed(contents, source, PouchTransferResult.Outcome.INCOMPATIBLE_ITEM);
        }

        ItemStack remainder = source.copy();
        remainder.shrink(1);
        return new PouchTransferResult(contents.insertOne(source), remainder, PouchTransferResult.Outcome.MOVED);
    }

    public static PouchTransferResult extractOne(
            SingleTypePouchContents contents,
            ItemStack destination,
            int capacity,
            int destinationLimit
    ) {
        Objects.requireNonNull(contents);
        Objects.requireNonNull(destination);

        if (!contents.isValidFor(capacity)) {
            return failed(contents, destination, PouchTransferResult.Outcome.INVALID_CONTENTS);
        }
        if (contents.isEmpty()) {
            return failed(contents, destination, PouchTransferResult.Outcome.EMPTY_POUCH);
        }

        ItemStack extracted = contents.createStack(1);
        int legalLimit = Math.min(destinationLimit, extracted.getMaxStackSize());
        if (legalLimit <= 0) {
            return failed(contents, destination, PouchTransferResult.Outcome.DESTINATION_FULL);
        }

        ItemStack updatedDestination;
        if (destination.isEmpty()) {
            updatedDestination = extracted;
        } else {
            if (!ItemStack.isSameItemSameComponents(destination, extracted)) {
                return failed(contents, destination, PouchTransferResult.Outcome.INCOMPATIBLE_ITEM);
            }
            if (destination.getCount() >= legalLimit) {
                return failed(contents, destination, PouchTransferResult.Outcome.DESTINATION_FULL);
            }
            updatedDestination = destination.copy();
            updatedDestination.grow(1);
        }

        return new PouchTransferResult(
                contents.extractOne(),
                updatedDestination,
                PouchTransferResult.Outcome.MOVED
        );
    }

    private static PouchTransferResult failed(
            SingleTypePouchContents contents,
            ItemStack other,
            PouchTransferResult.Outcome outcome
    ) {
        return new PouchTransferResult(contents, other, outcome);
    }
}
