package net.paramada.stockpouches.component;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.paramada.stockpouches.item.TotemPouchItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotemSwapTransactionsTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void emptyOffhandReceivesExactlyOneTotem() {
        SingleTypePouchContents contents = totems(3);

        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                contents,
                ItemStack.EMPTY,
                inventoryOfSize(36),
                0,
                64,
                TotemPouchItem.CAPACITY
        );

        assertTrue(result.moved());
        assertEquals(2, result.contents().amount());
        assertTrue(result.offhand().is(Items.TOTEM_OF_UNDYING));
        assertEquals(1, result.offhand().getCount());
        assertTrue(result.inventoryChanges().isEmpty());
    }

    @Test
    void occupiedOffhandIsFullyPlannedBeforeTotemExtraction() {
        List<ItemStack> inventory = inventoryOfSize(36);
        inventory.set(1, new ItemStack(Items.ARROW, 63));
        ItemStack oldOffhand = new ItemStack(Items.ARROW, 2);
        SingleTypePouchContents contents = totems(2);

        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                contents,
                oldOffhand,
                inventory,
                0,
                64,
                TotemPouchItem.CAPACITY
        );

        assertTrue(result.moved());
        assertEquals(1, result.contents().amount());
        assertEquals(2, result.inventoryChanges().size());
        assertEquals(64, result.inventoryChanges().get(0).stack().getCount());
        assertEquals(1, result.inventoryChanges().get(1).stack().getCount());
        assertEquals(63, inventory.get(1).getCount());
        assertEquals(2, oldOffhand.getCount());
    }

    @Test
    void cursorHeldPouchCanStoreThePreviousOffhandWithoutAnExcludedSlot() {
        List<ItemStack> inventory = inventoryOfSize(36);
        ItemStack oldOffhand = new ItemStack(Items.DIAMOND);

        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                totems(2),
                oldOffhand,
                inventory,
                -1,
                64,
                TotemPouchItem.CAPACITY
        );

        assertTrue(result.moved());
        assertEquals(1, result.contents().amount());
        assertTrue(result.offhand().is(Items.TOTEM_OF_UNDYING));
        assertEquals(1, result.inventoryChanges().size());
        assertTrue(result.inventoryChanges().getFirst().stack().is(Items.DIAMOND));
        assertEquals(1, oldOffhand.getCount());
    }

    @Test
    void fullInventoryRejectsWithoutAnyPartialChange() {
        List<ItemStack> inventory = fullInventory();
        ItemStack oldOffhand = new ItemStack(Items.DIAMOND);
        SingleTypePouchContents contents = totems(2);

        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                contents,
                oldOffhand,
                inventory,
                0,
                64,
                TotemPouchItem.CAPACITY
        );

        assertEquals(TotemSwapResult.Outcome.INVENTORY_FULL, result.outcome());
        assertEquals(contents, result.contents());
        assertTrue(ItemStack.isSameItemSameComponents(oldOffhand, result.offhand()));
        assertEquals(1, result.offhand().getCount());
        assertTrue(result.inventoryChanges().isEmpty());
    }

    @Test
    void pouchInventorySlotCannotBeUsedAsTemporaryStorage() {
        List<ItemStack> inventory = fullInventory();
        inventory.set(9, ItemStack.EMPTY);

        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                totems(1),
                new ItemStack(Items.DIAMOND),
                inventory,
                9,
                64,
                TotemPouchItem.CAPACITY
        );

        assertEquals(TotemSwapResult.Outcome.INVENTORY_FULL, result.outcome());
        assertEquals(1, result.contents().amount());
        assertTrue(result.inventoryChanges().isEmpty());
    }

    @Test
    void forgedNonTotemContentsAreInert() {
        SingleTypePouchContents forged = SingleTypePouchContents.of(new ItemStack(Items.APPLE), 1);

        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                forged,
                ItemStack.EMPTY,
                inventoryOfSize(36),
                0,
                64,
                TotemPouchItem.CAPACITY
        );

        assertEquals(TotemSwapResult.Outcome.INVALID_CONTENTS, result.outcome());
        assertEquals(forged, result.contents());
        assertTrue(result.offhand().isEmpty());
    }

    private static SingleTypePouchContents totems(int amount) {
        return SingleTypePouchContents.of(new ItemStack(Items.TOTEM_OF_UNDYING), amount);
    }

    private static List<ItemStack> inventoryOfSize(int size) {
        List<ItemStack> inventory = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            inventory.add(ItemStack.EMPTY);
        }
        return inventory;
    }

    private static List<ItemStack> fullInventory() {
        List<ItemStack> inventory = inventoryOfSize(36);
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        return inventory;
    }
}
