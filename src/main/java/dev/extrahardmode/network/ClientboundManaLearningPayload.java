package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Effective mana learning capacity: configured base plus redstone gifts. */
public record ClientboundManaLearningPayload(double capacity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundManaLearningPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("mana_learning"));

    public static final StreamCodec<ByteBuf, ClientboundManaLearningPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ClientboundManaLearningPayload::capacity, ClientboundManaLearningPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
