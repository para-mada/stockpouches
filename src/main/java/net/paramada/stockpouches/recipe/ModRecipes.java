package net.paramada.stockpouches.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.paramada.stockpouches.Stockpouches;
import net.paramada.stockpouches.item.ModItems;
import net.paramada.stockpouches.tag.ModItemTags;

import java.util.concurrent.atomic.AtomicReference;

public final class ModRecipes {

    public static final RecipeSerializer<PouchRecipe> QUARRY_POUCH = register(
            "quarry_pouch", ModItemTags.QUARRY, ModItems.QUARRY_POUCH);
    public static final RecipeSerializer<PouchRecipe> LUMBERJACK_POUCH = register(
            "lumberjack_pouch", ModItemTags.LUMBERJACK, ModItems.LUMBERJACK_POUCH);
    public static final RecipeSerializer<PouchRecipe> HUNTER_POUCH = register(
            "hunter_pouch", ModItemTags.HUNTER, ModItems.HUNTER_POUCH);
    public static final RecipeSerializer<PouchRecipe> RANGER_POUCH = register(
            "ranger_pouch", ModItemTags.RANGER, ModItems.RANGER_POUCH);
    public static final RecipeSerializer<PouchRecipe> FARMER_POUCH = register(
            "farmer_pouch", ModItemTags.FARMER, ModItems.FARMER_POUCH);

    private ModRecipes() {}

    private static RecipeSerializer<PouchRecipe> register(String path, TagKey<Item> materialTag, Item result) {
        AtomicReference<RecipeSerializer<PouchRecipe>> reference = new AtomicReference<>();
        PouchRecipe recipe = new PouchRecipe(materialTag, result, reference::get);
        RecipeSerializer<PouchRecipe> serializer = new RecipeSerializer<>(
                MapCodec.unit(recipe),
                StreamCodec.<RegistryFriendlyByteBuf, PouchRecipe>unit(recipe)
        );
        reference.set(serializer);
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Stockpouches.id(path), serializer);
    }

    public static void initialize() {
        // Loading this class registers the five serializers.
    }
}
