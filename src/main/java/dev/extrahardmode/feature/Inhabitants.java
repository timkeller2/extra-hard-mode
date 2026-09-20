package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Scored player homes can attract one traveler. Default off.
 */
public final class Inhabitants implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("inhabitants");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, Inhabitants::onUseBlock);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Inhabitants::onDeath);
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, Inhabitants::onDamage);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20L == 0L) {
            long day = AntiFarming.overworldDay(level);
            for (ServerPlayer player : level.players()) {
                CouncilMissions.tick(player, day);
            }
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        tickHomes(level);
        if (level.getGameTime() % 20L == 0L) {
            keepNearHome(level);
            visitNearbyHomes(level);
        }
    }

    static InteractionResult onUseBlock(
            Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || !serverPlayer.getMainHandItem().is(Items.CLOCK)
                || !server.getBlockState(hit.getBlockPos()).is(BlockTags.BEDS)) {
            return InteractionResult.PASS;
        }
        inspect(serverPlayer, server, hit.getBlockPos());
        return InteractionResult.PASS;
    }

    static void inspect(ServerPlayer player, ServerLevel level, BlockPos bed) {
        WorldConfig cfg = ConfigManager.world(level);
        long day = AntiFarming.overworldDay(level);
        InhabitantData data = InhabitantData.of(level);
        List<BlockPos> occupied = occupiedBeds(level);
        ResidenceScan.Result result =
                ResidenceScan.inspect(level, bed, occupied, cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
        catchUpArrival(
                level,
                data,
                result,
                day,
                occupied,
                cfg,
                level.getRandom(),
                CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level)),
                player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.inhabitant_inspect",
                "%s",
                Component.literal(InhabitantRules.inspectFallback(result.gates()))));
        InhabitantData.Home home = data.get(ResidenceScan.homeId(result.bed()));
        if (home != null && home.living().isPresent()) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.inhabitant_lives_here",
                    "%s the %s lives here.",
                    Component.literal(home.name()),
                    Component.literal(InhabitantRules.specialtyFallback(home.specialty()))));
        }
    }

    static void tickHomes(ServerLevel level) {
        long day = AntiFarming.overworldDay(level);
        InhabitantData data = InhabitantData.of(level);
        maintainOccupied(level, data, day);
        if (!InhabitantRules.shouldAttemptDawn(data.lastDawnDay(), day, hasSurvivalPlayer(level))) {
            return;
        }
        data.setLastDawnDay(day);
        restockDue(level, data, day);
        tryArrive(level, data, day);
    }

    static void maintainOccupied(ServerLevel level, InhabitantData data, long day) {
        WorldConfig cfg = ConfigManager.world(level);
        for (InhabitantData.Home home : List.copyOf(data.homes())) {
            if (home.living().isEmpty()) {
                continue;
            }
            Entity entity = level.getEntityInAnyDimension(home.living().get());
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            ResidenceScan.Result result = ResidenceScan.inspect(
                    level, home.bed(), occupiedBeds(level, home.id()), cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
            boolean ok = result.gates().eligible();
            if (ok) {
                if (home.uneasy() || home.leaveAfterDay() >= 0L || home.score() != result.gates().score()) {
                    data.put(home.withScore(result.gates().score()).withLeave(-1L, false));
                }
                continue;
            }
            if (!home.uneasy()) {
                complain(level, entity, home);
                data.put(home.withLeave(InhabitantRules.leaveAfterDay(day), true));
                continue;
            }
            if (InhabitantRules.shouldLeave(day, home.leaveAfterDay())) {
                leave(level, data, home, entity);
            }
        }
    }

    static void tryArrive(ServerLevel level, InhabitantData data, long day) {
        WorldConfig cfg = ConfigManager.world(level);
        boolean blight = CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level));
        Set<String> seen = new HashSet<>();
        List<BlockPos> occupied = occupiedBeds(level);
        RandomSource random = level.getRandom();
        for (BlockPos pos : bedHeadsInTickingChunks(level)) {
            considerArrival(level, data, pos, day, occupied, seen, cfg, random, blight, null);
        }
    }

    static void visitNearbyHomes(ServerLevel level) {
        if (!hasSurvivalPlayer(level)) {
            return;
        }
        long day = AntiFarming.overworldDay(level);
        InhabitantData data = InhabitantData.of(level);
        WorldConfig cfg = ConfigManager.world(level);
        boolean blight = CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level));
        Set<String> seen = new HashSet<>();
        List<BlockPos> occupied = occupiedBeds(level);
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            for (BlockPos pos : bedHeadsNear(level, player.blockPosition(), InhabitantRules.VISIT_RADIUS)) {
                considerArrival(level, data, pos, day, occupied, seen, cfg, random, blight, player);
            }
        }
    }

    static void considerArrival(
            ServerLevel level,
            InhabitantData data,
            BlockPos pos,
            long day,
            List<BlockPos> occupied,
            Set<String> seen,
            WorldConfig cfg,
            RandomSource random,
            boolean blight,
            ServerPlayer visitor) {
        InhabitantData.Home existing = data.get(ResidenceScan.homeId(pos));
        if (existing != null && existing.living().isPresent()) {
            seen.add(existing.id());
            return;
        }
        if (existing != null && existing.lastRollDay() >= 0L && existing.lastRollDay() >= day) {
            seen.add(existing.id());
            return;
        }
        ResidenceScan.Result result =
                ResidenceScan.inspect(level, pos, occupied, cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
        String id = ResidenceScan.homeId(result.bed());
        if (!seen.add(id)) {
            return;
        }
        catchUpArrival(level, data, result, day, occupied, cfg, random, blight, visitor);
    }

    /**
     * Stamp a newly eligible empty home, or roll once per missed day since the
     * last stamp, then set the stamp to today.
     */
    static void catchUpArrival(
            ServerLevel level,
            InhabitantData data,
            ResidenceScan.Result result,
            long day,
            List<BlockPos> occupied,
            WorldConfig cfg,
            RandomSource random,
            boolean blight,
            ServerPlayer visitor) {
        String id = ResidenceScan.homeId(result.bed());
        InhabitantData.Home existing = data.get(id);
        if (existing != null && existing.living().isPresent()) {
            return;
        }
        if (existing != null && InhabitantRules.spawnBlocked(day, existing.emptyUntilDay())) {
            data.put(existing.withLastRoll(day));
            return;
        }
        if (!result.gates().eligible()) {
            if (existing != null && existing.living().isEmpty()) {
                data.put(existing.withLastRoll(-1L));
            }
            return;
        }
        if (existing == null) {
            data.put(new InhabitantData.Home(
                    id, result.bed(), result.gates().score(), "", "", Optional.empty(), -1L, -1L, false, -1L, day));
            awardFirstHomeBiome(level, result.bed(), result.gates().score(), visitor);
            return;
        }
        int missed = InhabitantRules.missedArrivalRolls(existing.lastRollDay(), day);
        data.put(existing.withScore(result.gates().score()).withLastRoll(day));
        if (missed <= 0) {
            return;
        }
        int chance = InhabitantRules.spawnChancePercent(
                result.gates().score(),
                cfg.inhabitantMinScore(),
                cfg.inhabitantBaseChancePercent(),
                cfg.inhabitantChancePerPoint(),
                cfg.inhabitantMaxChancePercent(),
                blight);
        for (int i = 0; i < missed; i++) {
            if (!InhabitantRules.spawnRoll(random.nextInt(100), chance)) {
                continue;
            }
            spawnResident(level, data, result, random);
            occupied.add(result.bed());
            return;
        }
    }

    static void awardFirstHomeBiome(ServerLevel level, BlockPos bed, int houseScore, ServerPlayer preferred) {
        ServerPlayer player = preferred;
        if (player == null || Achievements.skipPlayer(player)) {
            player = nearestSurvivalPlayer(level, bed);
        }
        Exploration.maybeAwardFirstHome(player, level, bed, houseScore);
    }

    static ServerPlayer nearestSurvivalPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer best = null;
        double bestDist = (double) InhabitantRules.VISIT_RADIUS * InhabitantRules.VISIT_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (Achievements.skipPlayer(player)) {
                continue;
            }
            double dist = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist <= bestDist) {
                best = player;
                bestDist = dist;
            }
        }
        return best;
    }

    static void spawnResident(
            ServerLevel level, InhabitantData data, ResidenceScan.Result result, RandomSource random) {
        BlockPos spawn = ResidenceScan.standableNear(level, result.bed());
        if (spawn == null) {
            return;
        }
        Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
        if (villager == null) {
            return;
        }
        villager.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        String specialty = InhabitantRules.pickSpecialty(
                result.amenities(), result.gates().score(), random.nextInt(), data.spawnedSpecialties());
        String name = InhabitantRules.pickName(random.nextInt());
        stamp(villager, ResidenceScan.homeId(result.bed()), specialty, name);
        applyOffers(
                villager,
                specialty,
                result.amenities(),
                CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level)),
                random,
                result.gates().score());
        if (!level.addFreshEntity(villager)) {
            villager.discard();
            return;
        }
        data.markSpawned(specialty);
        long today = AntiFarming.overworldDay(level);
        data.put(new InhabitantData.Home(
                ResidenceScan.homeId(result.bed()),
                result.bed(),
                result.gates().score(),
                specialty,
                name,
                Optional.of(villager.getUUID()),
                -1L,
                -1L,
                false,
                today,
                today));
        Component message = Component.translatableWithFallback(
                "tougher.chat.inhabitant_arrive",
                "%s the %s has taken up residence.",
                Component.literal(name),
                Component.literal(InhabitantRules.specialtyFallback(specialty)));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(villager) <= 64.0 * 64.0) {
                player.sendSystemMessage(message);
            }
        }
        level.playSound(null, spawn, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, spawn, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    static void stamp(Villager villager, String homeId, String specialty, String name) {
        villager.setAttached(EhmAttachments.EHM_INHABITANT, Boolean.TRUE);
        villager.setAttached(EhmAttachments.EHM_INHABITANT_HOME, homeId);
        villager.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        villager.setPersistenceRequired();
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(villager.registryAccess()
                        .lookupOrThrow(Registries.VILLAGER_PROFESSION)
                        .getOrThrow(VillagerProfession.NITWIT)));
    }

    static void applyOffers(
            Villager villager,
            String specialty,
            InhabitantRules.AmenityCounts amenities,
            boolean blight,
            RandomSource random,
            int score) {
        MerchantOffers offers = villager.getOffers();
        offers.clear();
        offers.addAll(offersFor(specialty, amenities, blight, random, score));
    }

    static List<MerchantOffer> offersFor(
            String specialty,
            InhabitantRules.AmenityCounts amenities,
            boolean blight,
            RandomSource random,
            int score) {
        int storage = amenities == null ? 0 : amenities.storage();
        java.util.Random rng = new java.util.Random(random == null ? 0L : random.nextLong());
        List<MerchantOffer> offers = new ArrayList<>();
        for (InhabitantRules.TradeListing listing :
                InhabitantRules.scaledListings(specialty, score, storage, blight, rng)) {
            MerchantOffer offer = offerFrom(listing);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return offers;
    }

    static MerchantOffer offerFrom(InhabitantRules.TradeListing listing) {
        if (listing == null) {
            return null;
        }
        net.minecraft.world.item.Item item = itemFromId(listing.itemId());
        if (item == null || item == Items.AIR) {
            return null;
        }
        if (listing.buy()) {
            return buy(item, listing.count(), listing.maxUses());
        }
        return sell(item, listing.count(), listing.emeralds(), listing.maxUses());
    }

    static net.minecraft.world.item.Item itemFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    static MerchantOffer buy(net.minecraft.world.item.Item item, int count, int maxUses) {
        return new MerchantOffer(
                new ItemCost(item, Math.max(1, count)),
                Optional.empty(),
                new ItemStack(Items.EMERALD),
                Math.max(1, maxUses),
                0,
                0.0F);
    }

    static MerchantOffer sell(net.minecraft.world.item.Item item, int count, int emeralds, int maxUses) {
        int total = Math.max(1, emeralds);
        int first = Math.min(64, total);
        Optional<ItemCost> extra = Optional.empty();
        if (total > 64) {
            extra = Optional.of(new ItemCost(Items.EMERALD, Math.min(64, total - 64)));
        }
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, first),
                extra,
                new ItemStack(item, Math.max(1, count)),
                Math.max(1, maxUses),
                0,
                0.0F);
    }

    static void restockDue(ServerLevel level, InhabitantData data, long day) {
        boolean blight = CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level));
        RandomSource random = level.getRandom();
        for (InhabitantData.Home home : List.copyOf(data.homes())) {
            if (home.living().isEmpty()) {
                continue;
            }
            if (!InhabitantRules.shouldRestock(
                    home.lastRestockDay(), day, InhabitantRules.restockDays(home.specialty()))) {
                continue;
            }
            Entity entity = level.getEntityInAnyDimension(home.living().get());
            if (entity instanceof Villager villager && isInhabitant(villager)) {
                WorldConfig cfg = ConfigManager.world(level);
                ResidenceScan.Result result = ResidenceScan.inspect(
                        level, home.bed(), occupiedBeds(level, home.id()), cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
                applyOffers(
                        villager,
                        home.specialty(),
                        result.amenities(),
                        blight,
                        random,
                        result.gates().score());
                data.put(home.withLastRestock(day));
            }
        }
    }

    static void keepNearHome(ServerLevel level) {
        for (InhabitantData.Home home : InhabitantData.of(level).homes()) {
            if (home.living().isEmpty()) {
                continue;
            }
            Entity entity = level.getEntityInAnyDimension(home.living().get());
            if (!(entity instanceof Villager villager) || !isInhabitant(villager)) {
                continue;
            }
            BlockPos dest = ResidenceScan.standableNear(level, home.bed());
            if (dest == null) {
                dest = home.bed().above();
            }
            double destX = dest.getX() + 0.5;
            double destY = dest.getY();
            double destZ = dest.getZ() + 0.5;
            double dist = villager.distanceToSqr(destX, destY, destZ);
            double snap = (double) InhabitantRules.SNAP_HOME_RANGE * InhabitantRules.SNAP_HOME_RANGE;
            double wander = (double) InhabitantRules.WANDER_RANGE * InhabitantRules.WANDER_RANGE;
            if (dist > snap) {
                villager.teleportTo(destX, destY, destZ);
            } else if (dist > wander) {
                villager.getNavigation().moveTo(destX, destY, destZ, 0.8);
            }
        }
    }

    static void complain(ServerLevel level, Entity entity, InhabitantData.Home home) {
        Component message = Component.translatableWithFallback(
                "tougher.chat.inhabitant_uneasy",
                "%s is uneasy about this house.",
                Component.literal(home.name()));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) <= 64.0 * 64.0) {
                player.sendSystemMessage(message);
            }
        }
    }

    static void leave(ServerLevel level, InhabitantData data, InhabitantData.Home home, Entity entity) {
        Component message = Component.translatableWithFallback(
                "tougher.chat.inhabitant_leave",
                "%s has moved out.",
                Component.literal(home.name()));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) <= 64.0 * 64.0) {
                player.sendSystemMessage(message);
            }
        }
        entity.discard();
        data.put(home.withLiving(Optional.empty())
                .withLeave(-1L, false)
                .withLastRoll(AntiFarming.overworldDay(level)));
    }

    static void onDeath(LivingEntity entity, DamageSource source) {
        CouncilMissions.onKill(entity, source);
        if (!(entity instanceof Villager villager)
                || !isInhabitant(villager)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        InhabitantData data = InhabitantData.of(level);
        InhabitantData.Home home = data.byLiving(villager.getUUID());
        if (home == null) {
            return;
        }
        long day = AntiFarming.overworldDay(level);
        data.put(home.withEmptyUntil(InhabitantRules.emptyUntilDay(day)).withLastRoll(day));
        Component message = Component.translatableWithFallback(
                "tougher.chat.inhabitant_slain",
                "%s will not be replaced here for a while.",
                Component.literal(home.name()));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(villager) <= 64.0 * 64.0) {
                player.sendSystemMessage(message);
            }
        }
    }

    static void onDamage(
            LivingEntity entity, DamageSource source, float dealt, float original, boolean blocked) {
        if (!(entity instanceof Villager villager)
                || !isInhabitant(villager)
                || !(entity.level() instanceof ServerLevel level)
                || !(source.getEntity() instanceof Player)) {
            return;
        }
        InhabitantData data = InhabitantData.of(level);
        InhabitantData.Home home = data.byLiving(villager.getUUID());
        if (home == null || home.uneasy()) {
            return;
        }
        complain(level, villager, home);
        data.put(home.withLeave(InhabitantRules.leaveAfterDay(AntiFarming.overworldDay(level)), true));
    }

    public static boolean isInhabitant(Entity entity) {
        return entity != null
                && Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_INHABITANT, Boolean.FALSE));
    }

    public static void onStartTrading(Villager villager, Player player) {
        ensureOffers(villager);
        if (!(player instanceof ServerPlayer serverPlayer) || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        InhabitantData.Home home = InhabitantData.of(level).byLiving(villager.getUUID());
        if (home == null) {
            return;
        }
        if (InhabitantRules.COUNCIL.equals(home.specialty())) {
            CouncilMissions.onTalk(serverPlayer, level, home);
            return;
        }
        if (!InhabitantRules.BOUNTY.equals(home.specialty())) {
            return;
        }
        if (BiomeBosses.anySpawned(level.getServer())) {
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.inhabitant_bounty_boss",
                    "Something huge is out there. You would be wise to be careful."));
        }
    }

    public static void ensureOffers(Villager villager) {
        if (!isInhabitant(villager) || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        InhabitantData.Home home = InhabitantData.of(level).byLiving(villager.getUUID());
        if (home == null) {
            return;
        }
        if (villager.getOffers().isEmpty()) {
            WorldConfig cfg = ConfigManager.world(level);
            ResidenceScan.Result result = ResidenceScan.inspect(
                    level, home.bed(), occupiedBeds(level, home.id()), cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
            applyOffers(
                    villager,
                    home.specialty(),
                    result.amenities(),
                    CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level)),
                    level.getRandom(),
                    result.gates().score());
        }
    }

    static List<BlockPos> occupiedBeds(ServerLevel level) {
        return occupiedBeds(level, null);
    }

    static List<BlockPos> occupiedBeds(ServerLevel level, String exceptId) {
        List<BlockPos> beds = new ArrayList<>();
        for (InhabitantData.Home home : InhabitantData.of(level).homes()) {
            if (home.living().isEmpty()) {
                continue;
            }
            if (exceptId != null && exceptId.equals(home.id())) {
                continue;
            }
            beds.add(home.bed());
        }
        return beds;
    }

    static boolean hasSurvivalPlayer(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && !player.isCreative()) {
                return true;
            }
        }
        return false;
    }

    static List<BlockPos> bedHeadsInTickingChunks(ServerLevel level) {
        List<BlockPos> beds = new ArrayList<>();
        level.getChunkSource().chunkMap.forEachBlockTickingChunk(chunk -> collectBedHeads(chunk, beds));
        return beds;
    }

    static List<BlockPos> bedHeadsNear(ServerLevel level, BlockPos origin, int range) {
        List<BlockPos> beds = new ArrayList<>();
        int radius = Math.max(0, range);
        int minCx = origin.getX() - radius >> 4;
        int maxCx = origin.getX() + radius >> 4;
        int minCz = origin.getZ() - radius >> 4;
        int maxCz = origin.getZ() + radius >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                chunk.findBlocks(Inhabitants::isBedHead, (pos, state) -> {
                    if (TorchLifetimeRules.chestInRange(
                            pos.getX() - origin.getX(),
                            pos.getY() - origin.getY(),
                            pos.getZ() - origin.getZ(),
                            radius)) {
                        beds.add(pos.immutable());
                    }
                });
            }
        }
        return beds;
    }

    static void collectBedHeads(LevelChunk chunk, List<BlockPos> beds) {
        chunk.findBlocks(Inhabitants::isBedHead, (pos, state) -> beds.add(pos.immutable()));
    }

    static boolean isBedHead(BlockState state) {
        if (!state.is(BlockTags.BEDS)) {
            return false;
        }
        if (!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)) {
            return true;
        }
        return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)
                == BedPart.HEAD;
    }
}
