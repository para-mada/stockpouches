package net.paramada.stockpouches.mixin.client;

import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.paramada.stockpouches.client.PouchMouseActions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {

    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction action);

    @Inject(method = "init", at = @At("TAIL"))
    private void stockpouches$addMouseActions(CallbackInfo callbackInfo) {
        addItemSlotMouseAction(new PouchMouseActions());
    }
}
