package net.paramada.stockpouches.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.paramada.stockpouches.component.PouchTransactions;
import net.paramada.stockpouches.component.PouchTransferResult;
import net.paramada.stockpouches.component.SingleTypePouchContents;

import java.util.function.Consumer;

/** A bounded pouch for one exact normal, splash, or lingering potion variant. */
public final class PotionPouchItem extends SingleTypePouchItem {

    public static final int CAPACITY = 9;

    public PotionPouchItem(Properties properties) {
        super(properties, CAPACITY, PotionPouchItem::isAcceptedPotion);
    }

    public static boolean isAcceptedPotion(ItemStack stack) {
        return stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        SingleTypePouchContents contents = contentsOf(pouch);
        if (contents.isEmpty() || !contents.isValidFor(capacity())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Inventory inventory = player.getInventory();
        int freeSlot = inventory.getFreeSlot();
        if (freeSlot == Inventory.NOT_FOUND_INDEX) {
            return InteractionResult.FAIL;
        }

        PouchTransferResult result = PouchTransactions.extractOne(
                contents,
                ItemStack.EMPTY,
                capacity(),
                1
        );
        if (!result.moved()) {
            return InteractionResult.FAIL;
        }

        inventory.setItem(freeSlot, result.otherStack());
        setContents(pouch, result.contents());
        playRemoveSound(player);
        broadcastInventoryChanges(player);
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
        textConsumer.accept(Component.translatable("tooltip.stockpouches.potion_pouch.use")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
