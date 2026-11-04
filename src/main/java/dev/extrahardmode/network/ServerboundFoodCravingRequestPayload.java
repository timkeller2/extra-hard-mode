package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The client has no meal snapshot yet, so it asks the server to send one. */
public record ServerboundFoodCravingRequestPayload() implements CustomPacketPayload {
    public static final ServerboundFoodCravingRequestPayload INSTANCE = new ServerboundFoodCravingRequestPayload();

    public static final CustomPacketPayload.Type<ServerboundFoodCravingRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("food_craving_request"));

    public static final StreamCodec<ByteBuf, ServerboundFoodCravingRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
