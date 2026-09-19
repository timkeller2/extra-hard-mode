package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AttachedStemBlock.class)
public abstract class AttachedStemBlockMixin extends VegetationBlock {
    protected AttachedStemBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel server && WorldGate.isModuleActive(server, AntiFarming.ID)) {
            AntiFarming.onStemVinePlaced(server, pos, oldState);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        if (WorldGate.isModuleActive(level, AntiFarming.ID)) {
            AntiFarming.onStemVineRemoved(level, pos);
        }
    }
}
