package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.player.FoodHistory;
import dev.extrahardmode.world.WorldGate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Extra exhaustion while the player is doing anything, paused only when AFK.
 * Variety bonus on a new food in the last 7 eaten, repeat-food penalty,
 * no saturation-fast regen, slow regen 1 HP / 15s.
 */
public final class Hunger implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("hunger");

    /**
     * Last sampled pose. 26.2 snaps {@code xOld} to the current pos when movement
     * packets are applied, so {@code getX() - xOld} is 0 on dedicated servers.
     */
    private static final Map<UUID, Pose> LAST_POSE = new ConcurrentHashMap<>();

    private static final Map<UUID, Long> LAST_ACTIVITY_TICK = new ConcurrentHashMap<>();

    private record Pose(double x, double y, double z, float yaw, float pitch) {}

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerPlayConnectionEvents.DISCONNECT, ID, (handler, server) -> clear(handler.player.getUUID()));
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, (level, player, pos, state, blockEntity) -> noteActivity(player));
        bus.listen(UseBlockCallback.EVENT, ID, (player, level, hand, hit) -> {
            noteActivity(player);
            return InteractionResult.PASS;
        });
        bus.listen(UseItemCallback.EVENT, ID, (player, level, hand) -> {
            noteActivity(player);
            return InteractionResult.PASS;
        });
        bus.listen(AttackEntityCallback.EVENT, ID, (player, level, hand, entity, hit) -> {
            noteActivity(player);
            return InteractionResult.PASS;
        });
    }

    @Override
    public void serverTick(ServerLevel level) {
        long tick = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            sampleActivity(player, tick);
            applyExtraExhaustion(player, tick);
            WellFed.maintain(player);
        }
    }

    /** Movement packets, swings, and other player actions. */
    public static void noteActivity(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        LAST_ACTIVITY_TICK.put(serverPlayer.getUUID(), level.getGameTime());
    }

    public static void noteMovement(Player player, double dx, double dy, double dz) {
        if (HungerRules.isMoving(dx, dy, dz)) {
            noteActivity(player);
        }
    }

    private static void sampleActivity(ServerPlayer player, long tick) {
        Pose now = new Pose(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        Pose last = LAST_POSE.put(player.getUUID(), now);
        if (last != null
                && (HungerRules.isMoving(now.x() - last.x(), now.y() - last.y(), now.z() - last.z())
                        || HungerRules.isLooking(
                                HungerRules.wrapDegrees(now.yaw() - last.yaw()),
                                now.pitch() - last.pitch()))) {
            LAST_ACTIVITY_TICK.put(player.getUUID(), tick);
            return;
        }
        Input input = player.getLastClientInput();
        if (input != null
                && HungerRules.hasControlInput(
                        input.forward(),
                        input.backward(),
                        input.left(),
                        input.right(),
                        input.jump(),
                        input.shift(),
                        input.sprint())) {
            LAST_ACTIVITY_TICK.put(player.getUUID(), tick);
        }
    }

    private static void applyExtraExhaustion(ServerPlayer player, long tick) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || player.isSpectator()
                || EhmApi.playerBypasses(player)) {
            return;
        }
        int timeoutTicks = HungerRules.activityTimeoutTicks(ConfigManager.world(level).afkTimeoutSeconds());
        if (HungerRules.isAfk(tick, LAST_ACTIVITY_TICK.get(player.getUUID()), timeoutTicks)) {
            return;
        }
        float exhaustion = HungerRules.exhaustionPerTick(ConfigManager.world(level).movingExhaustionPerSecond());
        if (exhaustion > 0.0F) {
            player.causeFoodExhaustion(exhaustion);
        }
    }

    private static void clear(UUID id) {
        LAST_POSE.remove(id);
        LAST_ACTIVITY_TICK.remove(id);
    }

    public static boolean overridesRegen(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Level level = serverPlayer.level();
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, ID)) {
            return false;
        }
        if (EhmApi.playerBypasses(serverPlayer) || serverPlayer.isSpectator()) {
            return false;
        }
        return ConfigManager.world(serverLevel).disableFastSaturationRegen();
    }

    /** How much hunger this player can hold before eating is refused. */
    public static int edibleUntil(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return WellFedRules.foodMax(WellFed.level(serverPlayer));
        }
        return ClientFoodCap.lookup.cap(player);
    }

    public static int slowRegenTicks(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.level() instanceof ServerLevel level)) {
            return HungerRules.VANILLA_SLOW_REGEN_TICKS;
        }
        if (!overridesRegen(serverPlayer)) {
            return HungerRules.VANILLA_SLOW_REGEN_TICKS;
        }
        return WellFedRules.regenTicks(ConfigManager.world(level).slowRegenTicks(), WellFed.level(serverPlayer));
    }

    public static void onFoodEaten(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (EhmApi.playerBypasses(serverPlayer) || serverPlayer.isSpectator()) {
            return;
        }
        noteActivity(serverPlayer);
        String foodId = itemId(stack);
        if (foodId == null) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        int historySize = Math.max(1, config.foodHistorySize());
        List<String> recent = serverPlayer.getAttachedOrElse(EhmAttachments.EHM_FOOD_HISTORY, List.of());
        boolean novel = FoodHistory.isNovelInLast(recent, foodId, historySize);
        int wellFedBefore = Math.max(0, serverPlayer.getAttachedOrElse(EhmAttachments.EHM_WELL_FED, 0));
        List<String> updated = FoodHistory.record(recent, foodId, WellFedRules.HISTORY_CAP);
        serverPlayer.setAttached(EhmAttachments.EHM_FOOD_HISTORY, updated);
        int wellFed = WellFed.update(serverPlayer, updated);
        int maxFood = WellFedRules.foodMax(wellFed);
        if (novel) {
            applyVarietyBonus(serverPlayer.getFoodData(), maxFood);
            celebrateVariety(serverPlayer, level);
        }
        if (FoodHistory.lastAreAllDifferent(updated, HungerRules.DEFAULT_FOOD_HISTORY)) {
            applyUniqueWindowBonus(serverPlayer.getFoodData(), maxFood);
            celebrateUniqueWindow(serverPlayer, level);
        }
        int count = FoodHistory.countInLast(updated, foodId, WellFedRules.penaltyWindow(wellFed));
        int penalty = WellFedRules.repeatPenalty(count, wellFed);
        if (penalty > 0) {
            applyRepeatPenalty(serverPlayer.getFoodData(), penalty, maxFood);
            warnRepeatFood(serverPlayer, level, stack, foodId, count);
        }
        WellFed.clampFood(serverPlayer, maxFood);
        if (WellFedRules.novelFoodMana(novel, wellFed, wellFed > wellFedBefore)) {
            Achievements.shiftMana(serverPlayer, 1);
        } else {
            Achievements.sendMana(serverPlayer);
        }
    }

    static void applyUniqueWindowBonus(FoodData food, int maxFood) {
        int level = HungerRules.foodAfterUniqueWindowBonus(food.getFoodLevel(), maxFood, true);
        float saturation = HungerRules.saturationAfterUniqueWindowBonus(level, food.getSaturationLevel(), maxFood, true);
        food.setFoodLevel(level);
        food.setSaturation(saturation);
    }

    static void celebrateUniqueWindow(ServerPlayer player, ServerLevel level) {
        int xp = HungerRules.uniqueWindowExperience(true);
        if (xp > 0) {
            player.giveExperiencePoints(xp);
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.food_unique_window",
                "You ate seven different foods! Extra +1 hunger and saturation, and 3 experience."));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                0.4F,
                1.6F);
    }

    static void applyRepeatPenalty(FoodData food, int penalty, int maxFood) {
        int level = HungerRules.foodAfterRepeatPenalty(food.getFoodLevel(), penalty, maxFood);
        float saturation = HungerRules.saturationAfterRepeatPenalty(level, food.getSaturationLevel(), penalty);
        food.setFoodLevel(level);
        food.setSaturation(saturation);
    }

    static void warnRepeatFood(
            ServerPlayer player, ServerLevel level, ItemStack stack, String foodId, int streak) {
        Component name = foodDisplayName(stack, foodId);
        if (streak >= HungerRules.SICK_STREAK) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.food_sick", "You are getting sick of %s!", name));
            level.playSound(
                    null, player.blockPosition(), SoundEvents.PIGLIN_ANGRY, SoundSource.PLAYERS, 0.8F, 1.0F);
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.food_tired",
                "You are getting tired of %s. Try changing it up.",
                name));
        level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    static Component foodDisplayName(ItemStack stack, String foodId) {
        if (stack != null && !stack.isEmpty()) {
            return stack.getHoverName();
        }
        Identifier id = foodId == null ? null : Identifier.tryParse(foodId);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(id)).getHoverName();
        }
        return Component.literal(foodId == null ? "food" : foodId);
    }

    static void celebrateVariety(ServerPlayer player, ServerLevel level) {
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.food_variety", "You appreciate the food variety! +1 bonus"));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.7F,
                1.4F);
        level.sendParticles(
                ParticleTypes.HEART,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.7,
                player.getZ(),
                6,
                0.4,
                0.25,
                0.4,
                0.02);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5,
                player.getZ(),
                4,
                0.3,
                0.2,
                0.3,
                0.02);
    }

    static void applyVarietyBonus(FoodData food, int maxFood) {
        int level = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        if (level >= maxFood) {
            food.setSaturation(HungerRules.saturationAfterVarietyBonus(level, saturation, maxFood, true));
        } else {
            food.setFoodLevel(HungerRules.foodAfterVarietyBonus(level, maxFood, true));
        }
    }

    static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(null);
    }
}
