package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundManaPayload(int manaLevel, float currentMana) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundManaPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("mana"));

    public static final StreamCodec<ByteBuf, ClientboundManaPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundManaPayload::manaLevel,
            ByteBufCodecs.FLOAT,
            ClientboundManaPayload::currentMana,
            ClientboundManaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
