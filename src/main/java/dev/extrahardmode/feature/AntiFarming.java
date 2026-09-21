package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.FarmlandData;
import dev.extrahardmode.world.StemFruitData;
import dev.extrahardmode.world.WorldGate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import dev.extrahardmode.network.ClientboundSoilLookPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.GlowSquid;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class AntiFarming implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("anti_farming");
    /** Vanilla adult cooldown after a successful breed, in ticks (5 minutes). */
    public static final int VANILLA_BREED_COOLDOWN_TICKS = 6000;
    public static final int DEFAULT_BREED_COOLDOWN_MULTIPLIER = 6;
    /** Vanilla egg timer uses random(this) + this ticks (5–10 minutes). */
    public static final int VANILLA_EGG_LAY_SPAN_TICKS = 6000;
    public static final int DEFAULT_EGG_LAY_TIME_MULTIPLIER = 2;
    public static final int VANILLA_HIVE_HONEYCOMB = 3;
    public static final int DEFAULT_HIVE_HONEYCOMB = 1;
    /** How long the hoe soil-modifier number stays in the world. */
    public static final int FLOAT_TEXT_TICKS = 40;
    public static final float FLOAT_TEXT_SCALE = 0.5F;
    public static final double FLOAT_TEXT_RISE = 0.02;
    public static final int FLOAT_TEXT_GREEN = 0x00FF00;
    public static final int FLOAT_TEXT_RED = 0xFF2020;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, AntiFarming::onUseBlock);
        bus.listen(UseItemCallback.EVENT, ID, AntiFarming::onUseItem);
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, AntiFarming::onCropBroken);
        bus.listen(ServerLivingEntityEvents.ALLOW_DEATH, ID, AntiFarming::onAllowDeath);
        bus.listen(LootTableEvents.MODIFY_DROPS, ID, AntiFarming::onModifyDrops);
        bus.listen(ServerEntityEvents.ALLOW_LOAD, ID, AntiFarming::onAllowLoad);
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            tickSoilLook(player);
        }
    }

    static InteractionResult onUseBlock(Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, ID)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            tryTellSeason(serverPlayer, server, hand);
            if (EhmApi.playerBypasses(serverPlayer)) {
                return InteractionResult.PASS;
            }
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
        if (cfg.noFarmNetherWart() && item == Items.NETHER_WART && isNetherWartPlaceAttempt(server, hit)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    static InteractionResult onUseItem(Player player, net.minecraft.world.level.Level level, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)) {
            return InteractionResult.PASS;
        }
        tryTellSeason(serverPlayer, server, hand);
        return InteractionResult.PASS;
    }

    static boolean tryTellSeason(ServerPlayer player, ServerLevel level, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().is(Items.CLOCK)) {
            return false;
        }
        Long tick = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_HANDLED_TICK, -1L);
        if (tick != null && tick == level.getGameTime()) {
            return true;
        }
        player.setAttached(EhmAttachments.EHM_ABILITY_HANDLED_TICK, level.getGameTime());
        double loss = currentSeasonalLossRate(level);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.crop_season_loss",
                "The current crop loss rate is %s%%.",
                CropGrowthRules.lossRateLabel(loss)));
        if (CropGrowthRules.beesInactive(loss)) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.crop_season_bees",
                    "Bees are staying in their hives."));
        }
        ManaAbilities.sendMeReport(player, player::sendSystemMessage);
        Achievements.sendWatchReport(player, player::sendSystemMessage);
        return true;
    }

    public static long overworldDay(ServerLevel level) {
        return CropGrowthRules.dayIndex(level.getOverworldClockTime());
    }

    public static double currentSeasonalLossRate(ServerLevel level) {
        WorldConfig config = ConfigManager.world(level);
        return CropGrowthRules.seasonalLossRate(
                config.lossRate(), overworldDay(level), config.changingSeasons());
    }

    public static int currentSeasonalDurationPercent(ServerLevel level, int baseDurationPercent) {
        return CropGrowthRules.durationPercent(
                baseDurationPercent,
                ConfigManager.world(level).lossRate(),
                currentSeasonalLossRate(level),
                0);
    }

    public static int currentDurationPercent(ServerLevel level, BlockPos pos, int baseDurationPercent) {
        return CropGrowthRules.durationPercent(
                baseDurationPercent,
                ConfigManager.world(level).lossRate(),
                currentSeasonalLossRate(level),
                soilModifier(level, pos));
    }

    static boolean onAllowDeath(
            net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.damagesource.DamageSource source,
            float amount) {
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return true;
        }
        if (ConfigManager.world(level).animalXpNerf() && entity instanceof Animal) {
            entity.skipDropExperience();
        }
        return true;
    }

    static boolean isNetherWartPlaceAttempt(ServerLevel level, BlockHitResult hit) {
        BlockPos clicked = hit.getBlockPos();
        Block clickedBlock = level.getBlockState(clicked).getBlock();
        if (clickedBlock == Blocks.SOUL_SAND) {
            return true;
        }
        BlockPos placed = clicked.relative(hit.getDirection());
        return level.getBlockState(placed.below()).getBlock() == Blocks.SOUL_SAND
                && level.getBlockState(placed).canBeReplaced();
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
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (cfg.ironGolemNerf() && entity instanceof IronGolem) {
            drops.clear();
            return;
        }
        if (entity != null) {
            Entity killer = context.getOptional(LootContextParams.LAST_DAMAGE_PLAYER);
            if (!(killer instanceof ServerPlayer player && EhmApi.playerBypasses(player))) {
                reduceSeasonalMeat(entity, drops, currentSeasonalLossRate(level));
            }
        }
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state != null
                && isFarmHarvestBlock(state.getBlock())
                && !(entity instanceof Player)) {
            net.minecraft.world.phys.Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
            if (origin != null) {
                ItemStack tool = ItemStack.EMPTY;
                Object rawTool = context.getOptional(LootContextParams.TOOL);
                if (rawTool instanceof ItemStack stack) {
                    tool = stack;
                }
                onCropHarvested(level, BlockPos.containing(origin), tool, state.getBlock());
            }
        }
        if (state != null && isStemVine(state.getBlock())) {
            if (entity instanceof ServerPlayer player && EhmApi.playerBypasses(player)) {
                return;
            }
            drops.removeIf(AntiFarming::isSeedResult);
            return;
        }
        if (state != null && state.is(BlockTags.LEAVES)) {
            if (!(entity instanceof ServerPlayer player && EhmApi.playerBypasses(player))) {
                Random rng = new Random(level.getRandom().nextLong());
                drops.removeIf(stack -> {
                    if (!stack.is(ItemTags.SAPLINGS)) {
                        return false;
                    }
                    int keep = CropGrowthRules.scaledDropCount(
                            stack.getCount(), CropGrowthRules.SAPLING_DROP_KEEP_PERCENT, rng);
                    if (keep <= 0) {
                        return true;
                    }
                    stack.setCount(keep);
                    return false;
                });
            }
        }
        if (isHiveHoneycombHarvest(table, state)) {
            if (!(entity instanceof ServerPlayer player && EhmApi.playerBypasses(player))) {
                int want = hiveHoneycombCount(cfg.hiveHoneycombCount());
                drops.removeIf(stack -> {
                    if (stack.getItem() != Items.HONEYCOMB) {
                        return false;
                    }
                    if (want <= 0) {
                        return true;
                    }
                    stack.setCount(want);
                    return false;
                });
            }
        }
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

    /** Adult breeding cooldown in ticks. Multiplier 1 is vanilla; 4 is four times as long. */
    public static int breedCooldownTicks(int vanillaTicks, int multiplier) {
        return scaleTicks(vanillaTicks, multiplier);
    }

    /** Chicken egg delay. Multiplier 1 is vanilla; 2 is twice as long. */
    public static int eggLayTime(int vanillaTicks, int multiplier) {
        return scaleTicks(vanillaTicks, multiplier);
    }

    public static int scaleTicks(int vanillaTicks, int multiplier) {
        int ticks = Math.max(0, vanillaTicks);
        int factor = Math.max(1, multiplier);
        long scaled = (long) ticks * (long) factor;
        return scaled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }

    /**
     * Original {@code BlockModule.plantDies}: evaluated only at full growth.
     * Dark always dies; desert +50; unwatered +25. No separate needWater toggle.
     * {@code soilModifier} is added to the loss rate (positive is worse).
     */
    public static int deathProbability(int lossRate, int skyLight, boolean desert, boolean infertileDeserts, int moisture) {
        return deathProbability(lossRate, skyLight, desert, infertileDeserts, moisture, 0);
    }

    public static int deathProbability(
            int lossRate, int skyLight, boolean desert, boolean infertileDeserts, int moisture, int soilModifier) {
        if (skyLight < 10) {
            return 100;
        }
        int probability = lossRate + soilModifier;
        if (desert && infertileDeserts) {
            probability += 50;
        }
        if (moisture == 0) {
            probability += 25;
        }
        return CropGrowthRules.clampLossChance(probability);
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
        int seasonalLoss = (int) Math.round(currentSeasonalLossRate(level));
        int chance = deathProbability(
                seasonalLoss, sky, desert, cfg.infertileDeserts(), moisture, soilModifier(level, pos));
        return CropGrowthRules.rollLoss(chance, new Random(level.getRandom().nextLong()));
    }

    /** Kill a food crop that just reached full growth. Let it grow does not call this. */
    public static boolean tryKillIfMature(ServerLevel level, BlockPos pos, BlockState state) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        if (!(state.getBlock() instanceof CropBlock crop) || !shouldBlockCropGrowth(crop, state)) {
            return false;
        }
        if (!plantDies(level, pos, state)) {
            return false;
        }
        killCrop(level, pos);
        return true;
    }

    public static int soilModifier(ServerLevel level, BlockPos cropPos) {
        return FarmlandData.of(level).modifier(soilForPlant(level, cropPos));
    }

    public static void onSoilTilled(ServerLevel level, BlockPos soil, ItemStack hoe) {
        FarmlandData data = FarmlandData.of(level);
        int before = data.modifier(soil);
        int modifier;
        if (!data.hasModifier(soil)) {
            int d10 = 1 + level.getRandom().nextInt(CropGrowthRules.FIRST_TILL_DIE);
            modifier = CropGrowthRules.firstTillModifier(
                    d10, countWaterSources(level, soil, CropGrowthRules.FIRST_TILL_WATER_RANGE));
        } else {
            int quality = AbilityRules.growHoeBonus(itemId(hoe));
            modifier = CropGrowthRules.afterHoeWork(before, quality);
        }
        data.setModifier(soil, modifier);
    }

    static int countWaterSources(ServerLevel level, BlockPos origin, int range) {
        int r = Math.max(0, range);
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    var fluid = level.getFluidState(origin.offset(dx, dy, dz));
                    if (fluid.isSource() && fluid.is(FluidTags.WATER)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    public static void onCropHarvested(ServerLevel level, BlockPos cropPos, ItemStack tool) {
        onCropHarvested(level, cropPos, tool, level.getBlockState(cropPos).getBlock());
    }

    public static void onCropHarvested(ServerLevel level, BlockPos cropPos, ItemStack tool, Block harvested) {
        BlockPos soil = soilForPlant(level, cropPos, harvested);
        FarmlandData data = FarmlandData.of(level);
        int before = data.modifier(soil);
        int next;
        boolean hoe = tool != null && !tool.isEmpty() && tool.is(ItemTags.HOES);
        if (hoe) {
            next = CropGrowthRules.afterHoeWork(before, AbilityRules.growHoeBonus(itemId(tool)));
        } else {
            next = CropGrowthRules.afterHandHarvest(before);
        }
        data.setModifier(soil, next);
        data.setLastCrop(soil, cropKey(harvested));
    }

    public static void onCropPlanted(BlockPlaceContext context, Block block) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (context.getPlayer() instanceof ServerPlayer player && EhmApi.playerBypasses(player)) {
            return;
        }
        if (!isPlantedCrop(block)) {
            return;
        }
        BlockPos plant = context.getClickedPos();
        if (block instanceof SugarCaneBlock
                && level.getBlockState(plant.below()).getBlock() instanceof SugarCaneBlock) {
            return;
        }
        BlockPos soil = soilForPlant(level, plant, block);
        String id = cropKey(block);
        FarmlandData data = FarmlandData.of(level);
        if (CropGrowthRules.isSameCrop(data.lastCrop(soil), id)) {
            int before = data.modifier(soil);
            int next = CropGrowthRules.afterSameCropReplant(before);
            data.setModifier(soil, next);
        }
        data.setLastCrop(soil, id);
    }

    public static void onLetItGrow(ServerLevel level, BlockPos plant) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        BlockPos soil = soilForPlant(level, plant);
        FarmlandData data = FarmlandData.of(level);
        int before = data.modifier(soil);
        int next = CropGrowthRules.afterGrowWork(before);
        data.setModifier(soil, next);
    }

    public static void onBoneMeal(ServerLevel level, BlockPos plant) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!GrowPlants.isPlant(level, plant)) {
            return;
        }
        BlockPos soil = soilForPlant(level, plant);
        FarmlandData data = FarmlandData.of(level);
        int before = data.modifier(soil);
        int next = CropGrowthRules.afterBoneMeal(before);
        data.setModifier(soil, next);
    }

    static BlockPos soilForPlant(ServerLevel level, BlockPos plant) {
        return soilForPlant(level, plant, level.getBlockState(plant).getBlock());
    }

    static BlockPos soilForPlant(ServerLevel level, BlockPos plant, Block harvested) {
        BlockPos below = plant.below();
        Block column = harvested;
        if (!(column instanceof SugarCaneBlock || column instanceof CactusBlock)) {
            Block standing = level.getBlockState(below).getBlock();
            if (standing instanceof SugarCaneBlock || standing instanceof CactusBlock) {
                column = standing;
            } else {
                return below;
            }
        }
        while (level.getBlockState(below).getBlock() == column) {
            below = below.below();
        }
        return below;
    }

    static void tickSoilLook(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            sendSoilLook(player, null);
            return;
        }
        if (EhmApi.playerBypasses(player)) {
            sendSoilLook(player, null);
            return;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) {
            sendSoilLook(player, null);
            return;
        }
        BlockPos soil = soilForLook(level, blockHit.getBlockPos());
        FarmlandData data = FarmlandData.of(level);
        if (!data.hasModifier(soil)) {
            sendSoilLook(player, null);
            return;
        }
        sendSoilLook(player, CropGrowthRules.displayedModifier(data.modifier(soil)));
    }

    static BlockPos soilForLook(ServerLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block == Blocks.FARMLAND || isPlantedCrop(block) || isFarmHarvestBlock(block)) {
            if (block == Blocks.FARMLAND) {
                return pos;
            }
            return soilForPlant(level, pos, block);
        }
        return pos;
    }

    static void sendSoilLook(ServerPlayer player, Integer displayed) {
        String key = displayed == null ? "" : Integer.toString(displayed);
        String last = player.getAttachedOrElse(EhmAttachments.EHM_SOIL_LOOK, "");
        if (key.equals(last)) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_SOIL_LOOK, key);
        EhmNetworking.sendSoilLook(
                player,
                displayed == null
                        ? ClientboundSoilLookPayload.HIDDEN
                        : new ClientboundSoilLookPayload(true, displayed));
    }

    static void onCropBroken(
            net.minecraft.world.level.Level world,
            Player player,
            BlockPos pos,
            BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        if (!isFarmHarvestBlock(state.getBlock())) {
            return;
        }
        onCropHarvested(level, pos, player.getMainHandItem(), state.getBlock());
    }

    public static boolean isFarmHarvestBlock(Block block) {
        return block instanceof CropBlock
                || block instanceof StemBlock
                || block instanceof AttachedStemBlock
                || block instanceof SugarCaneBlock;
    }

    public static boolean isPlantedCrop(Block block) {
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof SugarCaneBlock;
    }

    static String cropKey(Block block) {
        if (block == null) {
            return "";
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "" : id.toString();
    }

    public static boolean sugarCaneWouldGrowSecondSegment(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(SugarCaneBlock.AGE)) {
            return false;
        }
        int age = state.getValue(SugarCaneBlock.AGE);
        boolean airAbove = level.isEmptyBlock(pos.above());
        boolean belowIsCane = level.getBlockState(pos.below()).getBlock() == Blocks.SUGAR_CANE;
        return CropGrowthRules.sugarCaneGrowsSecondSegment(age, airAbove, belowIsCane);
    }

    public static boolean sugarCaneBecomesWeed(ServerLevel level, BlockPos pos) {
        int chance = CropGrowthRules.lossChance(currentSeasonalLossRate(level), soilModifier(level, pos));
        return CropGrowthRules.rollLoss(chance, new Random(level.getRandom().nextLong()));
    }

    /** Kill a 1-high cane that would place its second segment. Let it grow does not call this. */
    public static boolean tryWeedSugarCaneOnSecondSegment(ServerLevel level, BlockPos pos, BlockState state) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        if (!sugarCaneWouldGrowSecondSegment(level, pos, state)) {
            return false;
        }
        if (!sugarCaneBecomesWeed(level, pos)) {
            return false;
        }
        killCrop(level, pos);
        return true;
    }

    static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return stack.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
    }

    public static void killCrop(ServerLevel level, BlockPos pos) {
        boolean vine = isStemVine(level.getBlockState(pos).getBlock());
        BlockPos soil = pos.below();
        boolean farmland = level.getBlockState(soil).getBlock() == Blocks.FARMLAND;
        // Replace the plant first so turning farmland to dirt does not pop it as air.
        level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (farmland) {
            level.setBlock(soil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (vine) {
            StemFruitData.of(level).remove(pos);
        }
    }

    public static boolean isStemVine(Block block) {
        return block instanceof StemBlock || block instanceof AttachedStemBlock;
    }

    public static void onStemVinePlaced(ServerLevel level, BlockPos pos, BlockState oldState) {
        if (isStemVine(oldState.getBlock())) {
            return;
        }
        StemFruitData.of(level).remove(pos);
    }

    public static void onStemVineRemoved(ServerLevel level, BlockPos pos) {
        if (isStemVine(level.getBlockState(pos).getBlock())) {
            return;
        }
        StemFruitData.of(level).remove(pos);
    }

    /**
     * After a pumpkin or melon appears: first fruit never kills the vine, then
     * +5% weed chance per fruit already grown.
     */
    public static void onStemFruitAppeared(ServerLevel level, BlockPos pos) {
        StemFruitData data = StemFruitData.of(level);
        int grown = data.fruitsGrown(pos);
        if (CropGrowthRules.stemVineDies(grown, new java.util.Random(level.getRandom().nextLong()))) {
            killCrop(level, pos);
            return;
        }
        data.setFruitsGrown(pos, grown + 1);
    }

    public static boolean isSnowCovered(ServerLevel level, BlockPos pos) {
        Block above = level.getBlockState(pos.above()).getBlock();
        return above == Blocks.SNOW || above == Blocks.SNOW_BLOCK;
    }

    public static boolean isInfertileDesert(ServerLevel level, BlockPos pos) {
        return ConfigManager.world(level).infertileDeserts() && level.getBiome(pos).is(EhmTags.DESERT_INFERTILE);
    }

    public static void notifyNoMelonSeeds(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Map<String, Integer> shown = new HashMap<>(
                serverPlayer.getAttachedOrElse(EhmAttachments.EHM_TUTORIAL, Map.of()));
        if (shown.getOrDefault("no_crafting_melon_seeds", 0) > 0) {
            return;
        }
        shown.put("no_crafting_melon_seeds", 1);
        serverPlayer.setAttached(EhmAttachments.EHM_TUTORIAL, shown);
        EhmNetworking.sendToast(serverPlayer, "no_crafting_melon_seeds");
    }

    public static int hiveHoneycombCount(int configured) {
        return Math.clamp(configured, 0, 64);
    }

    static boolean isHiveHoneycombHarvest(
            net.minecraft.core.Holder<net.minecraft.world.level.storage.loot.LootTable> table, BlockState state) {
        if (table.unwrapKey().map(key -> "harvest/beehive".equals(key.identifier().getPath())).orElse(false)) {
            return true;
        }
        return state != null && state.getBlock() instanceof BeehiveBlock;
    }

    public static boolean isSeedResult(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS;
    }

    public static boolean shouldBlockCropGrowth(CropBlock crop, BlockState state) {
        return crop.isMaxAge(state) && isWeakFoodCrop(state.getBlock());
    }

    static void reduceSeasonalMeat(Entity entity, java.util.List<ItemStack> drops, double seasonalLoss) {
        int reduce;
        if (entity instanceof AbstractCow || entity instanceof Pig || entity instanceof Sheep) {
            reduce = CropGrowthRules.largeAnimalMeatReduce(seasonalLoss);
        } else if (entity instanceof Chicken) {
            reduce = CropGrowthRules.chickenMeatReduce(seasonalLoss);
        } else {
            return;
        }
        if (reduce <= 0) {
            return;
        }
        reduceMeatDrops(drops, reduce, meatItemsFor(entity));
    }

    static Item[] meatItemsFor(Entity entity) {
        if (entity instanceof AbstractCow) {
            return new Item[] {Items.BEEF, Items.COOKED_BEEF};
        }
        if (entity instanceof Pig) {
            return new Item[] {Items.PORKCHOP, Items.COOKED_PORKCHOP};
        }
        if (entity instanceof Sheep) {
            return new Item[] {Items.MUTTON, Items.COOKED_MUTTON};
        }
        if (entity instanceof Chicken) {
            return new Item[] {Items.CHICKEN, Items.COOKED_CHICKEN};
        }
        return new Item[0];
    }

    static void reduceMeatDrops(java.util.List<ItemStack> drops, int reduce, Item... meats) {
        if (drops == null || meats == null || meats.length == 0 || reduce <= 0) {
            return;
        }
        int[] counts = new int[drops.size()];
        for (int i = 0; i < drops.size(); i++) {
            ItemStack stack = drops.get(i);
            counts[i] = isMeatItem(stack.getItem(), meats) ? stack.getCount() : 0;
        }
        CropGrowthRules.reduceStackCounts(counts, reduce);
        for (int i = drops.size() - 1; i >= 0; i--) {
            if (!isMeatItem(drops.get(i).getItem(), meats)) {
                continue;
            }
            if (counts[i] <= 0) {
                drops.remove(i);
            } else {
                drops.get(i).setCount(counts[i]);
            }
        }
    }

    static boolean isMeatItem(Item item, Item... meats) {
        for (Item meat : meats) {
            if (item == meat) {
                return true;
            }
        }
        return false;
    }
}
