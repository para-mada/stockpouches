package net.paramada.stockpouches.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.paramada.stockpouches.component.ModDataComponents;
import net.paramada.stockpouches.component.PouchContents;
import net.paramada.stockpouches.component.PouchTooltip;

import java.util.Optional;
import java.util.function.Consumer;

public final class StockPouchItem extends Item {

    private final TagKey<Item> acceptedItems;

    public StockPouchItem(Properties properties, TagKey<Item> acceptedItems) {
        super(properties);
        this.acceptedItems = acceptedItems;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        ItemStack inSlot = slot.getItem();

        if (action == ClickAction.PRIMARY && !inSlot.isEmpty()) {
            if (!canInsert(inSlot) || !slot.allowModification(player)) {
                playInsertFailSound(player);
                return true;
            }

            PouchContents current = contentsOf(pouch);
            PouchContents.InsertResult preview = current.insert(inSlot);
            if (preview.inserted() == 0) {
                playInsertFailSound(player);
                return true;
            }

            ItemStack taken = slot.safeTake(preview.inserted(), preview.inserted(), player);
            PouchContents.InsertResult inserted = current.insert(taken);
            if (inserted.inserted() == 0) {
                slot.safeInsert(taken);
                playInsertFailSound(player);
                return true;
            }

            setContents(pouch, inserted.contents());
            playInsertSound(player);
            broadcastInventoryChanges(player);
            return true;
        }

        if (action == ClickAction.SECONDARY && inSlot.isEmpty()) {
            PouchContents.ExtractResult extracted = contentsOf(pouch).extractLastStack();
            if (extracted.extracted().isEmpty()) {
                return true;
            }

            ItemStack remainder = slot.safeInsert(extracted.extracted());
            PouchContents updated = extracted.contents();
            if (!remainder.isEmpty()) {
                updated = updated.insert(remainder).contents();
            }
            setContents(pouch, updated);
            if (remainder.getCount() < extracted.extracted().getCount()) {
                playRemoveSound(player);
            }
            broadcastInventoryChanges(player);
            return true;
        }

        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack pouch,
            ItemStack cursorStack,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess cursorAccess
    ) {
        if (action == ClickAction.PRIMARY && !cursorStack.isEmpty()) {
            if (!canInsert(cursorStack) || !slot.allowModification(player)) {
                playInsertFailSound(player);
                return true;
            }

            PouchContents.InsertResult inserted = contentsOf(pouch).insert(cursorStack);
            if (inserted.inserted() == 0) {
                playInsertFailSound(player);
                return true;
            }

            cursorStack.shrink(inserted.inserted());
            setContents(pouch, inserted.contents());
            playInsertSound(player);
            broadcastInventoryChanges(player);
            return true;
        }

        if (action == ClickAction.SECONDARY && cursorStack.isEmpty() && slot.allowModification(player)) {
            PouchContents.ExtractResult extracted = contentsOf(pouch).extractLastStack();
            if (!extracted.extracted().isEmpty() && cursorAccess.set(extracted.extracted())) {
                setContents(pouch, extracted.contents());
                playRemoveSound(player);
                broadcastInventoryChanges(player);
            }
            return true;
        }

        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        if (contentsOf(pouch).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        PouchContents.ExtractResult extracted = contentsOf(pouch).extractLastStack();
        if (extracted.extracted().isEmpty()) {
            return InteractionResult.PASS;
        }

        setContents(pouch, extracted.contents());
        player.drop(extracted.extracted(), true, Prediction.PREDICTED);
        playRemoveSound(player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> textConsumer,
            TooltipFlag type
    ) {
        PouchContents contents = contentsOf(stack);
        if (contents.isEmpty()) {
            textConsumer.accept(Component.translatable("tooltip.stockpouches.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        // The visual component carries the contents; keeping this area free of
        // text gives the pouch the same compact silhouette as the vanilla bundle.
    }

    @Override
    public Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
        PouchContents contents = contentsOf(stack);
        return contents.isEmpty() ? Optional.empty() : Optional.of(new PouchTooltip(contents));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    private boolean canInsert(ItemStack stack) {
        return stack.is(acceptedItems) && stack.getItem().canFitInsideContainerItems();
    }

    private static PouchContents contentsOf(ItemStack pouch) {
        return pouch.getOrDefault(ModDataComponents.POUCH_CONTENTS, PouchContents.EMPTY);
    }

    private static void setContents(ItemStack pouch, PouchContents contents) {
        pouch.set(ModDataComponents.POUCH_CONTENTS, contents);
    }

    private static void broadcastInventoryChanges(Player player) {
        if (player.containerMenu != null) {
            player.containerMenu.slotsChanged(player.getInventory());
        }
    }

    private static void playInsertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    private static void playRemoveSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }
}
