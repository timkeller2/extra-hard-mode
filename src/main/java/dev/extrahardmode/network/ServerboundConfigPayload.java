package dev.extrahardmode.network;

import dev.extrahardmode.ExtraHardModeMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cloth Config save from the client. Applied on the logical server after an admin check. */
public record ServerboundConfigPayload(
        boolean enabledByDefault,
        boolean debug,
        int tutorialMaxShows,
        boolean applyWorld,
        boolean checkPermission,
        boolean creativeBypasses,
        boolean operatorsBypass,
        boolean limitedBuilding,
        boolean torchYDeny,
        boolean torchSoftDeny,
        int torchNoPlacementUnderY,
        boolean torchFizz,
        int torchBurnDays,
        boolean creeperTntWarning)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ExtraHardModeMod.id("config"));

    public static final StreamCodec<ByteBuf, ServerboundConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerboundConfigPayload::enabledByDefault,
            ByteBufCodecs.BOOL,
            ServerboundConfigPayload::debug,
            ByteBufCodecs.VAR_INT,
            ServerboundConfigPayload::tutorialMaxShows,
            WorldBits.STREAM_CODEC,
            ServerboundConfigPayload::worldBits,
            ServerboundConfigPayload::fromBits);

    private WorldBits worldBits() {
        return new WorldBits(
                applyWorld,
                checkPermission,
                creativeBypasses,
                operatorsBypass,
                limitedBuilding,
                torchYDeny,
                torchSoftDeny,
                torchNoPlacementUnderY,
                torchFizz,
                torchBurnDays,
                creeperTntWarning);
    }

    private static ServerboundConfigPayload fromBits(
            boolean enabledByDefault, boolean debug, int tutorialMaxShows, WorldBits world) {
        return new ServerboundConfigPayload(
                enabledByDefault,
                debug,
                tutorialMaxShows,
                world.applyWorld(),
                world.checkPermission(),
                world.creativeBypasses(),
                world.operatorsBypass(),
                world.limitedBuilding(),
                world.torchYDeny(),
                world.torchSoftDeny(),
                world.torchNoPlacementUnderY(),
                world.torchFizz(),
                world.torchBurnDays(),
                world.creeperTntWarning());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private record WorldBits(
            boolean applyWorld,
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean limitedBuilding,
            boolean torchYDeny,
            boolean torchSoftDeny,
            int torchNoPlacementUnderY,
            boolean torchFizz,
            int torchBurnDays,
            boolean creeperTntWarning) {
        static final StreamCodec<ByteBuf, WorldBits> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                WorldBits::applyWorld,
                ByteBufCodecs.BOOL,
                WorldBits::checkPermission,
                ByteBufCodecs.BOOL,
                WorldBits::creativeBypasses,
                ByteBufCodecs.BOOL,
                WorldBits::operatorsBypass,
                ByteBufCodecs.BOOL,
                WorldBits::limitedBuilding,
                ByteBufCodecs.BOOL,
                WorldBits::torchYDeny,
                ByteBufCodecs.BOOL,
                WorldBits::torchSoftDeny,
                ByteBufCodecs.VAR_INT,
                WorldBits::torchNoPlacementUnderY,
                ByteBufCodecs.VAR_INT,
                WorldBits::torchBurnDays,
                Sounds.STREAM_CODEC,
                WorldBits::sounds,
                WorldBits::withSounds);

        private Sounds sounds() {
            return new Sounds(torchFizz, creeperTntWarning);
        }

        private static WorldBits withSounds(
                boolean applyWorld,
                boolean checkPermission,
                boolean creativeBypasses,
                boolean operatorsBypass,
                boolean limitedBuilding,
                boolean torchYDeny,
                boolean torchSoftDeny,
                int torchNoPlacementUnderY,
                int torchBurnDays,
                Sounds sounds) {
            return new WorldBits(
                    applyWorld,
                    checkPermission,
                    creativeBypasses,
                    operatorsBypass,
                    limitedBuilding,
                    torchYDeny,
                    torchSoftDeny,
                    torchNoPlacementUnderY,
                    sounds.torchFizz(),
                    torchBurnDays,
                    sounds.creeperTntWarning());
        }

        private record Sounds(boolean torchFizz, boolean creeperTntWarning) {
            static final StreamCodec<ByteBuf, Sounds> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    Sounds::torchFizz,
                    ByteBufCodecs.BOOL,
                    Sounds::creeperTntWarning,
                    Sounds::new);
        }
    }
}
