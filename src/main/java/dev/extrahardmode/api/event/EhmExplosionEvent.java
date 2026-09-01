package dev.extrahardmode.api.event;

import dev.extrahardmode.api.ExplosionType;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Fired from {@code ServerExplosion.explode} HEAD. Fabric API 0.158 has no explosion callback.
 * {@link #cancel()} skips block damage, fire, and extra TNT craters (claim mods).
 * Entity damage still runs.
 */
public final class EhmExplosionEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onEhmExplosion(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final ServerLevel level;
    private final Vec3 origin;
    private final ExplosionType type;
    private final Entity source;
    private float power;
    private boolean fire;
    private boolean worldDamage;
    private boolean canceled;

    public EhmExplosionEvent(
            ServerLevel level,
            Vec3 origin,
            ExplosionType type,
            float power,
            boolean fire,
            boolean worldDamage,
            Entity source) {
        this.level = level;
        this.origin = origin;
        this.type = type;
        this.power = power;
        this.fire = fire;
        this.worldDamage = worldDamage;
        this.source = source;
    }

    public ServerLevel level() {
        return level;
    }

    public Vec3 origin() {
        return origin;
    }

    public ExplosionType type() {
        return type;
    }

    public Entity source() {
        return source;
    }

    public float power() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public boolean fire() {
        return fire;
    }

    public void setFire(boolean fire) {
        this.fire = fire;
    }

    public boolean worldDamage() {
        return worldDamage;
    }

    public void setWorldDamage(boolean worldDamage) {
        this.worldDamage = worldDamage;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    @FunctionalInterface
    public interface Listener {
        void onEhmExplosion(EhmExplosionEvent event);
    }
}
