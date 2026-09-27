package net.paramada.stockpouches.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.paramada.stockpouches.component.ModDataComponents;
import net.paramada.stockpouches.component.PouchContents;
import net.paramada.stockpouches.item.StockPouchItem;
import net.paramada.stockpouches.network.SelectPouchEntryPayload;

public final class PouchMouseActions implements ItemSlotMouseAction {

    @Override
    public boolean matches(Slot slot) {
        return slot.getItem().getItem() instanceof StockPouchItem;
    }

    @Override
    public boolean onMouseScrolled(double horizontal, double vertical, int slotId, ItemStack stack) {
        if (vertical == 0.0) {
            return false;
        }

        PouchContents current = stack.getOrDefault(ModDataComponents.POUCH_CONTENTS, PouchContents.EMPTY);
        PouchContents updated = current.cycleSelection(vertical < 0.0 ? 1 : -1);
        if (updated.selectedIndex() == current.selectedIndex()) {
            return false;
        }

        // Update immediately for responsive rendering; the server validates and
        // authoritatively applies the same selected index.
        stack.set(ModDataComponents.POUCH_CONTENTS, updated);
        ClientPlayNetworking.send(new SelectPouchEntryPayload(slotId, updated.selectedIndex()));
        return true;
    }

    @Override
    public void onStopHovering(Slot slot) {
    }

    @Override
    public void onSlotClicked(Slot slot, ContainerInput input) {
    }
}
