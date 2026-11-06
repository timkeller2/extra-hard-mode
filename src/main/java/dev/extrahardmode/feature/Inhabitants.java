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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
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
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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
        bus.listen(UseEntityCallback.EVENT, ID, Inhabitants::onUseEntity);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Inhabitants::onDeath);
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, Inhabitants::onDamage);
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, PlayerShops::onBreak);
    }

    @Override
    public void serverTick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 5L == 0L) {
            WiseLessons.mist(level);
        }
        boolean second = now % 20L == 0L;
        boolean expire = Math.floorMod(now, InhabitantRules.EXPIRE_SCAN_TICKS) == 0L;
        if (second || expire) {
            long day = AntiFarming.overworldDay(level);
            if (expire) {
                WiseLessons.despawnExpired(level, day);
                CouncilMissions.retireNightSpawns(level, day);
            }
            if (second) {
                for (ServerPlayer player : level.players()) {
                    CouncilMissions.tick(player, day);
                    WiseLessons.tick(player, level, day);
                }
            }
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        tickHomes(level);
        if (second) {
            keepNearHome(level);
            discardOrphanResidents(level);
        }
        if (Math.floorMod(now, InhabitantRules.VISIT_SCAN_TICKS) == 0L) {
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

    static InteractionResult onUseEntity(
            Player player, Level level, InteractionHand hand, Entity entity, net.minecraft.world.phys.EntityHitResult hit) {
        if (!(entity instanceof Villager villager) || !isInhabitant(villager)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, ID)) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && serverPlayer.getMainHandItem().is(Items.CLOCK)) {
            tradeHomes(serverPlayer, server, villager);
            return InteractionResult.SUCCESS;
        }
        InhabitantData.Home home = InhabitantData.of(server).byLiving(villager.getUUID());
        if (home == null || !InhabitantRules.WISE_TEACHER.equals(home.specialty())) {
            return InteractionResult.PASS;
        }
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        teach(serverPlayer, villager, server, home);
        return InteractionResult.SUCCESS;
    }

    static void tradeHomes(ServerPlayer player, ServerLevel level, Villager clicked) {
        InhabitantData data = InhabitantData.of(level);
        InhabitantData.Home clickedHome = data.byLiving(clicked.getUUID());
        if (clickedHome == null || clickedHome.living().isEmpty()) {
            player.setAttached(EhmAttachments.EHM_PENDING_MOVE, "");
            tell(player, "tougher.message.inhabitant_move_gone", "That resident is no longer there.");
            return;
        }
        String pendingId = player.getAttachedOrElse(EhmAttachments.EHM_PENDING_MOVE, "");
        if (pendingId == null || pendingId.isEmpty()) {
            chooseMove(player, clickedHome);
            return;
        }
        if (pendingId.equals(clickedHome.id())) {
            tell(player, "tougher.message.inhabitant_move_same", "Clock a different resident.");
            return;
        }
        InhabitantData.Home pendingHome = data.get(pendingId);
        if (pendingHome == null || pendingHome.living().isEmpty()) {
            tell(player, "tougher.message.inhabitant_move_gone", "That resident is no longer there.");
            chooseMove(player, clickedHome);
            return;
        }
        boolean refused = false;
        if (!InhabitantRules.specialtyAllowed(pendingHome.specialty(), clickedHome.score())) {
            refuseMove(player, pendingHome, clickedHome.score());
            refused = true;
        }
        if (!InhabitantRules.specialtyAllowed(clickedHome.specialty(), pendingHome.score())) {
            refuseMove(player, clickedHome, pendingHome.score());
            refused = true;
        }
        if (refused) {
            return;
        }
        int fee = InhabitantRules.movingFee(pendingHome.score(), clickedHome.score());
        if (InventorySearch.count(player, Items.EMERALD) < fee) {
            tellFee(player, fee);
            return;
        }
        boolean pendingHeld = false;
        boolean clickedHeld = false;
        try {
            loadAroundBed(level, pendingHome.bed());
            pendingHeld = true;
            loadAroundBed(level, clickedHome.bed());
            clickedHeld = true;
            Villager pendingVillager = residentIn(level, pendingHome.living().get());
            if (pendingVillager == null || !clicked.isAlive() || clicked.isRemoved() || !isInhabitant(clicked)) {
                tell(player, "tougher.message.inhabitant_move_gone", "That resident is no longer there.");
                if (pendingVillager == null) {
                    chooseMove(player, clickedHome);
                }
                return;
            }
            double pendingX = pendingVillager.getX();
            double pendingY = pendingVillager.getY();
            double pendingZ = pendingVillager.getZ();
            float pendingYaw = pendingVillager.getYRot();
            float pendingPitch = pendingVillager.getXRot();
            double clickedX = clicked.getX();
            double clickedY = clicked.getY();
            double clickedZ = clicked.getZ();
            float clickedYaw = clicked.getYRot();
            float clickedPitch = clicked.getXRot();
            if (!moveBody(level, pendingVillager, clickedHome.bed())) {
                tell(player, "tougher.message.inhabitant_move_failed", "That resident could not be moved.");
                return;
            }
            if (!moveBody(level, clicked, pendingHome.bed())) {
                placeBody(level, pendingVillager, pendingX, pendingY, pendingZ, pendingYaw, pendingPitch);
                tell(player, "tougher.message.inhabitant_move_failed", "That resident could not be moved.");
                return;
            }
            if (InventorySearch.count(player, Items.EMERALD) < fee
                    || !InventorySearch.consume(player, Items.EMERALD, fee)) {
                placeBody(level, pendingVillager, pendingX, pendingY, pendingZ, pendingYaw, pendingPitch);
                placeBody(level, clicked, clickedX, clickedY, clickedZ, clickedYaw, clickedPitch);
                tellFee(player, fee);
                return;
            }
            String pendingPlate = InhabitantRules.nameplate(pendingHome.name(), pendingHome.specialty());
            String clickedPlate = InhabitantRules.nameplate(clickedHome.name(), clickedHome.specialty());
            data.put(pendingHome.withResident(
                    clickedHome.specialty(), clickedHome.name(), clickedHome.living(), clickedHome.lastRestockDay()));
            data.put(clickedHome.withResident(
                    pendingHome.specialty(), pendingHome.name(), pendingHome.living(), pendingHome.lastRestockDay()));
            pendingVillager.setAttached(EhmAttachments.EHM_INHABITANT_HOME, clickedHome.id());
            clicked.setAttached(EhmAttachments.EHM_INHABITANT_HOME, pendingHome.id());
            showNameplate(pendingVillager, pendingHome.name(), pendingHome.specialty());
            showNameplate(clicked, clickedHome.name(), clickedHome.specialty());
            player.setAttached(EhmAttachments.EHM_PENDING_MOVE, "");
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.chat.inhabitant_swap",
                    "%s and %s have traded homes.",
                    Component.literal(pendingPlate),
                    Component.literal(clickedPlate)));
            level.playSound(null, clickedHome.bed(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.playSound(null, pendingHome.bed(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } finally {
            if (clickedHeld) {
                releaseBed(level, clickedHome.bed());
            }
            if (pendingHeld) {
                releaseBed(level, pendingHome.bed());
            }
        }
    }

    /**
     * Brings the bed and the wander around it into entity-ticking range. An ender-pearl ticket
     * expires on its own and is not a permanent force-load. The caller removes it after the move.
     */
    private static void loadAroundBed(ServerLevel level, BlockPos bed) {
        ChunkPos center = ChunkPos.containing(bed);
        int spread = InhabitantRules.residentChunkSpread(InhabitantRules.SNAP_HOME_RANGE);
        int radius = InhabitantRules.residentLoadRadius(InhabitantRules.SNAP_HOME_RANGE);
        CompletableFuture<?> loading = level.getChunkSource()
                .addTicketAndLoadWithRadius(TicketType.ENDER_PEARL, center, radius);
        if (!loading.isDone()) {
            level.getServer().managedBlock(loading::isDone);
        }
        if (!loading.isCompletedExceptionally()) {
            level.waitForEntities(center, spread);
        }
    }

    private static void releaseBed(ServerLevel level, BlockPos bed) {
        level.getChunkSource().removeTicketWithRadius(
                TicketType.ENDER_PEARL,
                ChunkPos.containing(bed),
                InhabitantRules.residentLoadRadius(InhabitantRules.SNAP_HOME_RANGE));
    }

    private static @Nullable Villager residentIn(ServerLevel level, UUID id) {
        Entity entity = level.getEntityInAnyDimension(id);
        if (entity instanceof Villager villager
                && villager.level() == level
                && villager.isAlive()
                && !villager.isRemoved()
                && isInhabitant(villager)) {
            return villager;
        }
        return null;
    }

    private static void tellFee(ServerPlayer player, int fee) {
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.inhabitant_move_fee",
                "Moving costs %s emeralds.",
                Component.literal(Integer.toString(fee))));
    }

    static void chooseMove(ServerPlayer player, InhabitantData.Home home) {
        player.setAttached(EhmAttachments.EHM_PENDING_MOVE, home.id());
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.inhabitant_move_chosen",
                "%s will trade homes. Clock another resident.",
                Component.literal(InhabitantRules.nameplate(home.name(), home.specialty()))));
    }

    static void refuseMove(ServerPlayer player, InhabitantData.Home resident, int destinationScore) {
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.inhabitant_move_score",
                "%s will not move. That home has %s points, and the job needs %s.",
                Component.literal(InhabitantRules.nameplate(resident.name(), resident.specialty())),
                Component.literal(Integer.toString(destinationScore)),
                Component.literal(Integer.toString(InhabitantRules.minScoreForSpecialty(resident.specialty())))));
    }

    static void tell(ServerPlayer player, String key, String fallback) {
        player.sendSystemMessage(Component.translatableWithFallback(key, fallback));
    }

    static boolean moveBody(ServerLevel level, Villager villager, BlockPos bed) {
        BlockPos dest = ResidenceScan.standableNear(level, bed);
        if (dest == null) {
            dest = bed.above();
        }
        return placeBody(
                level, villager, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, villager.getYRot(), villager.getXRot());
    }

    static boolean placeBody(
            ServerLevel level, Villager villager, double x, double y, double z, float yaw, float pitch) {
        if (villager.getNavigation() != null) {
            villager.getNavigation().stop();
        }
        boolean moved = villager.teleportTo(level, x, y, z, Set.of(), yaw, pitch, false);
        if (!moved || villager.isRemoved() || !villager.isAlive()) {
            return false;
        }
        villager.resetFallDistance();
        villager.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    static void teach(ServerPlayer player, Villager villager, ServerLevel level, InhabitantData.Home home) {
        try {
            if (WiseLessons.willStartHunt(player, villager, level)) {
                WiseLessons.grantTeachingOffer(player, villager.getStringUUID());
            }
            teachServices(player, villager, level, home);
        } finally {
            WiseLessons.onTalk(player, villager, level, home);
        }
    }

    private static void teachServices(ServerPlayer player, Villager villager, ServerLevel level, InhabitantData.Home home) {
        ensureWiseAbility(villager, level);
        String teacherId = villager.getStringUUID();
        int offers = WiseLessons.teachingOffers(player, teacherId);
        int price = InhabitantRules.wiseAbilityPrice(home.score());
        int emeralds = InventorySearch.count(player, Items.EMERALD);
        int storedMana = Achievements.storedManaLevel(player);
        if (holdingRedstoneBlock(player)) {
            int blocks = InventorySearch.count(player, Items.REDSTONE_BLOCK);
            if (InhabitantRules.canGiveRedstone(offers, blocks)) {
                giveRedstone(player, villager, level, teacherId, blocks);
                return;
            }
        }
        String ability = focusAbility(player);
        if (ability != null && InhabitantRules.canTeachFocus(offers, storedMana, emeralds, price)) {
            teachFocus(player, villager, level, teacherId, ability, price, InhabitantRules.wiseFocusUses(home.score()));
            return;
        }
        if (holdingEmeralds(player) && InhabitantRules.canTeachFocus(offers, storedMana, emeralds, price)) {
            String rolled = InhabitantRules.pickWiseAbility(level.getRandom().nextInt());
            teachFocus(
                    player, villager, level, teacherId, rolled, price, InhabitantRules.wiseEmeraldUses(home.score()));
            return;
        }
        explainLesson(player, ability, holdingEmeralds(player), offers, storedMana, price);
    }

    private static void teachFocus(
            ServerPlayer player,
            Villager villager,
            ServerLevel level,
            String teacherId,
            String ability,
            int price,
            int uses) {
        if (price > 0 && !InventorySearch.consume(player, Items.EMERALD, price)) {
            return;
        }
        boolean knows = ManaAbilities.knowsAbility(player, ability);
        ManaAbilities.grantWiseUses(player, ability, uses);
        WiseLessons.recordLesson(player, teacherId);
        WiseLessons.spendTeachingOffer(player, teacherId);
        Component name = Component.literal(AbilityRules.nameFallback(ability));
        if (knows) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.freed", "You already knew %s.", name));
        } else {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.flavor." + ability, AbilityRules.teacherFlavor(ability)));
            String how = AbilityRules.teacherInstruction(ability);
            if (!how.isEmpty()) {
                player.sendSystemMessage(Component.translatableWithFallback("tougher.wise.how." + ability, how));
            }
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.learned", "You have learned %s.", name));
        }
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private static void giveRedstone(
            ServerPlayer player, Villager villager, ServerLevel level, String teacherId, int blocks) {
        int gifts = WiseLessons.redstoneGifts(player, teacherId);
        int take = InhabitantRules.redstoneBlocksTaken(gifts, blocks);
        if (take <= 0 || !InventorySearch.consume(player, Items.REDSTONE_BLOCK, take)) {
            return;
        }
        double gain = InhabitantRules.capacityGain(take);
        double earned = WiseLessons.addLearningCapacity(player, gain);
        int base = ConfigManager.world(level).manaLearningCapacity();
        double effective = Math.max(0, base) + earned;
        WiseLessons.addRedstoneGift(player, teacherId);
        WiseLessons.spendTeachingOffer(player, teacherId);
        String taken = take == 1 ? "1 redstone block" : take + " redstone blocks";
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.wise.redstone",
                "Your mana learning rises by %s, to %s. I took %s.",
                Component.literal(InhabitantRules.learningLabel(gain)),
                Component.literal(InhabitantRules.learningLabel(effective)),
                Component.literal(taken)));
        level.playSound(
                null, villager.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        Achievements.sendMana(player);
    }

    private static void explainLesson(
            ServerPlayer player, String ability, boolean emeraldsInHand, int offers, int storedMana, int price) {
        if (offers <= 0) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.no_offer",
                    "I can teach when a new hunt is ready for you, or after you kill more than half of a hunt before the marks vanish."));
        }
        if ((ability != null || emeraldsInHand) && storedMana < 1) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.need_mana",
                    "You need a mana level before I will train an ability."));
        }
        if (ability != null) {
            String how = AbilityRules.teacherInstruction(ability);
            if (!how.isEmpty()) {
                player.sendSystemMessage(Component.translatableWithFallback("tougher.wise.how." + ability, how));
            }
            Component name = Component.literal(AbilityRules.nameFallback(ability));
            if (price <= 0) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.wise.price_ability_free",
                        "I teach %s for free. You gain the house points in uses.",
                        name));
            } else {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.wise.price_ability",
                        "I teach %s for %s emeralds from your pack. You gain the house points in uses.",
                        name,
                        Component.literal(Integer.toString(price))));
            }
            return;
        }
        if (emeraldsInHand) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.emeralds",
                    "Hand me emeralds and I choose an ability. You gain twice the house points in uses."));
        } else {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.hold",
                    "Hold the focus of the ability you want. I take the emeralds from your pack, and you gain the house points in uses. Hold emeralds and I choose an ability for twice the house points. Hold redstone blocks to raise your mana learning."));
        }
        if (price <= 0) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.price_free", "My price is free."));
        } else {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.wise.price",
                    "My price is %s emeralds.",
                    Component.literal(Integer.toString(price))));
        }
    }

    private static boolean holdingRedstoneBlock(ServerPlayer player) {
        return player.getMainHandItem().is(Items.REDSTONE_BLOCK);
    }

    private static boolean holdingEmeralds(ServerPlayer player) {
        return player.getMainHandItem().is(Items.EMERALD);
    }

    /** The ability for the main-hand focus. A tome with experience reads. A redstone block does not. */
    private static String focusAbility(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held == null || held.isEmpty() || held.is(Items.REDSTONE_BLOCK)) {
            return null;
        }
        String ability = AbilityRules.abilityForItem(ManaAbilities.heldItemId(held));
        if (ability != null) {
            return ability;
        }
        if (ManaAbilities.tomeXp(held) > 0) {
            return AbilityRules.READ_TOME;
        }
        return null;
    }

    static String ensureWiseAbility(Villager villager, ServerLevel level) {
        String stored = villager.getAttached(EhmAttachments.EHM_WISE_ABILITY);
        String ability = AbilityRules.storedWiseAbility(stored);
        if (ability == null || ability.isEmpty() || !AbilityRules.ABILITY_IDS.contains(ability)) {
            ability = InhabitantRules.pickWiseAbility(level.getRandom().nextInt());
        }
        if (!ability.equals(stored)) {
            villager.setAttached(EhmAttachments.EHM_WISE_ABILITY, ability);
        }
        return ability;
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
        String report = InhabitantRules.inspectFallback(result.gates());
        if (!result.gates().eligible()) {
            String biome = Exploration.biomeId(level.getBiome(result.bed()));
            boolean earns = ExplorationRules.homeEarnsExperience(
                    player.getAttachedOrElse(EhmAttachments.EHM_HOME_BIOMES, List.of()),
                    biome,
                    level.dimension().identifier().toString(),
                    result.bed().getX(),
                    result.bed().getZ());
            if (!biome.isEmpty()) {
                report = report + " " + ExplorationRules.homeExperienceProspect(earns);
            }
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.inhabitant_inspect", "%s", Component.literal(report)));
        InhabitantData.Home home = data.get(ResidenceScan.homeId(result.bed()));
        if (home != null && home.living().isPresent()) {
            home = ensureMaleName(data, home);
            home = releaseIfResidentMissing(level, data, home, day);
        }
        if (home != null && home.living().isPresent()) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.inhabitant_lives_here",
                    "%s the %s lives here.",
                    Component.literal(home.name()),
                    Component.literal(InhabitantRules.specialtyFallback(home.specialty()))));
        } else if (villagerNearby(level, result.bed())) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.inhabitant_near_villager",
                    "A villager is within 35 blocks, so no resident will appear."));
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
        long now = level.getGameTime();
        int homeIndex = 0;
        for (InhabitantData.Home home : List.copyOf(data.homes())) {
            int phase = homeIndex++;
            if (home.living().isEmpty()) {
                continue;
            }
            home = ensureMaleName(data, home);
            home = releaseIfResidentMissing(level, data, home, day);
            if (home.living().isEmpty()) {
                continue;
            }
            Entity entity = level.getEntityInAnyDimension(home.living().get());
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof Villager villager) {
                showNameplate(villager, home.name(), home.specialty());
                showOutfit(villager, home.specialty(), level.getRandom());
                if (isInhabitant(villager)) {
                    ResidentDefense.tick(level, villager, home.score());
                }
            }
            if (!InhabitantRules.houseScanDue(now, phase)) {
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
        if (villagerNearby(level, result.bed())) {
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
                result.amenities(),
                result.gates().score(),
                random.nextInt(),
                data.spawnedSpecialties(),
                data.livingCounts());
        String name = InhabitantRules.pickName(random.nextInt());
        stamp(villager, ResidenceScan.homeId(result.bed()), specialty, name, random);
        if (InhabitantRules.WISE_TEACHER.equals(specialty)) {
            villager.setAttached(
                    EhmAttachments.EHM_WISE_ABILITY, InhabitantRules.pickWiseAbility(random.nextInt()));
        }
        applyOffers(
                villager,
                specialty,
                result.amenities(),
                CropGrowthRules.beesInactive(AntiFarming.currentSeasonalLossRate(level)),
                random,
                shopScore(villager, result.gates().score()));
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

    static void showNameplate(Villager villager, String name, String specialty) {
        String plate = InhabitantRules.nameplate(name, specialty);
        Component current = villager.getCustomName();
        if (current == null || !plate.equals(current.getString()) || !villager.isCustomNameVisible()) {
            villager.setCustomName(Component.literal(plate));
            villager.setCustomNameVisible(true);
        }
    }

    /**
     * Roll once, then put those clothes back if the game changes them. Changing profession
     * clears the trade list, so a stocked resident keeps the offers they already had.
     */
    static void showOutfit(Villager villager, String specialty, RandomSource random) {
        String saved = villager.getAttachedOrElse(EhmAttachments.EHM_RESIDENT_OUTFIT, "");
        InhabitantRules.Outfit outfit = InhabitantRules.parseOutfit(saved);
        if (outfit == null) {
            RandomSource rng = random == null ? villager.getRandom() : random;
            outfit = InhabitantRules.pickOutfit(specialty, rng.nextInt(), rng.nextInt(), rng.nextInt());
            villager.setAttached(EhmAttachments.EHM_RESIDENT_OUTFIT, InhabitantRules.outfitKey(outfit));
        }
        applyOutfit(villager, outfit);
        holdCostume(villager);
    }

    /** A novice coat with no experience is deleted, and the shop closes with it. */
    public static void holdCostume(Villager villager) {
        if (villager == null) {
            return;
        }
        if (InhabitantRules.vanillaClearsNoviceCostume(villager.getVillagerData().level(), villager.getVillagerXp())) {
            villager.setVillagerXp(InhabitantRules.COSTUME_EXPERIENCE);
        }
    }

    /** Put the costume back and refill an empty shop before the trade screen decides. */
    public static void prepareTrade(Villager villager) {
        if (!isInhabitant(villager) || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        InhabitantData.Home home = InhabitantData.of(level).byLiving(villager.getUUID());
        if (home != null) {
            showOutfit(villager, home.specialty(), level.getRandom());
        } else {
            holdCostume(villager);
        }
        ensureOffers(villager);
    }

    static void applyOutfit(Villager villager, InhabitantRules.Outfit outfit) {
        Identifier professionId = Identifier.tryParse(outfit.professionId());
        Identifier styleId = Identifier.tryParse(outfit.styleId());
        if (professionId == null || styleId == null) {
            return;
        }
        ResourceKey<VillagerProfession> profession = ResourceKey.create(Registries.VILLAGER_PROFESSION, professionId);
        ResourceKey<VillagerType> style = ResourceKey.create(Registries.VILLAGER_TYPE, styleId);
        var access = villager.registryAccess();
        var dressed = villager.getVillagerData().withProfession(access, profession).withType(access, style).withLevel(outfit.level());
        if (dressed.equals(villager.getVillagerData()) && villager.getVillagerDataFinalized()) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        boolean keepOffers = offers != null && !offers.isEmpty();
        villager.setVillagerData(dressed);
        villager.setVillagerDataFinalized(true);
        if (keepOffers) {
            villager.setOffers(offers);
        }
    }

    static void stamp(Villager villager, String homeId, String specialty, String name, RandomSource random) {
        villager.setAttached(EhmAttachments.EHM_INHABITANT, Boolean.TRUE);
        villager.setAttached(EhmAttachments.EHM_INHABITANT_HOME, homeId);
        villager.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        villager.setPersistenceRequired();
        showNameplate(villager, name, specialty);
        showOutfit(villager, specialty, random);
    }

    static void applyOffers(
            Villager villager,
            String specialty,
            InhabitantRules.AmenityCounts amenities,
            boolean blight,
            RandomSource random,
            double score) {
        MerchantOffers offers = villager.getOffers();
        offers.clear();
        offers.addAll(offersFor(specialty, amenities, blight, random, score, villager.level().registryAccess()));
    }

    static List<MerchantOffer> offersFor(
            String specialty,
            InhabitantRules.AmenityCounts amenities,
            boolean blight,
            RandomSource random,
            double score,
            RegistryAccess registries) {
        int storage = amenities == null ? 0 : amenities.storage();
        RandomSource rng = random == null ? RandomSource.create(0L) : random;
        java.util.Random listingRng = new java.util.Random(rng.nextLong());
        List<MerchantOffer> offers = new ArrayList<>();
        for (InhabitantRules.TradeListing listing :
                InhabitantRules.scaledListings(specialty, score, storage, blight, listingRng)) {
            MerchantOffer offer = offerFrom(listing, rng, registries);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return offers;
    }

    static MerchantOffer offerFrom(
            InhabitantRules.TradeListing listing, RandomSource random, RegistryAccess registries) {
        if (listing == null) {
            return null;
        }
        if (!listing.potionId().isEmpty()) {
            if (registries == null) {
                return null;
            }
            ItemStack brewed = potionStack(listing.potionId(), registries);
            if (brewed == null) {
                return null;
            }
            return sell(brewed, listing.emeralds(), listing.maxUses());
        }
        net.minecraft.world.item.Item item = itemFromId(listing.itemId());
        if (item == null || item == Items.AIR) {
            return null;
        }
        int count = offerCount(item, listing.count());
        if (listing.buy()) {
            return buy(item, count, listing.emeralds(), listing.maxUses());
        }
        ItemStack result = new ItemStack(item, count);
        if (listing.enchantLevel() > 0 && random != null && registries != null) {
            result = enchantTrade(random, result, listing.enchantLevel(), registries);
        }
        return sell(result, listing.emeralds(), listing.maxUses());
    }

    /** A drinkable potion for this registry id, or null when the id is not a potion. */
    static ItemStack potionStack(String potionId, RegistryAccess registries) {
        Identifier id = Identifier.tryParse(potionId);
        if (id == null) {
            return null;
        }
        ResourceKey<Potion> key = ResourceKey.create(Registries.POTION, id);
        Optional<Holder.Reference<Potion>> holder = registries.lookupOrThrow(Registries.POTION).get(key);
        if (holder.isEmpty()) {
            return null;
        }
        return PotionContents.createItemStack(Items.POTION, holder.get());
    }

    /** Traded-equipment enchantments only, retried so the piece is not sold plain. */
    static ItemStack enchantTrade(RandomSource random, ItemStack stack, int level, RegistryAccess registries) {
        Optional<HolderSet.Named<Enchantment>> traded = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentTags.ON_TRADED_EQUIPMENT);
        ItemStack result = stack;
        for (int attempt = 0; attempt < 8; attempt++) {
            Stream<Holder<Enchantment>> allowed = traded.map(HolderSet::stream).orElseGet(Stream::empty);
            result = EnchantmentHelper.enchantItem(random, stack.copy(), level, allowed);
            if (EnchantmentHelper.hasAnyEnchantments(result)) {
                return result;
            }
        }
        return result;
    }

    /** A trade slot cannot hold more than the item's stack. */
    static int offerCount(net.minecraft.world.item.Item item, int count) {
        int max = Math.max(1, new net.minecraft.world.item.ItemStack(item).getMaxStackSize());
        return Math.clamp(count, 1, max);
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

    static MerchantOffer buy(net.minecraft.world.item.Item item, int count, int emeralds, int maxUses) {
        int paid = Math.clamp(emeralds, 1, 64);
        return new MerchantOffer(
                new ItemCost(item, Math.max(1, count)),
                Optional.empty(),
                new ItemStack(Items.EMERALD, paid),
                Math.max(0, maxUses),
                0,
                0.0F);
    }

    static MerchantOffer sell(ItemStack result, int emeralds, int maxUses) {
        int total = Math.max(1, emeralds);
        int first = Math.min(64, total);
        Optional<ItemCost> extra = Optional.empty();
        if (total > 64) {
            extra = Optional.of(new ItemCost(Items.EMERALD, Math.min(64, total - 64)));
        }
        ItemStack sold = result == null || result.isEmpty() ? new ItemStack(Items.AIR) : result.copy();
        if (sold.getCount() < 1) {
            sold.setCount(1);
        }
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, first),
                extra,
                sold,
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
                        shopScore(villager, result.gates().score()));
                data.put(home.withLastRestock(day));
            }
        }
    }

    /** A living home whose saved name is not on the male list is renamed once. */
    static InhabitantData.Home ensureMaleName(InhabitantData data, InhabitantData.Home home) {
        if (home == null || InhabitantRules.listedName(home.name())) {
            return home;
        }
        InhabitantData.Home named = home.withName(InhabitantRules.nameForHome(home.id()));
        data.put(named);
        return named;
    }

    /** Drop a home's resident record when that villager is dead or no longer loaded near the bed. */
    static InhabitantData.Home releaseIfResidentMissing(
            ServerLevel level, InhabitantData data, InhabitantData.Home home, long day) {
        if (home.living().isEmpty()) {
            return home;
        }
        Entity entity = level.getEntityInAnyDimension(home.living().get());
        boolean found = entity != null;
        boolean alive = found && entity.isAlive();
        if (!InhabitantRules.residentMissing(found, alive, homeAreaLoaded(level, home.bed()))) {
            return home;
        }
        InhabitantData.Home released = home.withLiving(Optional.empty()).withLeave(-1L, false).withLastRoll(day);
        data.put(released);
        return released;
    }

    /** Chunks covering the snap-home radius around the bed. */
    static boolean homeAreaLoaded(ServerLevel level, BlockPos bed) {
        int radius = InhabitantRules.SNAP_HOME_RANGE;
        int minCx = (bed.getX() - radius) >> 4;
        int maxCx = (bed.getX() + radius) >> 4;
        int minCz = (bed.getZ() - radius) >> 4;
        int maxCz = (bed.getZ() + radius) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A resident whose home no longer lists them is removed, so a missing record cannot come back as a second villager. */
    static void discardOrphanResidents(ServerLevel level) {
        InhabitantData data = InhabitantData.of(level);
        for (Villager villager : level.getEntities(EntityTypes.VILLAGER, Inhabitants::isInhabitant)) {
            if (!villager.isAlive() || data.byLiving(villager.getUUID()) != null) {
                continue;
            }
            villager.discard();
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
        WiseLessons.onMobDeath(entity);
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

    /** Restock time and heart icons for the resident under the crosshair. Null when it is not a resident. */
    public record ResidentLook(int restockTicks, int hearts) {}

    /**
     * {@code null} when the crosshair is not on a resident. Restock ticks are {@code 0} when already due.
     * Hearts are shown even at full health: one per 10 health.
     */
    public static @Nullable ResidentLook lookAtResident(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        Entity target = entityOnCrosshair(player);
        if (!(target instanceof Villager villager)) {
            return null;
        }
        if (!isInhabitant(villager) && homeFor(InhabitantData.of(level), villager) == null) {
            return null;
        }
        InhabitantData data = InhabitantData.of(level);
        InhabitantData.Home home = homeFor(data, villager);
        int hearts = ResidentCombatRules.heartIcons(villager.getHealth());
        if (home == null) {
            return new ResidentLook(0, hearts);
        }
        return new ResidentLook(
                InhabitantRules.restockRemainingTicks(
                        level.getOverworldClockTime(),
                        home.lastRestockDay(),
                        data.lastDawnDay(),
                        InhabitantRules.restockDays(home.specialty())),
                hearts);
    }

    /** Home for this villager, including residents saved before the living id or tag was reliable. */
    static InhabitantData.@Nullable Home homeFor(InhabitantData data, Villager villager) {
        InhabitantData.Home home = data.byLiving(villager.getUUID());
        if (home != null) {
            return home;
        }
        String homeId = villager.getAttachedOrElse(EhmAttachments.EHM_INHABITANT_HOME, "");
        if (homeId.isEmpty()) {
            return null;
        }
        return data.get(homeId);
    }

    /**
     * Closest pickable entity the crosshair hits before the first block, out to block reach.
     * A resident's box is taller so looking at the name still counts.
     */
    static @Nullable Entity entityOnCrosshair(ServerPlayer player) {
        double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        if (reach <= 0.0) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        HitResult blockHit = player.pick(reach, 1.0F, false);
        double maxDistSq = reach * reach;
        Vec3 clipEnd = eye.add(view.scale(reach));
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistSq = Math.min(maxDistSq, blockHit.getLocation().distanceToSqr(eye));
            clipEnd = blockHit.getLocation();
        }
        if (maxDistSq <= 0.0) {
            return null;
        }
        AABB search = player.getBoundingBox().expandTowards(view.scale(reach)).inflate(2.0);
        Entity closest = null;
        double closestDist = maxDistSq;
        for (Entity entity : player.level().getEntities(player, search, candidate -> candidate.isPickable() && !candidate.isSpectator())) {
            AABB box = lookBox(entity);
            if (box.contains(eye)) {
                return entity;
            }
            Optional<Vec3> clip = box.clip(eye, clipEnd);
            if (clip.isEmpty()) {
                continue;
            }
            double dist = eye.distanceToSqr(clip.get());
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
            }
        }
        return closest;
    }

    /** Name sits above the body. A little extra height and width so aiming at it still hits. */
    static AABB lookBox(Entity entity) {
        AABB box = entity.getBoundingBox();
        if (entity instanceof Villager) {
            return box.inflate(0.3, 0.0, 0.3).expandTowards(0.0, 0.8, 0.0);
        }
        return box;
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
        if (InhabitantRules.TRADER.equals(home.specialty()) || InhabitantRules.WEALTHY.equals(home.specialty())) {
            PlayerShops.refresh(villager, serverPlayer);
        }
        if (InhabitantRules.COUNCIL.equals(home.specialty())) {
            CouncilMissions.onTalk(serverPlayer, level, home);
            return;
        }
        if (!InhabitantRules.BOUNTY.equals(home.specialty())) {
            return;
        }
        if (BiomeBosses.anySpawned(level.getServer())) {
            warnAboutBiomeBoss(serverPlayer, villager);
        }
    }

    static void warnAboutBiomeBoss(ServerPlayer player, Villager resident) {
        Entity boss = BiomeBosses.nearestBoss(resident);
        String way = null;
        if (boss != null && boss.level() == resident.level()) {
            way = InhabitantRules.compassDirection(boss.getX() - resident.getX(), boss.getZ() - resident.getZ());
        }
        if (way == null) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.inhabitant_bounty_boss",
                    "Something huge is out there. You would be wise to be careful."));
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.inhabitant_bounty_boss_way",
                "Something huge is out there, to the %s. You would be wise to be careful.",
                Component.literal(way)));
    }

    /** One completed trade. Ordinary villagers are ignored. */
    public static void onTrade(Villager villager, MerchantOffer traded) {
        if (villager == null || !isInhabitant(villager) || villager.level().isClientSide()) {
            return;
        }
        if (PlayerShops.isShopOffer(traded)) {
            PlayerShops.settle(villager, traded);
            return;
        }
        int xp = tradeXp(villager);
        if (xp < Integer.MAX_VALUE) {
            villager.setAttached(EhmAttachments.EHM_RESIDENT_TRADE_XP, xp + 1);
        }
        if (traded != null) {
            adjustTraderBuys(villager, traded);
        }
    }

    /**
     * A purchase from a trader adds one use to every buyback. A sale spends one use
     * on every buyback. Vanilla already spent the use on the offer that just traded.
     */
    static void adjustTraderBuys(Villager villager, MerchantOffer traded) {
        if (PlayerShops.isShopOffer(traded) || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        InhabitantData.Home home = InhabitantData.of(level).byLiving(villager.getUUID());
        if (home == null || !InhabitantRules.TRADER.equals(home.specialty())) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        boolean changed = false;
        if (traded.getBaseCostA().is(Items.EMERALD)) {
            for (int i = 0; i < offers.size(); i++) {
                MerchantOffer offer = offers.get(i);
                if (!pairedTraderBuy(offers, offer)) {
                    continue;
                }
                offers.set(i, copyWithMaxUses(offer, offer.getMaxUses() + 1));
                changed = true;
            }
        } else if (traded.getResult().is(Items.EMERALD)) {
            for (MerchantOffer offer : offers) {
                if (offer == traded || !pairedTraderBuy(offers, offer)) {
                    continue;
                }
                if (offer.getUses() < offer.getMaxUses()) {
                    offer.increaseUses();
                    changed = true;
                }
            }
        }
        if (changed) {
            syncOffers(villager);
        }
    }

    /** A buy of an item this trader is also selling. Other buys stay on their own uses. */
    static boolean pairedTraderBuy(MerchantOffers offers, MerchantOffer offer) {
        if (offer == null || offer.getBaseCostA().is(Items.EMERALD) || !offer.getResult().is(Items.EMERALD)) {
            return false;
        }
        ItemStack cost = offer.getBaseCostA();
        for (MerchantOffer other : offers) {
            if (other.getBaseCostA().is(Items.EMERALD) && other.getResult().is(cost.getItem())) {
                return true;
            }
        }
        return false;
    }

    static MerchantOffer copyWithMaxUses(MerchantOffer offer, int maxUses) {
        return new MerchantOffer(
                offer.getItemCostA(),
                offer.getItemCostB(),
                offer.getResult().copy(),
                offer.getUses(),
                Math.max(0, maxUses),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand());
    }

    static void syncOffers(Villager villager) {
        Player player = villager.getTradingPlayer();
        if (player == null || player.containerMenu == null) {
            return;
        }
        if (player.containerMenu instanceof MerchantMenu menu) {
            menu.updateSellItem();
        }
        player.sendMerchantOffers(
                player.containerMenu.containerId,
                villager.getOffers(),
                villager.getVillagerData().level(),
                villager.getVillagerXp(),
                villager.showProgressBar(),
                villager.canRestock());
    }

    static int tradeXp(Villager villager) {
        if (villager == null) {
            return 0;
        }
        return Math.max(0, villager.getAttachedOrElse(EhmAttachments.EHM_RESIDENT_TRADE_XP, 0));
    }

    /** House points plus the square root of this resident's trading experience. */
    static double shopScore(Villager villager, int housePoints) {
        return InhabitantRules.stockScore(housePoints, tradeXp(villager));
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
                    shopScore(villager, result.gates().score()));
        }
    }

    /** A living villager who is not already a resident, closer than {@link InhabitantRules#VILLAGER_CLEARANCE}. */
    static boolean villagerNearby(ServerLevel level, BlockPos bed) {
        int range = InhabitantRules.VILLAGER_CLEARANCE;
        AABB box = new AABB(bed).inflate(range);
        double x = bed.getX() + 0.5;
        double y = bed.getY() + 0.5;
        double z = bed.getZ() + 0.5;
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, box, candidate -> candidate.isAlive() && !isInhabitant(candidate))) {
            if (!InhabitantRules.clearOfVillager(villager.getX() - x, villager.getY() - y, villager.getZ() - z, range)) {
                return true;
            }
        }
        return false;
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
