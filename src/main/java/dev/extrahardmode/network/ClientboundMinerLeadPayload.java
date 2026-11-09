package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Active old-miner ore leads. Empty when the player is not following a vein. */
public record ClientboundMinerLeadPayload(List<String> lines) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundMinerLeadPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("miner_lead"));

    public static final StreamCodec<ByteBuf, ClientboundMinerLeadPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            ClientboundMinerLeadPayload::lines,
            ClientboundMinerLeadPayload::new);

    public static final ClientboundMinerLeadPayload HIDDEN = new ClientboundMinerLeadPayload(List.of());

    public static ClientboundMinerLeadPayload from(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return HIDDEN;
        }
        return new ClientboundMinerLeadPayload(List.copyOf(lines));
    }

    public boolean visible() {
        return lines != null && !lines.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
