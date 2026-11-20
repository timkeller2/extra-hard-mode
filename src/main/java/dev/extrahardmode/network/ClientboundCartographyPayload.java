package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Active cartographer marks. Empty when the player is not walking to a place. */
public record ClientboundCartographyPayload(List<String> lines) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundCartographyPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("cartography"));

    public static final StreamCodec<ByteBuf, ClientboundCartographyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            ClientboundCartographyPayload::lines,
            ClientboundCartographyPayload::new);

    public static final ClientboundCartographyPayload HIDDEN = new ClientboundCartographyPayload(List.of());

    public static ClientboundCartographyPayload from(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return HIDDEN;
        }
        return new ClientboundCartographyPayload(List.copyOf(lines));
    }

    public boolean visible() {
        return lines != null && !lines.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
