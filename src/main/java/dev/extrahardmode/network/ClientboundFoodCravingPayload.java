package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Meal history used to outline foods in inventories and chests. Hidden when hunger rules are off. */
public record ClientboundFoodCravingPayload(boolean active, int wellFed, List<String> history)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundFoodCravingPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("food_craving"));

    public static final StreamCodec<ByteBuf, ClientboundFoodCravingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientboundFoodCravingPayload::active,
            ByteBufCodecs.VAR_INT,
            ClientboundFoodCravingPayload::wellFed,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            ClientboundFoodCravingPayload::history,
            ClientboundFoodCravingPayload::new);

    public static final ClientboundFoodCravingPayload HIDDEN = new ClientboundFoodCravingPayload(false, 0, List.of());

    public static ClientboundFoodCravingPayload of(boolean active, int wellFed, List<String> history) {
        if (!active) {
            return HIDDEN;
        }
        List<String> meals = history == null || history.isEmpty() ? List.of() : List.copyOf(history);
        return new ClientboundFoodCravingPayload(true, Math.max(0, wellFed), meals);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
