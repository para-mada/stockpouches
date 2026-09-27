package net.paramada.stockpouches.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.paramada.stockpouches.component.ModDataComponents;
import net.paramada.stockpouches.component.PouchContents;
import net.paramada.stockpouches.tag.ModItemTags;

public final class ModItems {

    public static final Item QUARRY_POUCH = register(ModItemIds.QUARRY_POUCH, ModItemTags.QUARRY);
    public static final Item LUMBERJACK_POUCH = register(ModItemIds.LUMBERJACK_POUCH, ModItemTags.LUMBERJACK);
    public static final Item HUNTER_POUCH = register(ModItemIds.HUNTER_POUCH, ModItemTags.HUNTER);
    public static final Item RANGER_POUCH = register(ModItemIds.RANGER_POUCH, ModItemTags.RANGER);
    public static final Item FARMER_POUCH = register(ModItemIds.FARMER_POUCH, ModItemTags.FARMER);

    private ModItems() {}

    private static Item register(ResourceKey<Item> key, TagKey<Item> acceptedItems) {
        Item.Properties properties = new Item.Properties()
                .stacksTo(1)
                .component(ModDataComponents.POUCH_CONTENTS, PouchContents.EMPTY)
                .setId(key);
        Item item = new StockPouchItem(properties, acceptedItems);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(QUARRY_POUCH);
            entries.accept(LUMBERJACK_POUCH);
            entries.accept(HUNTER_POUCH);
            entries.accept(RANGER_POUCH);
            entries.accept(FARMER_POUCH);
        });
    }
}
