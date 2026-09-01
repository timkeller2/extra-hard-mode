package dev.extrahardmode.task;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Repeating extra-attack volley. Heals when this fighter is defeated (original 25% restore).
 */
public final class DragonAttackPatternTask {
    public static final int PERIOD_TICKS = 20 * 30;

    private final UUID dragonId;
    private final UUID playerId;
    private int ticksLeft;

    public DragonAttackPatternTask(UUID dragonId, UUID playerId, int delayTicks) {
        this.dragonId = dragonId;
        this.playerId = playerId;
        this.ticksLeft = delayTicks;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID dragonId() {
        return dragonId;
    }

    /**
     * @return {@code true} when this task is finished (do not requeue)
     */
    public boolean tick(ServerLevel level, Scheduler scheduler) {
        if (--ticksLeft > 0) {
            return false;
        }
        EnderDragon dragon = DragonAttackTask.findDragon(level, dragonId);
        if (dragon == null || !dragon.isAlive()) {
            return true;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null || !player.isAlive() || player.level() != level) {
            scheduler.onFighterDefeated(level, dragon, playerId);
            return true;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            scheduler.scheduleAttack(new DragonAttackTask(dragonId, playerId, 20 * i + random.nextInt(20)));
        }
        ticksLeft = PERIOD_TICKS;
        return false;
    }

    public interface Scheduler {
        void scheduleAttack(DragonAttackTask task);

        void onFighterDefeated(ServerLevel level, EnderDragon dragon, UUID playerId);
    }
}
