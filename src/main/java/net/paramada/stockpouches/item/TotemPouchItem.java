package net.paramada.stockpouches.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.paramada.stockpouches.component.SingleTypePouchContents;
import net.paramada.stockpouches.component.TotemSwapResult;
import net.paramada.stockpouches.component.TotemSwapTransactions;

import java.util.function.Consumer;

/** Stores inert vanilla totems and equips one only after an intentional offhand-swap action. */
public final class TotemPouchItem extends SingleTypePouchItem {

    public static final int CAPACITY = 15;
    private static final int HOTBAR_SIZE = 9;

    public TotemPouchItem(Properties properties) {
        super(properties, CAPACITY, stack -> stack.is(Items.TOTEM_OF_UNDYING));
    }

    @Override
    public boolean overrideStackedOnOther(
            ItemStack pouch,
            Slot slot,
            ClickAction action,
        Player player
    ) {
        if (action == ClickAction.PRIMARY && isRestrictedPlayerSlot(slot, player)) {
            sendRestrictionWarning(player);
            return true;
        }
        return super.overrideStackedOnOther(pouch, slot, action, player);
    }

    /** Handles F from a pouch stored in the main inventory, never from the hotbar itself. */
    public static boolean handleOffhandSwap(ServerPlayer player) {
        if (player.isSpectator()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        boolean hasStoredPouch = false;
        for (int slot = HOTBAR_SIZE; slot < inventory.getNonEquipmentItems().size(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof TotemPouchItem pouch)) {
                continue;
            }
            hasStoredPouch = true;
            if (pouch.hasUsableTotem(stack)) {
                pouch.extractOneToOffhand(player, stack, slot);
                return true;
            }
        }

        return hasStoredPouch
                || isTotemPouch(player.getItemInHand(InteractionHand.MAIN_HAND))
                || isTotemPouch(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    /** Handles number-key swaps while a menu is open, including F over a pouch slot. */
    public static boolean handleMenuSwap(Player player, Slot slot, int targetInventorySlot) {
        ItemStack stack = slot.getItem();
        if (!(stack.getItem() instanceof TotemPouchItem pouch)) {
            return false;
        }

        int pouchSlot = findMainInventorySlot(player.getInventory(), stack);
        if (targetInventorySlot == Inventory.SLOT_OFFHAND && pouchSlot >= 0) {
            if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isSpectator()) {
                pouch.extractOneToOffhand(serverPlayer, stack, pouchSlot);
            }
            return true;
        }

        if (Inventory.isHotbarSlot(targetInventorySlot)
                || targetInventorySlot == Inventory.SLOT_OFFHAND) {
            sendRestrictionWarning(player);
            return true;
        }
        return false;
    }

    /** Server-authoritative target for the dedicated inventory F payload. */
    public static boolean extractFromInventoryOrCursor(ServerPlayer player, int inventorySlot) {
        ItemStack stack;
        if (inventorySlot == -1) {
            stack = player.containerMenu.getCarried();
        } else {
            if (inventorySlot < HOTBAR_SIZE
                    || inventorySlot >= player.getInventory().getNonEquipmentItems().size()) {
                return false;
            }
            stack = player.getInventory().getItem(inventorySlot);
        }

        if (!(stack.getItem() instanceof TotemPouchItem pouch)) {
            return false;
        }
        pouch.extractOneToOffhand(player, stack, inventorySlot);
        return true;
    }

    private boolean hasUsableTotem(ItemStack pouch) {
        SingleTypePouchContents contents = contentsOf(pouch);
        return contents.isValidFor(capacity())
                && !contents.isEmpty()
                && contents.createStack(1).is(Items.TOTEM_OF_UNDYING);
    }

    private void extractOneToOffhand(ServerPlayer player, ItemStack pouch, int pouchSlot) {
        Inventory inventory = player.getInventory();
        TotemSwapResult result = TotemSwapTransactions.extractToOffhand(
                contentsOf(pouch),
                player.getItemInHand(InteractionHand.OFF_HAND),
                inventory.getNonEquipmentItems(),
                pouchSlot,
                inventory.getMaxStackSize(),
                capacity()
        );
        if (!result.moved()) {
            return;
        }

        for (TotemSwapResult.SlotChange change : result.inventoryChanges()) {
            inventory.setItem(change.slot(), change.stack());
        }
        setContents(pouch, result.contents());
        player.setItemInHand(InteractionHand.OFF_HAND, result.offhand());
        player.stopUsingItem();
        inventory.setChanged();
        playRemoveSound(player);
        broadcastInventoryChanges(player);
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            ServerLevel level,
            Entity entity,
            EquipmentSlot equipmentSlot
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        Inventory inventory = player.getInventory();
        int destination = findEmptyMainInventorySlot(inventory);
        if (destination < 0) {
            return;
        }

        if (equipmentSlot == EquipmentSlot.OFFHAND
                && player.getItemInHand(InteractionHand.OFF_HAND) == stack) {
            ItemStack displaced = inventory.getItem(destination);
            inventory.setItem(destination, stack);
            player.setItemInHand(InteractionHand.OFF_HAND, displaced);
            inventory.setChanged();
            broadcastInventoryChanges(player);
            return;
        }

        for (int source = 0; source < HOTBAR_SIZE; source++) {
            if (inventory.getItem(source) != stack) {
                continue;
            }

            ItemStack displaced = inventory.getItem(destination);
            inventory.setItem(destination, stack);
            inventory.setItem(source, displaced);
            inventory.setChanged();
            broadcastInventoryChanges(player);
            return;
        }
    }

    private static int findEmptyMainInventorySlot(Inventory inventory) {
        for (int slot = HOTBAR_SIZE; slot < inventory.getNonEquipmentItems().size(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isRestrictedPlayerSlot(Slot slot, Player player) {
        if (slot.container != player.getInventory()) {
            return false;
        }
        int inventorySlot = slot.getContainerSlot();
        return Inventory.isHotbarSlot(inventorySlot) || inventorySlot == Inventory.SLOT_OFFHAND;
    }

    private static int findMainInventorySlot(Inventory inventory, ItemStack stack) {
        for (int slot = HOTBAR_SIZE; slot < inventory.getNonEquipmentItems().size(); slot++) {
            if (inventory.getItem(slot) == stack) {
                return slot;
            }
        }
        return -1;
    }

    private static void sendRestrictionWarning(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "tooltip.stockpouches.totem_pouch.restriction"
            ));
        }
    }

    private static boolean isTotemPouch(ItemStack stack) {
        return stack.getItem() instanceof TotemPouchItem;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> textConsumer,
            TooltipFlag type
    ) {
        SingleTypePouchContents contents = contentsOf(stack);
        if (contents.isEmpty()) {
            textConsumer.accept(Component.translatable("tooltip.stockpouches.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            ItemStack prototype = contents.createStack(1);
            textConsumer.accept(Component.translatable(
                    "tooltip.stockpouches.single_type",
                    prototype.getHoverName(),
                    contents.amount(),
                    capacity()
            ));
        }
        textConsumer.accept(Component.translatable("tooltip.stockpouches.totem_pouch.restriction")
                .withStyle(ChatFormatting.RED));
        textConsumer.accept(Component.translatable("tooltip.stockpouches.totem_pouch.swap")
                .withStyle(ChatFormatting.DARK_GRAY));
        textConsumer.accept(Component.translatable("tooltip.stockpouches.totem_pouch.extract")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
