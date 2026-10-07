package net.paramada.stockpouches.component;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PouchContentsTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void repeatedInsertionsAndExtractionsPreserveEveryItem() {
        PouchContents contents = PouchContents.EMPTY;
        long insertedTotal = 0;

        for (int index = 0; index < 2_000; index++) {
            ItemStackTemplate source = new ItemStackTemplate(Items.COBBLESTONE, 64);
            PouchContents.InsertResult inserted = contents.insert(source);
            contents = inserted.contents();
            insertedTotal += inserted.inserted();
        }

        long extractedTotal = 0;
        while (!contents.isEmpty()) {
            PouchContents.ExtractEntryResult extracted = contents.extractLast(64);
            assertTrue(extracted.extracted().isPresent());
            extractedTotal += extracted.extracted().orElseThrow().amount();
            contents = extracted.contents();
        }

        assertEquals(128_000L, insertedTotal);
        assertEquals(insertedTotal, extractedTotal);
    }

    @Test
    void reinsertingATypeMakesItTheNextTypeExtracted() {
        PouchContents contents = PouchContents.EMPTY
                .insert(new ItemStackTemplate(Items.COBBLESTONE, 20)).contents()
                .insert(new ItemStackTemplate(Items.GRANITE, 15)).contents()
                .insert(new ItemStackTemplate(Items.COBBLESTONE, 5)).contents();

        PouchContents.ExtractEntryResult extracted = contents.extractLast(64);

        assertEquals(Items.COBBLESTONE, extracted.extracted().orElseThrow().template().item().value());
        assertEquals(25, extracted.extracted().orElseThrow().amount());
        assertEquals(1, extracted.contents().distinctEntries());
    }

    @Test
    void componentVariantsAreNeverMerged() {
        ItemStackTemplate first = new ItemStackTemplate(Items.COBBLESTONE.builtInRegistryHolder(), 12, DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("First")).build());
        ItemStackTemplate second = new ItemStackTemplate(Items.COBBLESTONE.builtInRegistryHolder(), 7, DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("Second")).build());

        PouchContents contents = PouchContents.EMPTY.insert(first).contents().insert(second).contents();

        assertEquals(2, contents.distinctEntries());
        assertEquals(19, contents.totalItems());
        assertNotEquals(first.withCount(1), second.withCount(1));
        assertEquals(second.withCount(1), contents.extractLast(64).extracted().orElseThrow().template());
    }

    @Test
    void insertionStopsAtLongMaxWithoutOverflowing() {
        PouchEntry nearlyFull = PouchEntry.fromTemplate(new ItemStackTemplate(Items.COBBLESTONE), Long.MAX_VALUE - 10);
        PouchContents contents = new PouchContents(java.util.List.of(nearlyFull));

        PouchContents.InsertResult result = contents.insert(new ItemStackTemplate(Items.COBBLESTONE, 64));

        assertEquals(10, result.inserted());
        assertEquals(Long.MAX_VALUE, result.contents().totalItems());
        assertEquals(0, result.contents().insert(new ItemStackTemplate(Items.COBBLESTONE)).inserted());
    }

    @Test
    void persistentCodecRoundTripPreservesLargeAmountsAndOrder() {
        PouchContents original = new PouchContents(java.util.List.of(
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.DIORITE), 84_291L),
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.ANDESITE), 4_000_000_000L)
        ), 0);

        var encoded = PouchContents.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        PouchContents decoded = PouchContents.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, decoded);
        assertEquals(Items.DIORITE, decoded.extractLast(64).extracted().orElseThrow().template().item().value());
    }

    @Test
    void scrollingSelectsEntriesByDescendingAmountAndWraps() {
        PouchContents contents = new PouchContents(java.util.List.of(
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.DIORITE), 20),
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.ANDESITE), 90),
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.GRANITE), 40)
        ));

        contents = contents.cycleSelection(1);
        assertEquals(1, contents.selectedIndex());
        contents = contents.cycleSelection(1);
        assertEquals(2, contents.selectedIndex());
        contents = contents.cycleSelection(1);
        assertEquals(0, contents.selectedIndex());
        contents = contents.cycleSelection(1);
        assertEquals(1, contents.selectedIndex());
    }

    @Test
    void selectedEntryIsExtractedInsteadOfTheLastInsertedEntry() {
        PouchContents contents = new PouchContents(java.util.List.of(
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.DIORITE), 20),
                PouchEntry.fromTemplate(new ItemStackTemplate(Items.ANDESITE), 90)
        ), 0);

        PouchContents.ExtractEntryResult extracted = contents.extractLast(64);

        assertEquals(Items.DIORITE, extracted.extracted().orElseThrow().template().item().value());
        assertEquals(20, extracted.extracted().orElseThrow().amount());
        assertEquals(-1, extracted.contents().selectedIndex());
    }
}
