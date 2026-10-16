package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Active council bounty lines. Empty when nothing is being hunted. */
public record ClientboundCouncilBountyPayload(List<String> lines) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundCouncilBountyPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("council_bounty"));

    public static final StreamCodec<ByteBuf, ClientboundCouncilBountyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            ClientboundCouncilBountyPayload::lines,
            ClientboundCouncilBountyPayload::new);

    public static final ClientboundCouncilBountyPayload HIDDEN = new ClientboundCouncilBountyPayload(List.of());

    public static ClientboundCouncilBountyPayload from(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return HIDDEN;
        }
        return new ClientboundCouncilBountyPayload(List.copyOf(lines));
    }

    public boolean visible() {
        return lines != null && !lines.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
