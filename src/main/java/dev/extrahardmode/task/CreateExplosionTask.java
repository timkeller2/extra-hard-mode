package dev.extrahardmode.task;

import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.feature.Explosions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Delayed EHM explosion. TNT craters use 3/6/9 tick offsets. */
public final class CreateExplosionTask implements Runnable {
    private final ServerLevel level;
    private final Vec3 origin;
    private final ExplosionType type;
    private final Entity source;
    private int ticksLeft;

    public CreateExplosionTask(
            ServerLevel level, Vec3 origin, ExplosionType type, Entity source, int delayTicks) {
        this.level = level;
        this.origin = origin;
        this.type = type;
        this.source = source;
        this.ticksLeft = delayTicks;
    }

    public boolean tick() {
        if (--ticksLeft > 0) {
            return false;
        }
        run();
        return true;
    }

    @Override
    public void run() {
        Explosions.create(level, origin, type, source);
    }
}
