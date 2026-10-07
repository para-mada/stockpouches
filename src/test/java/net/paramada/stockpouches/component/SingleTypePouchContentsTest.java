package net.paramada.stockpouches.component;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleTypePouchContentsTest {

    private static final int CAPACITY = 9;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void codecRoundTripPreservesExactPrototypeAndAmount() {
        ItemStack namedPotion = new ItemStack(Items.POTION);
        namedPotion.set(DataComponents.CUSTOM_NAME, Component.literal("Expedition dose"));
        SingleTypePouchContents original = SingleTypePouchContents.of(namedPotion, 7);

        var encoded = SingleTypePouchContents.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        SingleTypePouchContents decoded = SingleTypePouchContents.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertEquals(SingleTypePouchContents.CURRENT_VERSION, decoded.version());
        assertEquals(1, decoded.prototype().orElseThrow().count());
        assertTrue(ItemStack.isSameItemSameComponents(namedPotion, decoded.createStack(1)));
    }

    @Test
    void codecRejectsStructurallyInvalidAmountsWithoutConstructingContents() {
        var result = SingleTypePouchContents.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"version\":1,\"amount\":-1}")
        );

        assertTrue(result.isError());
    }

    @Test
    void insertionMovesExactlyOneAndDoesNotMutateItsInput() {
        ItemStack source = new ItemStack(Items.COBBLESTONE, 2);

        PouchTransferResult result = PouchTransactions.insertOne(
                SingleTypePouchContents.EMPTY, source, CAPACITY, stack -> stack.is(Items.COBBLESTONE)
        );

        assertTrue(result.moved());
        assertEquals(1, result.contents().amount());
        assertEquals(1, result.otherStack().getCount());
        assertEquals(2, source.getCount());
    }

    @Test
    void insertionHonorsCapacityBoundaryAndRejectsOverCapacityData() {
        ItemStack source = new ItemStack(Items.COBBLESTONE, 3);
        SingleTypePouchContents oneBelow = SingleTypePouchContents.of(source, CAPACITY - 1);

        PouchTransferResult fills = PouchTransactions.insertOne(oneBelow, source, CAPACITY, stack -> true);
        PouchTransferResult full = PouchTransactions.insertOne(fills.contents(), source, CAPACITY, stack -> true);
        PouchTransferResult invalid = PouchTransactions.insertOne(
                SingleTypePouchContents.of(source, CAPACITY + 1), source, CAPACITY, stack -> true
        );

        assertTrue(fills.moved());
        assertEquals(CAPACITY, fills.contents().amount());
        assertEquals(PouchTransferResult.Outcome.POUCH_FULL, full.outcome());
        assertEquals(CAPACITY, full.contents().amount());
        assertEquals(3, full.otherStack().getCount());
        assertEquals(PouchTransferResult.Outcome.INVALID_CONTENTS, invalid.outcome());
        assertEquals(CAPACITY + 1, invalid.contents().amount());
        assertEquals(3, invalid.otherStack().getCount());
    }

    @Test
    void componentDifferencesAreIncompatibleAndLeaveBothSidesUntouched() {
        ItemStack first = new ItemStack(Items.POTION);
        first.set(DataComponents.CUSTOM_NAME, Component.literal("First"));
        ItemStack second = new ItemStack(Items.POTION);
        second.set(DataComponents.CUSTOM_NAME, Component.literal("Second"));
        SingleTypePouchContents contents = SingleTypePouchContents.of(first, 4);

        PouchTransferResult result = PouchTransactions.insertOne(contents, second, CAPACITY, stack -> true);

        assertEquals(PouchTransferResult.Outcome.INCOMPATIBLE_ITEM, result.outcome());
        assertEquals(contents, result.contents());
        assertEquals(1, result.otherStack().getCount());
        assertTrue(ItemStack.isSameItemSameComponents(second, result.otherStack()));
    }

    @Test
    void rejectedItemsDoNotMove() {
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);

        PouchTransferResult rejected = PouchTransactions.insertOne(
                SingleTypePouchContents.EMPTY, cobblestone, CAPACITY, stack -> false
        );

        assertEquals(PouchTransferResult.Outcome.REJECTED_ITEM, rejected.outcome());
        assertTrue(rejected.contents().isEmpty());
    }

    @Test
    void extractionMovesOneIntoAnEmptyOrPartialDestination() {
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        SingleTypePouchContents contents = SingleTypePouchContents.of(cobblestone, 2);

        PouchTransferResult toEmpty = PouchTransactions.extractOne(
                contents, ItemStack.EMPTY, CAPACITY, 64
        );
        ItemStack partial = new ItemStack(Items.COBBLESTONE, 63);
        PouchTransferResult toPartial = PouchTransactions.extractOne(
                contents, partial, CAPACITY, 64
        );

        assertTrue(toEmpty.moved());
        assertEquals(1, toEmpty.otherStack().getCount());
        assertEquals(1, toEmpty.contents().amount());
        assertTrue(toPartial.moved());
        assertEquals(64, toPartial.otherStack().getCount());
        assertEquals(1, toPartial.contents().amount());
        assertEquals(63, partial.getCount());
    }

    @Test
    void failedExtractionNeverChangesPouchOrDestination() {
        SingleTypePouchContents contents = SingleTypePouchContents.of(new ItemStack(Items.COBBLESTONE), 2);
        ItemStack incompatible = new ItemStack(Items.GRANITE, 12);
        ItemStack full = new ItemStack(Items.COBBLESTONE, 64);

        PouchTransferResult incompatibleResult = PouchTransactions.extractOne(
                contents, incompatible, CAPACITY, 64
        );
        PouchTransferResult fullResult = PouchTransactions.extractOne(contents, full, CAPACITY, 64);

        assertEquals(PouchTransferResult.Outcome.INCOMPATIBLE_ITEM, incompatibleResult.outcome());
        assertEquals(PouchTransferResult.Outcome.DESTINATION_FULL, fullResult.outcome());
        assertEquals(contents, incompatibleResult.contents());
        assertEquals(contents, fullResult.contents());
        assertEquals(12, incompatibleResult.otherStack().getCount());
        assertEquals(64, fullResult.otherStack().getCount());
        assertEquals(12, incompatible.getCount());
        assertEquals(64, full.getCount());
        assertFalse(incompatibleResult.moved());
        assertFalse(fullResult.moved());
    }

    @Test
    void repeatedTransfersPreserveTheCombinedItemCount() {
        SingleTypePouchContents contents = SingleTypePouchContents.EMPTY;
        ItemStack source = new ItemStack(Items.COBBLESTONE, CAPACITY);

        for (int click = 0; click < CAPACITY; click++) {
            PouchTransferResult result = PouchTransactions.insertOne(contents, source, CAPACITY, stack -> true);
            assertTrue(result.moved());
            contents = result.contents();
            source = result.otherStack();
            assertEquals(CAPACITY, contents.amount() + source.getCount());
        }

        ItemStack destination = ItemStack.EMPTY;
        for (int click = 0; click < CAPACITY; click++) {
            PouchTransferResult result = PouchTransactions.extractOne(contents, destination, CAPACITY, 64);
            assertTrue(result.moved());
            contents = result.contents();
            destination = result.otherStack();
            assertEquals(CAPACITY, contents.amount() + destination.getCount());
        }

        assertTrue(contents.isEmpty());
        assertEquals(CAPACITY, destination.getCount());
    }
}
