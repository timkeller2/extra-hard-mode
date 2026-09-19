package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CouncilMissionRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Active council bounty HUD. Hidden when {@code visible} is false. */
public record ClientboundCouncilBountyPayload(boolean visible, String label, int kills, int target)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundCouncilBountyPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("council_bounty"));

    public static final StreamCodec<ByteBuf, ClientboundCouncilBountyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundCouncilBountyPayload::visible,
            ByteBufCodecs.STRING_UTF8,
            ClientboundCouncilBountyPayload::label,
            ByteBufCodecs.VAR_INT,
            ClientboundCouncilBountyPayload::kills,
            ByteBufCodecs.VAR_INT,
            ClientboundCouncilBountyPayload::target,
            ClientboundCouncilBountyPayload::new);

    public static final ClientboundCouncilBountyPayload HIDDEN =
            new ClientboundCouncilBountyPayload(false, "", 0, 0);

    public static ClientboundCouncilBountyPayload from(CouncilMissionRules.Mission mission) {
        if (mission == null || !mission.active()) {
            return HIDDEN;
        }
        return new ClientboundCouncilBountyPayload(true, mission.label(), mission.kills(), mission.target());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
