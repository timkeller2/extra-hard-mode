package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @Shadow
    public abstract void findBlocks(
            java.util.function.Predicate<BlockState> predicate,
            BiConsumer<BlockPos, BlockState> output);

    @Inject(method = "findBlockLightSources", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$skipBurnableTorchesOnClient(
            BiConsumer<BlockPos, BlockState> output, CallbackInfo ci) {
        if (!(((Object) this) instanceof LevelChunk chunk)) {
            return;
        }
        Level level = chunk.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        ci.cancel();
        findBlocks(state -> state.getLightEmission() > 0 && !Torches.isBurnableTorch(state), output);
    }
}
