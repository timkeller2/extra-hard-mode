package dev.extrahardmode.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccess {
    @Accessor("respawnStage")
    DragonRespawnStage extrahardmode$respawnStage();

    @Accessor("dragonKilled")
    boolean extrahardmode$dragonKilled();

    @Accessor("exitPortalLocation")
    BlockPos extrahardmode$exitPortalLocation();

    @Accessor("level")
    ServerLevel extrahardmode$level();

    @Invoker("createNewDragon")
    EnderDragon extrahardmode$createNewDragon();
}
