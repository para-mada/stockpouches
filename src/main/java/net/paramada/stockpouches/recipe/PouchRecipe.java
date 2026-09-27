package net.paramada.stockpouches.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * A 3x3 ring of category materials around an empty vanilla bundle.
 *
 * A custom matcher is intentional: a normal shaped ingredient only checks the
 * item type and would consume filled bundles, silently deleting their contents.
 */
public final class PouchRecipe extends CustomRecipe {

    private static final int BUNDLE_SLOT = 4;

    private final TagKey<Item> materialTag;
    private final Item result;
    private final Supplier<RecipeSerializer<PouchRecipe>> serializer;

    PouchRecipe(
            TagKey<Item> materialTag,
            Item result,
            Supplier<RecipeSerializer<PouchRecipe>> serializer
    ) {
        this.materialTag = materialTag;
        this.result = result;
        this.serializer = serializer;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) {
            return false;
        }

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (slot == BUNDLE_SLOT) {
                if (!isEmptyBundle(stack)) {
                    return false;
                }
            } else if (!stack.is(materialTag)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(result);
    }

    @Override
    public RecipeSerializer<PouchRecipe> getSerializer() {
        return serializer.get();
    }

    private static boolean isEmptyBundle(ItemStack stack) {
        if (!stack.is(Items.BUNDLE)) {
            return false;
        }
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return contents.isEmpty();
    }
}
