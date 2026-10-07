package net.paramada.stockpouches.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.paramada.stockpouches.component.ModDataComponents;
import net.paramada.stockpouches.component.PouchContents;
import net.paramada.stockpouches.item.StockPouchItem;
import net.paramada.stockpouches.item.TotemPouchItem;

public final class ModNetworking {

    private ModNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                SelectPouchEntryPayload.TYPE,
                SelectPouchEntryPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                ExtractTotemPayload.TYPE,
                ExtractTotemPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(SelectPouchEntryPayload.TYPE, (payload, context) -> {
            if (payload.slotId() < 0 || payload.slotId() >= context.player().containerMenu.slots.size()) {
                return;
            }

            Slot slot = context.player().containerMenu.getSlot(payload.slotId());
            ItemStack pouch = slot.getItem();
            if (!(pouch.getItem() instanceof StockPouchItem)) {
                return;
            }

            PouchContents contents = pouch.getOrDefault(ModDataComponents.POUCH_CONTENTS, PouchContents.EMPTY);
            if (payload.selectedIndex() < -1 || payload.selectedIndex() >= contents.entries().size()) {
                return;
            }

            pouch.set(ModDataComponents.POUCH_CONTENTS, contents.withSelectedIndex(payload.selectedIndex()));
            context.player().containerMenu.broadcastChanges();
        });
        ServerPlayNetworking.registerGlobalReceiver(ExtractTotemPayload.TYPE, (payload, context) -> {
            if (TotemPouchItem.extractFromInventoryOrCursor(context.player(), payload.inventorySlot())) {
                context.player().containerMenu.broadcastChanges();
            }
        });
    }
}
