package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Active wise-teacher hunt lines. Empty when nothing is being hunted. */
public record ClientboundWiseLessonPayload(List<String> lines) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundWiseLessonPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("wise_lesson"));

    public static final StreamCodec<ByteBuf, ClientboundWiseLessonPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            ClientboundWiseLessonPayload::lines,
            ClientboundWiseLessonPayload::new);

    public static final ClientboundWiseLessonPayload HIDDEN = new ClientboundWiseLessonPayload(List.of());

    public static ClientboundWiseLessonPayload from(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return HIDDEN;
        }
        return new ClientboundWiseLessonPayload(List.copyOf(lines));
    }

    public boolean visible() {
        return lines != null && !lines.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
