package net.paramada.stockpouches.component;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.paramada.stockpouches.Stockpouches;

public final class ModDataComponents {

    public static final DataComponentType<PouchContents> POUCH_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Stockpouches.id("contents"),
            DataComponentType.<PouchContents>builder()
                    .persistent(PouchContents.CODEC)
                    .networkSynchronized(PouchContents.STREAM_CODEC)
                    .build()
    );

    private ModDataComponents() {}

    public static void initialize() {
        // Loading this class performs the registry operation above.
    }
}
