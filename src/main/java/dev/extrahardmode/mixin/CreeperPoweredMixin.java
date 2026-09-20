package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Creepers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Creeper.class)
public abstract class CreeperPoweredMixin implements Creepers.PoweredMutator {
    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_IS_POWERED;

    @Override
    public void tougher$setPowered(boolean powered) {
        ((Creeper) (Object) this).getEntityData().set(DATA_IS_POWERED, powered);
    }
}
