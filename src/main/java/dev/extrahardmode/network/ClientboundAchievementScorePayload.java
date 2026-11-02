package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Achievements earned and the victory goal. A goal of 0 hides the counter. */
public record ClientboundAchievementScorePayload(int earned, int required) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundAchievementScorePayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("achievement_score"));

    public static final StreamCodec<ByteBuf, ClientboundAchievementScorePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundAchievementScorePayload::earned,
            ByteBufCodecs.VAR_INT,
            ClientboundAchievementScorePayload::required,
            ClientboundAchievementScorePayload::new);

    public boolean visible() {
        return required > 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
