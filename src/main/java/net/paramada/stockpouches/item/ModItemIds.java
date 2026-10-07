package net.paramada.stockpouches.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.paramada.stockpouches.Stockpouches;

public final class ModItemIds {

    public static final ResourceKey<Item> QUARRY_POUCH = create("quarry_pouch");
    public static final ResourceKey<Item> LUMBERJACK_POUCH = create("lumberjack_pouch");
    public static final ResourceKey<Item> HUNTER_POUCH = create("hunter_pouch");
    public static final ResourceKey<Item> RANGER_POUCH = create("ranger_pouch");
    public static final ResourceKey<Item> FARMER_POUCH = create("farmer_pouch");
    public static final ResourceKey<Item> POTION_POUCH = create("potion_pouch");
    public static final ResourceKey<Item> TOTEM_POUCH = create("totem_pouch");

    private ModItemIds() {}

    private static ResourceKey<Item> create(String path) {
        return ResourceKey.create(Registries.ITEM, Stockpouches.id(path));
    }
}
