package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Mana learning capacity, plus the stored mana level before Well Fed and ready gold.
 * The inventory crystal shows those as level/capacity.
 */
public record ClientboundManaLearningPayload(double capacity, int baseManaLevel) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundManaLearningPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("mana_learning"));

    public static final StreamCodec<ByteBuf, ClientboundManaLearningPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ClientboundManaLearningPayload::capacity,
            ByteBufCodecs.VAR_INT,
            ClientboundManaLearningPayload::baseManaLevel,
            ClientboundManaLearningPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
