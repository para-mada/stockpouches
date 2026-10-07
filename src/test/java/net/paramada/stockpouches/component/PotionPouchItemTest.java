package net.paramada.stockpouches.component;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.paramada.stockpouches.item.PotionPouchItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PotionPouchItemTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void acceptsExactlyTheThreePotionItemForms() {
        ItemStack normal = potion(Items.POTION, Potions.SWIFTNESS);
        ItemStack splash = potion(Items.SPLASH_POTION, Potions.SWIFTNESS);
        ItemStack lingering = potion(Items.LINGERING_POTION, Potions.SWIFTNESS);
        ItemStack tippedArrow = potion(Items.TIPPED_ARROW, Potions.SWIFTNESS);

        assertTrue(PotionPouchItem.isAcceptedPotion(normal));
        assertTrue(PotionPouchItem.isAcceptedPotion(splash));
        assertTrue(PotionPouchItem.isAcceptedPotion(lingering));
        assertFalse(PotionPouchItem.isAcceptedPotion(new ItemStack(Items.GLASS_BOTTLE)));
        assertFalse(PotionPouchItem.isAcceptedPotion(tippedArrow));
    }

    @Test
    void formEffectPowerDurationAndComponentsRemainPartOfIdentity() {
        ItemStack normal = potion(Items.POTION, Potions.SWIFTNESS);
        SingleTypePouchContents contents = SingleTypePouchContents.of(normal, 1);

        assertMoved(contents, potion(Items.POTION, Potions.SWIFTNESS));
        assertIncompatible(contents, potion(Items.SPLASH_POTION, Potions.SWIFTNESS));
        assertIncompatible(contents, potion(Items.POTION, Potions.LONG_SWIFTNESS));
        assertIncompatible(contents, potion(Items.POTION, Potions.STRONG_SWIFTNESS));

        ItemStack scaledDuration = potion(Items.POTION, Potions.SWIFTNESS);
        scaledDuration.set(DataComponents.POTION_DURATION_SCALE, 0.5F);
        assertIncompatible(contents, scaledDuration);
    }

    @Test
    void extractionReturnsARealPotionWithAllComponents() {
        ItemStack original = potion(Items.LINGERING_POTION, Potions.LONG_POISON);
        original.set(DataComponents.POTION_DURATION_SCALE, 0.75F);
        SingleTypePouchContents contents = SingleTypePouchContents.of(original, 2);

        PouchTransferResult result = PouchTransactions.extractOne(
                contents,
                ItemStack.EMPTY,
                PotionPouchItem.CAPACITY,
                1
        );

        assertTrue(result.moved());
        assertEquals(1, result.contents().amount());
        assertTrue(ItemStack.isSameItemSameComponents(original, result.otherStack()));
    }

    private static ItemStack potion(net.minecraft.world.item.Item item, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return PotionContents.createItemStack(item, potion);
    }

    private static void assertMoved(SingleTypePouchContents contents, ItemStack candidate) {
        PouchTransferResult result = PouchTransactions.insertOne(
                contents,
                candidate,
                PotionPouchItem.CAPACITY,
                PotionPouchItem::isAcceptedPotion
        );
        assertTrue(result.moved());
    }

    private static void assertIncompatible(SingleTypePouchContents contents, ItemStack candidate) {
        PouchTransferResult result = PouchTransactions.insertOne(
                contents,
                candidate,
                PotionPouchItem.CAPACITY,
                PotionPouchItem::isAcceptedPotion
        );
        assertEquals(PouchTransferResult.Outcome.INCOMPATIBLE_ITEM, result.outcome());
        assertEquals(contents, result.contents());
        assertEquals(1, result.otherStack().getCount());
    }
}
