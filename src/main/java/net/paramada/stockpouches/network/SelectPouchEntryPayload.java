package net.paramada.stockpouches.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.paramada.stockpouches.Stockpouches;

public record SelectPouchEntryPayload(int slotId, int selectedIndex) implements CustomPacketPayload {

    public static final Type<SelectPouchEntryPayload> TYPE = new Type<>(Stockpouches.id("select_pouch_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPouchEntryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectPouchEntryPayload::slotId,
            ByteBufCodecs.VAR_INT, SelectPouchEntryPayload::selectedIndex,
            SelectPouchEntryPayload::new
    );

    @Override
    public Type<SelectPouchEntryPayload> type() {
        return TYPE;
    }
}
