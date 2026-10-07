package net.paramada.stockpouches.mixin.client;

import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.paramada.stockpouches.client.PouchMouseActions;
import net.paramada.stockpouches.item.TotemPouchItem;
import net.paramada.stockpouches.network.ExtractTotemPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction action);

    @Inject(method = "init", at = @At("TAIL"))
    private void stockpouches$addMouseActions(CallbackInfo callbackInfo) {
        addItemSlotMouseAction(new PouchMouseActions());
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void stockpouches$extractTotemWithOffhandKey(
            KeyEvent event,
            CallbackInfoReturnable<Boolean> callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.keySwapOffhand.matches(event)) {
            return;
        }

        ItemStack carried = menu.getCarried();
        if (carried.getItem() instanceof TotemPouchItem) {
            ClientPlayNetworking.send(new ExtractTotemPayload(ExtractTotemPayload.CARRIED_STACK));
            callback.setReturnValue(true);
            return;
        }

        if (hoveredSlot == null
                || hoveredSlot.container != minecraft.player.getInventory()
                || hoveredSlot.getContainerSlot() < 9
                || hoveredSlot.getContainerSlot() >= minecraft.player.getInventory().getNonEquipmentItems().size()
                || !(hoveredSlot.getItem().getItem() instanceof TotemPouchItem)) {
            return;
        }

        ClientPlayNetworking.send(new ExtractTotemPayload(hoveredSlot.getContainerSlot()));
        callback.setReturnValue(true);
    }
}
