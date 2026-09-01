package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public final class AntiFarming implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("anti_farming");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, AntiFarming::onUseBlock);
        bus.listen(ServerLivingEntityEvents.ALLOW_DEATH, ID, AntiFarming::onAllowDeath);
        bus.listen(LootTableEvents.MODIFY_DROPS, ID, AntiFarming::onModifyDrops);
        bus.listen(ServerEntityEvents.ALLOW_LOAD, ID, AntiFarming::onAllowLoad);
    }

    static InteractionResult onUseBlock(Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(server, ID)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        WorldConfig cfg = ConfigManager.world(server);
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        BlockPos clicked = hit.getBlockPos();
        Block clickedBlock = server.getBlockState(clicked).getBlock();
        if (cfg.noBonemealOnMushrooms()
                && item == Items.BONE_MEAL
                && (clickedBlock == Blocks.RED_MUSHROOM || clickedBlock == Blocks.BROWN_MUSHROOM)) {
            return InteractionResult.FAIL;
        }
        if (cfg.noFarmNetherWart() && item == Items.NETHER_WART) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    static boolean onAllowDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return true;
        }
        if (ConfigManager.world(level).animalXpNerf() && entity instanceof Animal) {
            entity.skipDropExperience();
        }
        return true;
    }

    static void onModifyDrops(
            net.minecraft.core.Holder<net.minecraft.world.level.storage.loot.LootTable> table,
            net.minecraft.world.level.storage.loot.LootContext context,
            java.util.List<ItemStack> drops) {
        ServerLevel level = context.getLevel();
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        WorldConfig cfg = ConfigManager.world(level);
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (cfg.ironGolemNerf() && entity instanceof IronGolem) {
            drops.clear();
            return;
        }
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (cfg.noFarmNetherWart() && state != null && state.getBlock() == Blocks.NETHER_WART) {
            if (entity instanceof ServerPlayer player && EhmApi.playerBypasses(player)) {
                return;
            }
            drops.clear();
            drops.add(new ItemStack(Items.NETHER_WART));
        }
    }

    static boolean onAllowLoad(Entity entity, ServerLevel level, EntitySpawnReason reason, boolean alreadyLoaded) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return true;
        }
        if (!ConfigManager.world(level).squidOceanOnly()) {
            return true;
        }
        if (alreadyLoaded || reason == EntitySpawnReason.LOAD || reason == EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        if (entity instanceof GlowSquid || !(entity instanceof Squid)) {
            return true;
        }
        if (reason != EntitySpawnReason.NATURAL) {
            return true;
        }
        return level.getBiome(entity.blockPosition()).is(BiomeTags.IS_OCEAN);
    }

    /**
     * Original {@code BlockModule.plantDies}: evaluated only at full growth.
     * Dark always dies; desert +50; unwatered +25. No separate needWater toggle.
     */
    public static int deathProbability(int lossRate, int skyLight, boolean desert, boolean infertileDeserts, int moisture) {
        if (skyLight < 10) {
            return 100;
        }
        int probability = lossRate;
        if (desert && infertileDeserts) {
            probability += 50;
        }
        if (moisture == 0) {
            probability += 25;
        }
        return Math.min(100, probability);
    }

    public static boolean isWeakFoodCrop(Block block) {
        return block == Blocks.WHEAT
                || block == Blocks.CARROTS
                || block == Blocks.POTATOES
                || block == Blocks.BEETROOTS;
    }

    public static boolean plantDies(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isWeakFoodCrop(state.getBlock())) {
            return false;
        }
        WorldConfig cfg = ConfigManager.world(level);
        if (!cfg.weakCrops()) {
            return false;
        }
        int sky = level.getLightEngine().getLayerListener(net.minecraft.world.level.LightLayer.SKY).getLightValue(pos);
        boolean desert = level.getBiome(pos).is(EhmTags.DESERT_INFERTILE);
        int moisture = 0;
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() == Blocks.FARMLAND) {
            moisture = below.getValue(FarmlandBlock.MOISTURE);
        }
        int chance = deathProbability(cfg.lossRate(), sky, desert, cfg.infertileDeserts(), moisture);
        return level.getRandom().nextInt(100) < chance;
    }

    public static void killCrop(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() == Blocks.FARMLAND) {
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static boolean isSnowCovered(ServerLevel level, BlockPos pos) {
        Block above = level.getBlockState(pos.above()).getBlock();
        return above == Blocks.SNOW || above == Blocks.SNOW_BLOCK;
    }

    public static boolean isInfertileDesert(ServerLevel level, BlockPos pos) {
        return ConfigManager.world(level).infertileDeserts() && level.getBiome(pos).is(EhmTags.DESERT_INFERTILE);
    }

    public static void notifyNoMelonSeeds(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            EhmNetworking.sendToast(serverPlayer, "no_crafting_melon_seeds");
        }
    }

    public static boolean isSeedResult(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS;
    }

    public static boolean shouldBlockCropGrowth(CropBlock crop, BlockState state) {
        return crop.isMaxAge(state) && isWeakFoodCrop(state.getBlock());
    }
}
