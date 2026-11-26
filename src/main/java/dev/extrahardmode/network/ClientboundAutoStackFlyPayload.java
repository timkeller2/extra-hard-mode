package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Stacks that just auto-stacked, so clients can show them flying to each chest. */
public record ClientboundAutoStackFlyPayload(int playerId, List<Flight> flights) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundAutoStackFlyPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("auto_stack_fly"));

    public record Flight(ItemStack stack, BlockPos chest) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Flight> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC,
                Flight::stack,
                buf(BlockPos.STREAM_CODEC),
                Flight::chest,
                Flight::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundAutoStackFlyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    buf(ByteBufCodecs.VAR_INT),
                    ClientboundAutoStackFlyPayload::playerId,
                    Flight.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    ClientboundAutoStackFlyPayload::flights,
                    ClientboundAutoStackFlyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> buf(StreamCodec<ByteBuf, T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buffer) {
                return codec.decode(buffer);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, T value) {
                codec.encode(buffer, value);
            }
        };
    }
}
