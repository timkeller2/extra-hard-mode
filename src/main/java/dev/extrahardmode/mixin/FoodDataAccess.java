package dev.extrahardmode.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Clears leftover exhaustion so a filled hunger bar stays full after a respawn. */
@Mixin(FoodData.class)
public interface FoodDataAccess {
    @Accessor("exhaustionLevel")
    void tougher$setExhaustionLevel(float exhaustionLevel);
}
