package net.paramada.stockpouches.mixin;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.paramada.stockpouches.item.TotemPouchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only vanilla's swap-hands packet when an inventory Totem Pouch can supply it. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void stockpouches$extractTotemOnSwap(
            ServerboundPlayerActionPacket packet,
            CallbackInfo callback
    ) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return;
        }

        if (TotemPouchItem.handleOffhandSwap(player)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "handleContainerClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void stockpouches$extractTotemFromMenu(
            ServerboundContainerClickPacket packet,
            CallbackInfo callback
    ) {
        if (packet.containerInput() != ContainerInput.SWAP
                || packet.buttonNum() != Inventory.SLOT_OFFHAND) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        int slotIndex = packet.slotNum();
        if (menu.containerId != packet.containerId()
                || player.isSpectator()
                || player.isDeadOrDying()
                || !menu.stillValid(player)
                || !menu.isValidSlotIndex(slotIndex)) {
            return;
        }

        Slot slot = menu.getSlot(slotIndex);
        if (TotemPouchItem.handleMenuSwap(player, slot, packet.buttonNum())) {
            menu.sendAllDataToRemote();
            callback.cancel();
        }
    }
}
