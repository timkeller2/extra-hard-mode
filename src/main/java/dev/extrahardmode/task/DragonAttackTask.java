package dev.extrahardmode.task;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Extra dragon fireball aimed at a fighter (original DragonAttackTask). */
public final class DragonAttackTask {
    private final UUID dragonId;
    private final UUID playerId;
    private int ticksLeft;

    public DragonAttackTask(UUID dragonId, UUID playerId, int delayTicks) {
        this.dragonId = dragonId;
        this.playerId = playerId;
        this.ticksLeft = delayTicks;
    }

    public boolean tick(ServerLevel level) {
        if (--ticksLeft > 0) {
            return false;
        }
        run(level);
        return true;
    }

    public void run(ServerLevel level) {
        EnderDragon dragon = findDragon(level, dragonId);
        if (dragon == null || !dragon.isAlive()) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null || !player.isAlive() || player.level() != level) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 target;
        if (random.nextInt(100) < 20) {
            BlockPos high = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, dragon.blockPosition());
            target = Vec3.atCenterOf(high);
        } else {
            target = player.position();
        }
        Vec3 offset = target.add(random.nextInt(10) - 5, random.nextInt(3) - 1, random.nextInt(10) - 5);
        Vec3 start = dragon.head.position();
        Vec3 motion = offset.subtract(start);
        if (motion.lengthSqr() < 1.0E-6) {
            motion = player.position().subtract(start);
        }
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        DragonFireball fireball = new DragonFireball(level, dragon, motion.normalize());
        fireball.setPos(start);
        level.addFreshEntity(fireball);
    }

    static EnderDragon findDragon(ServerLevel level, UUID dragonId) {
        for (EnderDragon dragon : level.getDragons()) {
            if (dragon.getUUID().equals(dragonId)) {
                return dragon;
            }
        }
        return null;
    }
}
