package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.BeeHoneyRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Slows the honey a returning bee adds. Shearing drops stay on hiveHoneycombCount. */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {
    @Redirect(
            method = "releaseOccupant",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean ehm$slowHoney(Level level, BlockPos pos, BlockState filled) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, AntiFarming.ID)) {
            return level.setBlockAndUpdate(pos, filled);
        }
        int before = BeehiveBlockEntity.getHoneyLevel(level.getBlockState(pos));
        int vanilla = BeehiveBlockEntity.getHoneyLevel(filled) - before;
        if (vanilla <= 0) {
            return level.setBlockAndUpdate(pos, filled);
        }
        int scaled = BeeHoneyRules.honeyIncrement(
                vanilla, ConfigManager.world(server).beeHoneyPercent(), level.getRandom().nextInt(100));
        if (scaled <= 0) {
            return false;
        }
        return level.setBlockAndUpdate(pos, filled);
    }
}
