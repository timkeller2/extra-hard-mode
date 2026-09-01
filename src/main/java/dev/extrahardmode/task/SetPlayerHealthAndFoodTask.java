package dev.extrahardmode.task;

import net.minecraft.server.level.ServerPlayer;

/** Applies reduced respawn health/food on the next tick so vanilla full bars don't overwrite us. */
public final class SetPlayerHealthAndFoodTask implements Runnable {
    private final ServerPlayer player;
    private final float health;
    private final int food;

    public SetPlayerHealthAndFoodTask(ServerPlayer player, float health, int food) {
        this.player = player;
        this.health = health;
        this.food = food;
    }

    @Override
    public void run() {
        if (player.isRemoved() || !player.isAlive()) {
            return;
        }
        float max = player.getMaxHealth();
        player.setHealth(Math.min(Math.max(health, 1.0f), max));
        player.getFoodData().setFoodLevel(Math.min(Math.max(food, 0), 20));
    }
}
