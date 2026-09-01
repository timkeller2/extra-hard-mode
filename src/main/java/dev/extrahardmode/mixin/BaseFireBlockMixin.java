package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Players;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void extrahardmode$igniteOnPunch(
            Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, Players.ID)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !player.getMainHandItem().isEmpty()) {
            return;
        }
        Players.igniteFromFire(serverPlayer);
    }
}
