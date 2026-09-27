package net.paramada.stockpouches.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.paramada.stockpouches.Stockpouches;

public final class ModItemTags {

    public static final TagKey<Item> QUARRY = create("quarry");
    public static final TagKey<Item> LUMBERJACK = create("lumberjack");
    public static final TagKey<Item> HUNTER = create("hunter");
    public static final TagKey<Item> RANGER = create("ranger");
    public static final TagKey<Item> FARMER = create("farmer");

    private ModItemTags() {}

    private static TagKey<Item> create(String path) {
        return TagKey.create(Registries.ITEM, Stockpouches.id(path));
    }
}
