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
        return false;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, Inhabitants::onUseBlock);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Inhabitants::onDeath);
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, Inhabitants::onDamage);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        tickHomes(level);
        if (level.getGameTime() % 20L == 0L) {
            keepNearHome(level);
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
        ResidenceScan.Result result =
                ResidenceScan.inspect(level, bed, occupiedBeds(level), cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
        player.sendSystemMessage(Component.translatableWithFallback(
                "extrahardmode.message.inhabitant_inspect",
                "%s",
                Component.literal(InhabitantRules.inspectFallback(result.gates()))));
        InhabitantData.Home home = InhabitantData.of(level).get(ResidenceScan.homeId(result.bed()));
        if (home != null && home.living().isPresent()) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "extrahardmode.message.inhabitant_lives_here",
                    "%s the %s lives here.",
                    Component.literal(home.name()),
                    Component.literal(InhabitantRules.specialtyFallback(home.specialty()))));
        }
    }

    static void tickHomes(ServerLevel level) {
        long day = AntiFarming.overworldDay(level);
        InhabitantData data = InhabitantData.of(level);
        maintainOccupied(level, data, day);
        if (data.lastDawnDay() == day) {
            return;
        }
        data.setLastDawnDay(day);
        restockAll(level, data);
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
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            BlockPos origin = player.blockPosition();
            int radius = InhabitantRules.SCAN_RADIUS;
            BlockPos min = origin.offset(-radius, -4, -radius);
            BlockPos max = origin.offset(radius, 8, radius);
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (!level.isLoaded(pos) || !isBedHead(level.getBlockState(pos))) {
                    continue;
                }
                ResidenceScan.Result result =
                        ResidenceScan.inspect(level, pos.immutable(), occupied, cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
                String id = ResidenceScan.homeId(result.bed());
                if (!seen.add(id)) {
                    continue;
                }
                InhabitantData.Home existing = data.get(id);
                if (existing != null && existing.living().isPresent()) {
                    continue;
                }
                if (existing != null && InhabitantRules.spawnBlocked(day, existing.emptyUntilDay())) {
                    continue;
                }
                if (!result.gates().eligible()) {
                    continue;
                }
                int chance = InhabitantRules.spawnChancePercent(
                        result.gates().score(),
                        cfg.inhabitantMinScore(),
                        cfg.inhabitantBaseChancePercent(),
                        cfg.inhabitantChancePerPoint(),
                        cfg.inhabitantMaxChancePercent(),
                        blight);
                if (!InhabitantRules.spawnRoll(random.nextInt(100), chance)) {
                    continue;
                }
                spawnResident(level, data, result, random);
                occupied.add(result.bed());
            }
        }
    }

    static void spawnResident(
            ServerLevel level, InhabitantData data, ResidenceScan.Result result, RandomSource random) {
        BlockPos spawn = result.bed().above();
        Villager villager = EntityTypes.VILLAGER.spawn(level, spawn, EntitySpawnReason.EVENT);
        if (villager == null) {
            return;
        }
        String specialty = InhabitantRules.pickSpecialty(
                result.amenities(), result.gates().score(), random.nextInt());
        String name = InhabitantRules.pickName(random.nextInt());
        stamp(villager, ResidenceScan.homeId(result.bed()), specialty, name);
        applyOffers(villager, specialty, result.amenities(), CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level)));
        data.put(new InhabitantData.Home(
                ResidenceScan.homeId(result.bed()),
                result.bed(),
                result.gates().score(),
                specialty,
                name,
                Optional.of(villager.getUUID()),
                -1L,
                -1L,
                false));
        Component message = Component.translatableWithFallback(
                "extrahardmode.chat.inhabitant_arrive",
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

    static void applyOffers(Villager villager, String specialty, InhabitantRules.AmenityCounts amenities, boolean blight) {
        MerchantOffers offers = villager.getOffers();
        offers.clear();
        offers.addAll(offersFor(specialty, amenities, blight));
    }

    static List<MerchantOffer> offersFor(
            String specialty, InhabitantRules.AmenityCounts amenities, boolean blight) {
        int storage = amenities == null ? 0 : amenities.storage();
        return switch (specialty == null ? InhabitantRules.TRADER : specialty) {
            case InhabitantRules.HAULER -> List.of(
                    buy(Items.COBBLESTONE, InhabitantRules.haulerStack(storage), 4),
                    buy(Items.DIRT, InhabitantRules.haulerStack(storage), 4),
                    buy(Items.GRAVEL, InhabitantRules.haulerStack(storage), 4),
                    buy(Items.COBBLED_DEEPSLATE, InhabitantRules.haulerStack(storage), 4),
                    buy(Items.NETHERRACK, InhabitantRules.haulerStack(storage), 4));
            case InhabitantRules.COOK -> List.of(
                    buy(Items.COOKED_BEEF, 8, 4),
                    buy(Items.COOKED_PORKCHOP, 8, 4),
                    buy(Items.COOKED_CHICKEN, 8, 4),
                    buy(Items.BAKED_POTATO, 8, 4),
                    sell(Items.RABBIT_STEW, 1, 4, 4));
            case InhabitantRules.FARM -> List.of(
                    buy(Items.WHEAT, InhabitantRules.farmWheatBuy(blight), 6),
                    sell(Items.WHEAT_SEEDS, 4, 1, 6),
                    sell(Items.OAK_SAPLING, 1, 2, 4));
            case InhabitantRules.BOUNTY -> List.of(
                    buy(Items.SPIDER_EYE, 8, 6),
                    buy(Items.GUNPOWDER, 8, 6),
                    buy(Items.BONE, 16, 6));
            default -> List.of(
                    sell(Items.BREAD, 4, 1, 8),
                    sell(Items.COAL, 8, 1, 8),
                    sell(Items.BOOK, 1, 4, 4),
                    sell(Items.WOOL.white(), 8, 1, 8));
        };
    }

    static MerchantOffer buy(net.minecraft.world.item.Item item, int count, int maxUses) {
        return new MerchantOffer(
                new ItemCost(item, Math.max(1, count)),
                Optional.empty(),
                new ItemStack(Items.EMERALD),
                maxUses,
                0,
                0.0F);
    }

    static MerchantOffer sell(net.minecraft.world.item.Item item, int count, int emeralds, int maxUses) {
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, Math.max(1, emeralds)),
                Optional.empty(),
                new ItemStack(item, Math.max(1, count)),
                maxUses,
                0,
                0.0F);
    }

    static void restockAll(ServerLevel level, InhabitantData data) {
        boolean blight = CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level));
        for (InhabitantData.Home home : data.homes()) {
            if (home.living().isEmpty()) {
                continue;
            }
            Entity entity = level.getEntityInAnyDimension(home.living().get());
            if (entity instanceof Villager villager && isInhabitant(villager)) {
                WorldConfig cfg = ConfigManager.world(level);
                ResidenceScan.Result result = ResidenceScan.inspect(
                        level, home.bed(), occupiedBeds(level, home.id()), cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
                applyOffers(villager, home.specialty(), result.amenities(), blight);
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
            double dist = villager.distanceToSqr(home.bed().getX() + 0.5, home.bed().getY() + 1.0, home.bed().getZ() + 0.5);
            double snap = (double) InhabitantRules.SNAP_HOME_RANGE * InhabitantRules.SNAP_HOME_RANGE;
            double wander = (double) InhabitantRules.WANDER_RANGE * InhabitantRules.WANDER_RANGE;
            if (dist > snap) {
                BlockPos dest = home.bed().above();
                villager.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
            } else if (dist > wander) {
                villager.getNavigation()
                        .moveTo(home.bed().getX() + 0.5, home.bed().getY() + 1.0, home.bed().getZ() + 0.5, 0.8);
            }
        }
    }

    static void complain(ServerLevel level, Entity entity, InhabitantData.Home home) {
        Component message = Component.translatableWithFallback(
                "extrahardmode.chat.inhabitant_uneasy",
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
                "extrahardmode.chat.inhabitant_leave",
                "%s has moved out.",
                Component.literal(home.name()));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) <= 64.0 * 64.0) {
                player.sendSystemMessage(message);
            }
        }
        entity.discard();
        data.put(home.withLiving(Optional.empty()).withLeave(-1L, false));
    }

    static void onDeath(LivingEntity entity, DamageSource source) {
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
        data.put(home.withEmptyUntil(InhabitantRules.emptyUntilDay(day)));
        Component message = Component.translatableWithFallback(
                "extrahardmode.chat.inhabitant_slain",
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
        if (home == null || !InhabitantRules.BOUNTY.equals(home.specialty())) {
            return;
        }
        if (BiomeBosses.anySpawned(level.getServer())) {
            serverPlayer.sendSystemMessage(Component.translatableWithFallback(
                    "extrahardmode.message.inhabitant_bounty_boss",
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
                    CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level)));
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
