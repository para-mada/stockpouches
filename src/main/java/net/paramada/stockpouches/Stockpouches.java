package net.paramada.stockpouches;

import net.fabricmc.api.ModInitializer;
import net.paramada.stockpouches.component.ModDataComponents;
import net.paramada.stockpouches.item.ModCreativeTabs;
import net.paramada.stockpouches.item.ModItems;
import net.paramada.stockpouches.recipe.ModRecipes;
import net.paramada.stockpouches.network.ModNetworking;
import net.minecraft.resources.Identifier;

public class Stockpouches implements ModInitializer {

    public static final String MOD_ID = "stockpouches";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModDataComponents.initialize();
        ModItems.initialize();
        ModCreativeTabs.initialize();
        ModRecipes.initialize();
        ModNetworking.initialize();
    }
}
