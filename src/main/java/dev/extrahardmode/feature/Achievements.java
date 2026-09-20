package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.mixin.LivingEntityLootAccess;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.module.TutorialCounts;
import dev.extrahardmode.network.ClientboundManaPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.AchievementClaimData;
import dev.extrahardmode.world.WorldGate;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Per-block builder and per-hostile slayer achievements, plus mana regen.
 */
public final class Achievements implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("achievements");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, Achievements::onBreak);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Achievements::onDeath);
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if ((level.getGameTime() + player.getId()) % AchievementRules.TICKS_PER_MINUTE == 0) {
                tickMana(player);
            }
        }
    }

    public static void sendMana(ServerPlayer player) {
        ServerPlayNetworking.send(
                player, new ClientboundManaPayload(manaLevel(player), (float) currentMana(player)));
    }

    public static void onPlaced(BlockPlaceContext context, Block block) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player) || skipPlayer(player)) {
            return;
        }
        String key = blockKey(block);
        if (key == null) {
            return;
        }
        int count = AchievementRules.increment(countOf(player, EhmAttachments.EHM_BLOCK_PLACE_COUNTS, key));
        putCount(player, EhmAttachments.EHM_BLOCK_PLACE_COUNTS, key, count);
        player.setAttached(EhmAttachments.EHM_LAST_PLACED_BLOCK, key);
        grantNewTiers(
                player,
                count,
                key,
                EhmAttachments.EHM_BLOCK_AWARDED,
                AchievementRules.BUILDER_THRESHOLDS,
                true,
                block.getName());
    }

    static void onBreak(
            net.minecraft.world.level.Level level,
            Player player,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)
                || !WorldGate.isModuleActive(serverLevel, ID)
                || !(player instanceof ServerPlayer serverPlayer)
                || skipPlayer(serverPlayer)) {
            return;
        }
        String key = blockKey(state.getBlock());
        if (key == null) {
            return;
        }
        int count = countOf(serverPlayer, EhmAttachments.EHM_BLOCK_PLACE_COUNTS, key);
        int next = AchievementRules.maybeDecrement(
                count, serverLevel.getRandom().nextInt(100), AchievementRules.BREAK_DECREMENT_PERCENT);
        if (next != count) {
            putCount(serverPlayer, EhmAttachments.EHM_BLOCK_PLACE_COUNTS, key, next);
        }
    }

    static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!(entity instanceof Enemy) || entity instanceof Player) {
            return;
        }
        if (!lootableKill(entity, level)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player) || skipPlayer(player)) {
            return;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null) {
            return;
        }
        String key = typeId.toString();
        int count = AchievementRules.increment(countOf(player, EhmAttachments.EHM_KILL_COUNTS, key));
        putCount(player, EhmAttachments.EHM_KILL_COUNTS, key, count);
        grantNewTiers(
                player,
                count,
                key,
                EhmAttachments.EHM_KILL_AWARDED,
                AchievementRules.SLAYER_THRESHOLDS,
                false,
                entity.getType().getDescription());
    }

    static boolean lootableKill(LivingEntity entity, ServerLevel level) {
        if (EntityHelper.lootless(entity) || Dragon.skipLoot(entity)) {
            return false;
        }
        return ((LivingEntityLootAccess) entity).tougher$shouldDropLoot(level);
    }

    static void tickMana(ServerPlayer player) {
        if (!WorldGate.isModuleActive(player.level(), ID) || skipPlayer(player)) {
            return;
        }
        int level = manaLevel(player);
        Double stored = player.getAttachedOrElse(EhmAttachments.EHM_MANA_CURRENT, 0.0);
        double current = AchievementRules.clampMana(stored == null ? 0.0 : stored);
        boolean quartz =
                AchievementRules.shouldBoostWithQuartz(level, current) && hasItem(player, Items.QUARTZ);
        double afterBase = AchievementRules.addMana(
                current, level, AchievementRules.regenPerMinute(level, current, quartz));
        if (quartz) {
            settleQuartzCredit(player, afterBase - current);
        }
        double next = afterBase;
        var food = player.getFoodData();
        float saturation = food.getSaturationLevel();
        if (AchievementRules.shouldRestoreFromSaturation(level, afterBase, saturation)) {
            double withSat = AchievementRules.clampMana(afterBase + AchievementRules.SATURATION_MANA_RESTORE);
            if (withSat > afterBase) {
                next = withSat;
                food.setSaturation(AchievementRules.saturationAfterManaRestore(saturation));
            }
        }
        if (stored == null || next != stored) {
            player.setAttached(EhmAttachments.EHM_MANA_CURRENT, next);
            sendMana(player);
        }
    }

    static void settleQuartzCredit(ServerPlayer player, double gained) {
        Double stored = player.getAttachedOrElse(EhmAttachments.EHM_QUARTZ_MANA_CREDIT, 0.0);
        double credit = AchievementRules.addQuartzCredit(stored == null ? 0.0 : stored, gained);
        int want = AchievementRules.quartzItemsForCredit(credit);
        int taken = 0;
        while (taken < want && consumeQuartz(player)) {
            taken++;
        }
        player.setAttached(
                EhmAttachments.EHM_QUARTZ_MANA_CREDIT, AchievementRules.remainingQuartzCredit(credit, taken));
    }

    static void grantNewTiers(
            ServerPlayer player,
            int count,
            String key,
            net.fabricmc.fabric.api.attachment.v1.AttachmentType<Map<String, Integer>> awardedType,
            int[] thresholds,
            boolean builder,
            Component subject) {
        int already = countOf(player, awardedType, key);
        int awarded = AchievementRules.awardedAfter(already, count, thresholds);
        if (awarded <= already) {
            return;
        }
        boolean competitive = ConfigManager.world(player.level()).competitiveAchievements();
        AchievementClaimData claims = AchievementClaimData.of(player.level());
        int grantedThrough = already;
        for (int i = already; i < awarded; i++) {
            String claimKey = AchievementRules.claimKey(builder, key, i);
            boolean first = claims.tryClaim(claimKey);
            if (competitive && !first) {
                continue;
            }
            grantedThrough = i + 1;
            announce(player, builder, subject);
            celebrate(player);
            giveReward(player);
            grantDiamond(player);
        }
        if (grantedThrough != already) {
            putCount(player, awardedType, key, grantedThrough);
        }
    }

    static void announce(ServerPlayer player, boolean builder, Component subject) {
        Component message = builder
                ? Component.translatableWithFallback(
                        "tougher.chat.builder",
                        "%s has achieved the %s builder achievement!",
                        player.getName(),
                        subject)
                : Component.translatableWithFallback(
                        "tougher.chat.slayer",
                        "%s has gained the %s slayer achievement!",
                        player.getName(),
                        subject);
        player.level().getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    static void celebrate(ServerPlayer player) {
        RandomSource random = player.getRandom();
        DyeColor color = DyeColor.values()[random.nextInt(DyeColor.values().length)];
        IntArrayList colors = new IntArrayList();
        colors.add(color.getFireworkColor());
        FireworkExplosion burst = new FireworkExplosion(
                FireworkExplosion.Shape.STAR, colors, new IntArrayList(), true, true);
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(burst)));
        player.level()
                .addFreshEntity(new FireworkRocketEntity(
                        player.level(), player.getX(), player.getY() + 1.0, player.getZ(), rocket));
    }

    static void grantDiamond(ServerPlayer player) {
        give(player, new ItemStack(Items.DIAMOND));
    }

    static void give(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            ItemEntity dropped = player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
            }
        }
    }

    static void giveReward(ServerPlayer player) {
        player.giveExperiencePoints(AchievementRules.REWARD_EXPERIENCE_POINTS);
        int gained = 0;
        if (AchievementRules.manaFromExperience(player.experienceLevel, player.getRandom().nextInt(100))) {
            gained++;
        }
        boolean consumedBlocks = tryConsumeManaBlocks(player);
        if (consumedBlocks) {
            gained++;
        }
        if (gained > 0) {
            grantMana(player, gained);
        }
        if (consumedBlocks) {
            ManaAbilities.startHealSparkle(player);
        }
    }

    static void grantMana(ServerPlayer player, int levels) {
        if (levels <= 0) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_MANA_LEVEL, manaLevel(player) + levels);
        sendMana(player);
        announceMana(player);
    }

    static void announceMana(ServerPlayer player) {
        Component message = Component.translatableWithFallback(
                "tougher.chat.mana_grow",
                "%s grows in special abilities to level %s!",
                player.getName(),
                Component.literal(String.valueOf(manaLevel(player))));
        player.level().getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    static boolean tryConsumeManaBlocks(ServerPlayer player) {
        int diamonds = countItem(player, Items.DIAMOND_BLOCK);
        int lapis = countItem(player, Items.LAPIS_BLOCK);
        int required = requiredLapis(player);
        if (AchievementRules.shouldHintLapis(diamonds, lapis, required)) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.mana_lapis_needed",
                    "You need %s lapis lazuli blocks to gain a mana level this way.",
                    Component.literal(Integer.toString(required))));
            return false;
        }
        if (!AchievementRules.canPayManaBlocks(diamonds, lapis, required)) {
            return false;
        }
        if (!consumeCount(player, Items.DIAMOND_BLOCK, AchievementRules.MANA_DIAMOND_COST)
                || !consumeCount(player, Items.LAPIS_BLOCK, required)) {
            return false;
        }
        player.setAttached(EhmAttachments.EHM_MANA_LAPIS_PAYMENTS, lapisPayments(player) + 1);
        return true;
    }

    static int requiredLapis(ServerPlayer player) {
        return AchievementRules.lapisCost(lapisPayments(player));
    }

    static int lapisPayments(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_MANA_LAPIS_PAYMENTS, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static int countItem(ServerPlayer player, Item item) {
        return InventorySearch.count(player, item);
    }

    static boolean consumeCount(ServerPlayer player, Item item, int amount) {
        return InventorySearch.consume(player, item, amount);
    }

    static boolean hasItem(ServerPlayer player, Item item) {
        return InventorySearch.has(player, item);
    }

    static boolean consumeOne(ServerPlayer player, Item item) {
        return InventorySearch.consumeOne(player, item);
    }

    static boolean consumeQuartz(ServerPlayer player) {
        return InventorySearch.consumeOne(player, Items.QUARTZ);
    }

    static boolean skipPlayer(Player player) {
        return player.isSpectator() || player.isCreative();
    }

    public static int sendClosest(ServerPlayer player, Consumer<Component> send) {
        Set<String> claimed = claimedKeys(player);
        List<AchievementRules.Progress> closest = AchievementRules.closest(
                player.getAttachedOrElse(EhmAttachments.EHM_BLOCK_PLACE_COUNTS, Map.of()),
                player.getAttachedOrElse(EhmAttachments.EHM_KILL_COUNTS, Map.of()),
                player.getAttachedOrElse(EhmAttachments.EHM_BLOCK_AWARDED, Map.of()),
                player.getAttachedOrElse(EhmAttachments.EHM_KILL_AWARDED, Map.of()),
                AchievementRules.CLOSEST_LIST_LIMIT,
                claimed);
        AchievementRules.Progress last = lastPlacedOf(player);
        if (closest.isEmpty() && last == null) {
            send.accept(Component.translatableWithFallback(
                    "tougher.command.achieve.empty",
                    "You have no in-progress achievements yet. Place blocks or defeat monsters to start them."));
            return 0;
        }
        send.accept(Component.translatableWithFallback(
                "tougher.command.achieve.paragraph",
                "Closest achievements: %s",
                joinProgress(closest, last)));
        return closest.size() + (last == null ? 0 : 1);
    }

    public static void sendWatchReport(ServerPlayer player, Consumer<Component> send) {
        Set<String> claimed = claimedKeys(player);
        List<AchievementRules.Progress> closest = AchievementRules.closestWatch(
                player.getAttachedOrElse(EhmAttachments.EHM_BLOCK_PLACE_COUNTS, Map.of()),
                player.getAttachedOrElse(EhmAttachments.EHM_KILL_COUNTS, Map.of()),
                player.getAttachedOrElse(EhmAttachments.EHM_BLOCK_AWARDED, Map.of()),
                player.getAttachedOrElse(EhmAttachments.EHM_KILL_AWARDED, Map.of()),
                claimed);
        AchievementRules.Progress last = lastPlacedOf(player);
        if (closest.isEmpty() && last == null) {
            return;
        }
        send.accept(Component.translatableWithFallback(
                "tougher.command.achieve.paragraph",
                "Closest achievements: %s",
                joinProgress(closest, last)));
    }

    static AchievementRules.Progress lastPlacedOf(ServerPlayer player) {
        String id = player.getAttachedOrElse(EhmAttachments.EHM_LAST_PLACED_BLOCK, "");
        if (id == null || id.isEmpty()) {
            return null;
        }
        return AchievementRules.lastPlaced(
                id,
                countOf(player, EhmAttachments.EHM_BLOCK_PLACE_COUNTS, id),
                countOf(player, EhmAttachments.EHM_BLOCK_AWARDED, id),
                claimedKeys(player));
    }

    static Set<String> claimedKeys(ServerPlayer player) {
        if (!ConfigManager.world(player.level()).competitiveAchievements()) {
            return Set.of();
        }
        return AchievementClaimData.of(player.level()).snapshot();
    }

    static Component joinProgress(List<AchievementRules.Progress> closest, AchievementRules.Progress lastPlaced) {
        var list = Component.empty();
        boolean first = true;
        for (AchievementRules.Progress progress : closest) {
            if (!first) {
                list.append("; ");
            }
            first = false;
            list.append(progressLine(progress, false));
        }
        if (lastPlaced != null) {
            if (!first) {
                list.append("; ");
            }
            list.append(progressLine(lastPlaced, true));
        }
        return list;
    }

    static Component progressLine(AchievementRules.Progress progress, boolean lastPlaced) {
        Component name = displayName(progress);
        Component count = Component.literal(Integer.toString(progress.count()));
        Component next = Component.literal(Integer.toString(progress.nextTarget()));
        if (lastPlaced) {
            return Component.translatableWithFallback(
                    "tougher.command.achieve.last_placed",
                    "Last placed %s: %s / %s",
                    name,
                    count,
                    next);
        }
        return Component.translatableWithFallback(
                "tougher.command.achieve.line", "%s: %s / %s", name, count, next);
    }

    public static Component displayName(AchievementRules.Progress progress) {
        String pretty = AchievementRules.prettyId(progress.id());
        Identifier id = Identifier.tryParse(progress.id());
        if (id == null) {
            return Component.literal(pretty);
        }
        if (progress.builder()) {
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                return Component.literal(pretty);
            }
            return BuiltInRegistries.BLOCK.getValue(id).getName();
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return Component.literal(pretty);
        }
        return BuiltInRegistries.ENTITY_TYPE.getValue(id).getDescription();
    }

    static String blockKey(Block block) {
        if (block == null || block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
            return null;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? null : id.toString();
    }

    static int countOf(
            ServerPlayer player,
            net.fabricmc.fabric.api.attachment.v1.AttachmentType<Map<String, Integer>> type,
            String key) {
        Map<String, Integer> map = player.getAttachedOrElse(type, Map.of());
        return map.getOrDefault(key, 0);
    }

    static void putCount(
            ServerPlayer player,
            net.fabricmc.fabric.api.attachment.v1.AttachmentType<Map<String, Integer>> type,
            String key,
            int value) {
        Map<String, Integer> map = TutorialCounts.mutableCopy(player.getAttachedOrElse(type, Map.of()));
        if (value <= 0) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
        player.setAttached(type, map);
    }

    static int manaLevel(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_MANA_LEVEL, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static double currentMana(ServerPlayer player) {
        Double value = player.getAttachedOrElse(EhmAttachments.EHM_MANA_CURRENT, 0.0);
        return value == null ? 0.0 : AchievementRules.clampMana(value);
    }
}
