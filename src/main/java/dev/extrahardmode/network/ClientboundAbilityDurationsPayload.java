package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundAbilityDurationsPayload(List<Entry> effects) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundAbilityDurationsPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("ability_durations"));

    public record Entry(String ability, int remainingTicks, int maxTicks, boolean autoContinue) {
        static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                Entry::ability,
                ByteBufCodecs.VAR_INT,
                Entry::remainingTicks,
                ByteBufCodecs.VAR_INT,
                Entry::maxTicks,
                ByteBufCodecs.BOOL,
                Entry::autoContinue,
                Entry::new);
    }

    public static final StreamCodec<ByteBuf, ClientboundAbilityDurationsPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundAbilityDurationsPayload::effects,
            ClientboundAbilityDurationsPayload::new);

    public static final ClientboundAbilityDurationsPayload INACTIVE =
            new ClientboundAbilityDurationsPayload(List.of());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
