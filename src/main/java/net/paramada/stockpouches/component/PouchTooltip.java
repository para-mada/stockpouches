package net.paramada.stockpouches.component;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record PouchTooltip(PouchContents contents) implements TooltipComponent {}
