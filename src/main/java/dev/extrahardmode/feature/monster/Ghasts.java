package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Arrow damage 20%, XP ×10, drops ×5. */
public final class Ghasts implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("ghasts");
    public static final int DEFAULT_ARROW_DAMAGE_PERCENT = 20;
    public static final int DEFAULT_EXP_MULTIPLIER = 10;
    public static final int DEFAULT_DROPS_MULTIPLIER = 5;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(LootTableEvents.MODIFY_DROPS, ID, Ghasts::modifyDrops);
    }

    public static float scaleArrowDamage(float amount, int percent) {
        if (percent < 0) {
            return amount;
        }
        if (percent == 100) {
            return amount;
        }
        return amount * (percent / 100.0F);
    }

    public static int scaleExperience(int vanilla, int multiplier) {
        if (multiplier == 1) {
            return vanilla;
        }
        return Math.max(0, vanilla) * Math.max(0, multiplier);
    }

    public static float scaleIncoming(Ghast ghast, DamageSource source, float amount) {
        if (!(ghast.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return amount;
        }
        if (!(source.getDirectEntity() instanceof AbstractArrow)) {
            return amount;
        }
        Entity owner = source.getEntity();
        if (owner instanceof ServerPlayer player && EhmApi.playerBypasses(player)) {
            return amount;
        }
        return scaleArrowDamage(amount, ConfigManager.world(level).ghastArrowDamagePercent());
    }

    public static int scaleIncomingXp(Ghast ghast, ServerLevel level, int vanilla) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return vanilla;
        }
        return scaleExperience(vanilla, ConfigManager.world(level).ghastExpMultiplier());
    }

    private static void modifyDrops(
            net.minecraft.core.Holder<net.minecraft.world.level.storage.loot.LootTable> table,
            LootContext context,
            java.util.List<ItemStack> drops) {
        ServerLevel level = context.getLevel();
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Ghast ghast)) {
            return;
        }
        if (Boolean.TRUE.equals(ghast.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            drops.clear();
            return;
        }
        int multiplier = ConfigManager.world(level).ghastDropsMultiplier();
        if (multiplier == 1) {
            return;
        }
        for (ItemStack drop : drops) {
            drop.setCount(drop.getCount() * Math.max(0, multiplier));
        }
    }
}
