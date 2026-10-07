package net.paramada.stockpouches.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.paramada.stockpouches.item.TotemPouchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts F over an inventory Totem Pouch into extraction before vanilla can swap the pouch. */
@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void stockpouches$handleTotemPouchMenuSwap(
            int slotIndex,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo callback
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (input != ContainerInput.SWAP || !menu.isValidSlotIndex(slotIndex)) {
            return;
        }

        Slot slot = menu.getSlot(slotIndex);
        if (TotemPouchItem.handleMenuSwap(player, slot, button)) {
            callback.cancel();
        }
    }
}
