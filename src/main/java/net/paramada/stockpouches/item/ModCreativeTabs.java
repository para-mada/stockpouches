package net.paramada.stockpouches.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.paramada.stockpouches.Stockpouches;

public final class ModCreativeTabs {

    public static final ResourceKey<CreativeModeTab> STOCK_POUCHES = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Stockpouches.id("stock_pouches")
    );

    private ModCreativeTabs() {}

    public static void initialize() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                STOCK_POUCHES,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.stockpouches.stock_pouches"))
                        .icon(() -> new ItemStack(ModItems.QUARRY_POUCH))
                        .displayItems((context, output) -> {
                            output.accept(ModItems.QUARRY_POUCH);
                            output.accept(ModItems.LUMBERJACK_POUCH);
                            output.accept(ModItems.HUNTER_POUCH);
                            output.accept(ModItems.RANGER_POUCH);
                            output.accept(ModItems.FARMER_POUCH);
                            output.accept(ModItems.POTION_POUCH);
                            output.accept(ModItems.TOTEM_POUCH);
                        })
                        .build()
        );
    }
}
