package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Where a large shield should rise because a chunk just became clear. */
public record ClientboundChunkClearShieldPayload(BlockPos origin) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundChunkClearShieldPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("chunk_clear_shield"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundChunkClearShieldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    buf(BlockPos.STREAM_CODEC),
                    ClientboundChunkClearShieldPayload::origin,
                    ClientboundChunkClearShieldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> buf(StreamCodec<ByteBuf, T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buffer) {
                return codec.decode(buffer);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, T value) {
                codec.encode(buffer, value);
            }
        };
    }
}
