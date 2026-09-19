package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {
    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    private void ehm$denyProjectileLight(
            Level level, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, Torches.ID)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        ServerPlayer player = projectile.getOwner() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (Torches.tryDenyCampfireLight(serverLevel, pos, player)) {
            ci.cancel();
        }
    }
}
