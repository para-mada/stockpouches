package net.paramada.stockpouches.component;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;

final class MinecraftTestBootstrap {

    private MinecraftTestBootstrap() {}

    static synchronized void initialize() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.ITEM.stream()
                .map(item -> item.builtInRegistryHolder())
                .filter(holder -> !holder.areComponentsBound())
                .forEach(holder -> holder.bindComponents(DataComponentMap.builder()
                        .set(DataComponents.MAX_STACK_SIZE, 64)
                        .build()));
    }
}
