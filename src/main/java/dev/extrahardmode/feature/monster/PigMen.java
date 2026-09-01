package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Always-angry zombified piglins, nether wart drops, overworld lightning groups.
 */
public final class PigMen implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("pigmen");
    public static final int DEFAULT_DAMAGE_PERCENT = 70;
    public static final int DEFAULT_ELSEWHERE_WART_PERCENT = 25;
    public static final long ALWAYS_ANGRY_TICKS = Integer.MAX_VALUE;
    private static final double ANGER_RANGE = 32.0;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerEntityEvents.ENTITY_LOAD, ID, PigMen::onLoad);
        bus.listen(LootTableEvents.MODIFY_DROPS, ID, PigMen::modifyDrops);
    }

    /**
     * Original lightning table: 60% one, 20% two, 20% three ({@code nextInt(10)}).
     */
    public static int lightningCount(int roll0to9) {
        return switch (roll0to9) {
            case 0, 1 -> 2;
            case 2, 3 -> 3;
            default -> 1;
        };
    }

    public static float scalePlayerDamage(float amount, int percent) {
        if (percent <= 0 || percent == 100) {
            return amount;
        }
        return amount * (percent / 100.0F);
    }

    public static void anger(ZombifiedPiglin piglin) {
        piglin.setTimeToRemainAngry(ALWAYS_ANGRY_TICKS);
    }

    public static void keepAngry(ZombifiedPiglin piglin, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!ConfigManager.world(level).pigmenAlwaysAngry()) {
            return;
        }
        anger(piglin);
        if (piglin.getTarget() != null) {
            return;
        }
        Player nearest = level.getNearestPlayer(piglin, ANGER_RANGE);
        if (nearest == null || !piglin.canAttack(nearest)) {
            return;
        }
        piglin.setPersistentAngerTarget(EntityReference.of(nearest));
        piglin.setTarget(nearest);
    }

    public static boolean shouldStayAngry(ServerLevel level) {
        return WorldGate.isModuleActive(level, ID) && ConfigManager.world(level).pigmenAlwaysAngry();
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        if (entity instanceof ZombifiedPiglin piglin && config.pigmenAlwaysAngry()) {
            anger(piglin);
            return;
        }
        if (entity instanceof LightningBolt bolt && config.pigmenLightningSpawns()) {
            onLightning(bolt, level, config);
        }
    }

    static void onLightning(LightningBolt bolt, ServerLevel level, WorldConfig config) {
        if (Boolean.TRUE.equals(bolt.getAttachedOrElse(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.FALSE))) {
            return;
        }
        bolt.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
            return;
        }
        if (level.getBiome(bolt.blockPosition()).is(SpawnReplaceService.NO_SPAWN_REPLACEMENTS)) {
            return;
        }
        if (level.structureManager()
                .getStructureWithPieceAt(bolt.blockPosition(), SpawnReplaceService.NO_SPAWN_REPLACEMENT_STRUCTURES)
                .isValid()) {
            return;
        }
        int count = lightningCount(bolt.getRandom().nextInt(10));
        BlockPos pos = bolt.blockPosition();
        for (int i = 0; i < count; i++) {
            Entity spawned = EntityTypes.ZOMBIFIED_PIGLIN.spawn(level, pos, EntitySpawnReason.EVENT);
            if (spawned instanceof ZombifiedPiglin piglin) {
                piglin.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
                anger(piglin);
            }
        }
    }

    private static void modifyDrops(
            net.minecraft.core.Holder<net.minecraft.world.level.storage.loot.LootTable> table,
            LootContext context,
            java.util.List<ItemStack> drops) {
        ServerLevel level = context.getLevel();
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (level.dimension() != Level.NETHER) {
            return;
        }
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof ZombifiedPiglin piglin)) {
            return;
        }
        if (Boolean.TRUE.equals(piglin.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        boolean fortress = Blazes.inFortress(level, piglin.blockPosition());
        if (fortress && config.pigmenFortressNetherwart()) {
            drops.add(new ItemStack(Items.NETHER_WART));
            return;
        }
        if (!fortress
                && Blazes.percentChance(piglin.getRandom(), config.pigmenElsewhereNetherwartPercent())) {
            drops.add(new ItemStack(Items.NETHER_WART));
        }
    }

    public static float scaleIfPlayerHit(Player player, Entity attacker, float amount, int percent) {
        if (!(attacker instanceof ZombifiedPiglin)) {
            return amount;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return amount;
        }
        return scalePlayerDamage(amount, percent);
    }
}
