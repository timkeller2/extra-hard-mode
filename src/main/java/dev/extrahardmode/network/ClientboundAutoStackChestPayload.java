package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Excluded slots for the chest the player has open. {@code active} is false when
 * that screen is not a chest auto-stack can use. {@code mask} replaces the client's copy.
 */
public record ClientboundAutoStackChestPayload(boolean active, long mask) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundAutoStackChestPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("auto_stack_chest"));

    public static final StreamCodec<ByteBuf, ClientboundAutoStackChestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundAutoStackChestPayload::active,
            ByteBufCodecs.VAR_LONG,
            ClientboundAutoStackChestPayload::mask,
            ClientboundAutoStackChestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
