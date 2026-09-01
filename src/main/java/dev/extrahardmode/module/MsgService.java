package dev.extrahardmode.module;

import dev.extrahardmode.command.EhmPermissions;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class MsgService {
    private MsgService() {}

    public static void actionBar(ServerPlayer player, PermissionNode<Boolean> silent, String key, String fallback) {
        if (player.checkPermission(silent, false)) {
            return;
        }
        player.sendOverlayMessage(Component.translatableWithFallback(key, fallback));
    }

    public static void lavaFizz(ServerLevel level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + level.getRandom().nextFloat() * 0.8F);
    }

    public static void noTorchesHere(ServerPlayer player, ServerLevel level, BlockPos pos, boolean fizz) {
        actionBar(
                player,
                EhmPermissions.SILENT_NO_TORCHES_HERE,
                "extrahardmode.message.no_torches_here",
                "There's not enough air flow down here for permanent flames. Use another method to light your way.");
        if (fizz) {
            lavaFizz(level, pos);
        }
    }

    public static void limitedTorchPlacement(ServerPlayer player, ServerLevel level, BlockPos pos, boolean fizz) {
        actionBar(
                player,
                EhmPermissions.SILENT_LIMITED_TORCH_PLACEMENT,
                "extrahardmode.message.limited_torch_placement",
                "It's too soft there to fasten a torch.");
        if (fizz) {
            lavaFizz(level, pos);
        }
    }

    public static void realisticBuilding(ServerPlayer player) {
        actionBar(
                player,
                EhmPermissions.SILENT_REALISTIC_BUILDING,
                "extrahardmode.message.realistic_building",
                "You can't build while in the air.");
    }
}
