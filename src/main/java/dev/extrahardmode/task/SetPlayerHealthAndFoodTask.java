package dev.extrahardmode.task;

import dev.extrahardmode.feature.WellFedRules;
import dev.extrahardmode.mixin.FoodDataAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/** Applies respawn health and hunger on the next tick so vanilla bars do not overwrite them. */
public final class SetPlayerHealthAndFoodTask implements Runnable {
    private final ServerPlayer player;
    private final float health;
    private final int food;
    private final boolean fillSaturation;

    public SetPlayerHealthAndFoodTask(ServerPlayer player, float health, int food, boolean fillSaturation) {
        this.player = player;
        this.health = health;
        this.food = food;
        this.fillSaturation = fillSaturation;
    }

    @Override
    public void run() {
        if (player.isRemoved() || !player.isAlive()) {
            return;
        }
        float max = player.getMaxHealth();
        player.setHealth(Math.min(Math.max(health, 1.0f), max));
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(Math.min(Math.max(food, 0), WellFedRules.FOOD_CAP));
        if (fillSaturation) {
            foodData.setSaturation(foodData.getFoodLevel());
            ((FoodDataAccess) foodData).tougher$setExhaustionLevel(0.0F);
        }
    }
}
