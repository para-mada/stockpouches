package net.paramada.stockpouches.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.paramada.stockpouches.item.TotemPouchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Enforces the Totem Pouch slot restriction across vanilla menu transfer paths. */
@Mixin(Slot.class)
abstract class SlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void stockpouches$rejectTotemPouchInHands(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        Slot slot = (Slot) (Object) this;
        if (!(stack.getItem() instanceof TotemPouchItem) || !(slot.container instanceof Inventory)) {
            return;
        }

        int inventorySlot = slot.getContainerSlot();
        if (Inventory.isHotbarSlot(inventorySlot) || inventorySlot == Inventory.SLOT_OFFHAND) {
            callback.setReturnValue(false);
        }
    }
}
