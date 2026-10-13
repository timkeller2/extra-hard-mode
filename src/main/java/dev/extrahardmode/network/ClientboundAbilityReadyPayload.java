package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Bottom-right mana ability icons, highest level first. */
public record ClientboundAbilityReadyPayload(List<Entry> abilities) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundAbilityReadyPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("ability_ready"));

    /**
     * {@code size} is the icon edge in GUI pixels. {@code level} is the effective level as an integer.
     * {@code cooldownSeconds} is 0 when the ability is ready. {@code learned} colors that level gold.
     */
    public record Entry(String ability, int size, int level, boolean learned, int cooldownSeconds) {
        static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                Entry::ability,
                ByteBufCodecs.VAR_INT,
                Entry::size,
                ByteBufCodecs.VAR_INT,
                Entry::level,
                ByteBufCodecs.BOOL,
                Entry::learned,
                ByteBufCodecs.VAR_INT,
                Entry::cooldownSeconds,
                Entry::new);
    }

    public static final StreamCodec<ByteBuf, ClientboundAbilityReadyPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundAbilityReadyPayload::abilities,
            ClientboundAbilityReadyPayload::new);

    public static final ClientboundAbilityReadyPayload EMPTY = new ClientboundAbilityReadyPayload(List.of());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
