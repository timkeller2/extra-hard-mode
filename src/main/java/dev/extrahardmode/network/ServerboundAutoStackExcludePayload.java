package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Alt-click toggled a slot. {@code chest} is a slot in the open chest. Otherwise
 * {@code slot} is a player inventory index. {@code whole} is the second click of a
 * double-click and flips the rest of that inventory.
 */
public record ServerboundAutoStackExcludePayload(int slot, boolean chest, boolean whole)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundAutoStackExcludePayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("auto_stack_exclude"));

    public static final StreamCodec<ByteBuf, ServerboundAutoStackExcludePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ServerboundAutoStackExcludePayload::slot,
                    ByteBufCodecs.BOOL,
                    ServerboundAutoStackExcludePayload::chest,
                    ByteBufCodecs.BOOL,
                    ServerboundAutoStackExcludePayload::whole,
                    ServerboundAutoStackExcludePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
