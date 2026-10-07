package net.paramada.stockpouches.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.paramada.stockpouches.Stockpouches;

/** Requests F extraction from a player inventory slot, or -1 for the carried cursor stack. */
public record ExtractTotemPayload(int inventorySlot) implements CustomPacketPayload {

    public static final int CARRIED_STACK = -1;
    public static final Type<ExtractTotemPayload> TYPE = new Type<>(Stockpouches.id("extract_totem"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractTotemPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ExtractTotemPayload::inventorySlot,
                    ExtractTotemPayload::new
            );

    @Override
    public Type<ExtractTotemPayload> type() {
        return TYPE;
    }
}
