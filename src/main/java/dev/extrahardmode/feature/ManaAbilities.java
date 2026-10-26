package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.module.TutorialCounts;
import dev.extrahardmode.network.ClientboundAbilityDurationsPayload;
import dev.extrahardmode.network.ClientboundAbilityReadyPayload;
import dev.extrahardmode.network.ClientboundFlightPayload;
import dev.extrahardmode.network.ClientboundPowerMinePayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Mana abilities: heal, iron heart, magic arrow, flight, let it grow, let there be light, power mining, detect ore, slow, sense evil, smite evil, master builder, diamond skin, empower, battery, lapis shield, write tome, read tome. */
public final class ManaAbilities implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("mana_abilities");
    /** Look range for Lapis Shield when the click itself did not land on a character. */
    private static final double LAPIS_SHIELD_LOOK_RANGE = 5.0;
    private static final int LAPIS_SILENT = 0;
    private static final int LAPIS_CANCEL = 1;
    private static final int LAPIS_EXPIRE = 2;
    private static final int LAPIS_BROKE = 3;
    private static final Map<UUID, Identifier> SLOWED = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(AttackEntityCallback.EVENT, ID, ManaAbilities::onAttackEntity);
        bus.listen(UseItemCallback.EVENT, ID, ManaAbilities::onUseItem);
        bus.listen(UseBlockCallback.EVENT, ID, ManaAbilities::onUseBlock);
        bus.listen(UseEntityCallback.EVENT, ID, ManaAbilities::onUseEntity);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, ManaAbilities::onDeath);
        bus.listen(ServerPlayConnectionEvents.DISCONNECT, ID, (handler, server) -> {
            if (handler.player instanceof ServerPlayer player) {
                endOutgoingLapisShield(player, LAPIS_SILENT);
                endLapisShieldOn(player, LAPIS_SILENT);
            }
        });
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        SlowWeb.discardLevel(level);
        if (level.getServer() == null) {
            return;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            String dim = player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_DIM, "");
            if (level.dimension().identifier().toString().equals(dim)) {
                clearPlayerLight(player);
            }
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            tickGoldWear(player);
            tickGoldShimmer(player);
            tickEmpower(player);
            tickBattery(player);
            tickLapisShield(player);
            tickFlight(player);
            tickPowerMine(player);
            tickPlayerLight(player);
            tickAbilityHint(player);
            tickHealSparkle(player);
            tickIronHeart(player);
            tickIronHeartSparkle(player);
            tickMasterBuilder(player);
            tickDiamondSkin(player);
            tickDiamondSkinSparkle(player);
            tickSenseSparkle(player);
            maybeSendAbilityDurations(player);
            syncStrongCastHud(player, false);
        }
        SlowWeb.tick(level);
        tickSlow(level);
    }

    public static InteractionResult onAttackEntity(
            Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
        return InteractionResult.PASS;
    }

    static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && !targetingUsableBlock(serverPlayer)) {
            if (serverPlayer.getMainHandItem().is(ItemTags.PICKAXES)) {
                tryPowerMine(serverPlayer);
            }
            if (serverPlayer.getMainHandItem().is(Items.COMPASS) && tryDetectOre(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.PAPER) && tryHeal(serverPlayer, null)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.IRON_INGOT) && tryIronHeart(serverPlayer, null)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.FEATHER) && tryFlight(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.ARROW) && tryMagicArrow(serverPlayer, null)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.STRING) && trySlow(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.SPIDER_EYE) && trySenseEvil(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.GOLDEN_SWORD) && trySmiteEvil(serverPlayer, null)) {
                return InteractionResult.FAIL;
            }
            if (isLightCoal(serverPlayer.getMainHandItem()) && tryLetThereBeLight(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.BRICK) && tryMasterBuilder(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.DIAMOND) && tryDiamondSkin(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.REDSTONE) && tryEmpower(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.QUARTZ_BLOCK) && tryBattery(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.LAPIS_LAZULI) && tryLapisShield(serverPlayer, null)) {
                return InteractionResult.FAIL;
            }
            if (tryReadTome(serverPlayer)) {
                return InteractionResult.FAIL;
            }
            if (serverPlayer.getMainHandItem().is(Items.BOOK) && tryWriteTome(serverPlayer)) {
                return InteractionResult.FAIL;
            }
        }
        skipGrowAfterUse(player, level, hand);
        return InteractionResult.PASS;
    }

    static InteractionResult onUseBlock(
            Player player, Level level, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getItemInHand(hand).is(ItemTags.HOES)
                && GrowPlants.isPlant(serverLevel, hit.getBlockPos())) {
            if (tryGrowFocused(serverPlayer, hit.getBlockPos())) {
                return InteractionResult.FAIL;
            }
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && skipsManaAbilityUse(serverPlayer, hit.getBlockPos())) {
            skipGrowAfterUse(player, level, hand);
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(ItemTags.PICKAXES)) {
            tryPowerMine(serverPlayer);
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.COMPASS)
                && tryDetectOre(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.PAPER)
                && tryHeal(serverPlayer, null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.IRON_INGOT)
                && tryIronHeart(serverPlayer, null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.FEATHER)
                && tryFlight(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.ARROW)
                && tryMagicArrow(serverPlayer, null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.STRING)
                && trySlow(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.SPIDER_EYE)
                && trySenseEvil(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.GOLDEN_SWORD)
                && trySmiteEvil(serverPlayer, null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && isLightCoal(serverPlayer.getMainHandItem())
                && tryLetThereBeLight(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.DIAMOND)
                && tryDiamondSkin(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.LAPIS_LAZULI)
                && tryLapisShield(serverPlayer, null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && tryReadTome(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.BOOK)
                && tryWriteTome(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        skipGrowAfterUse(player, level, hand);
        return InteractionResult.PASS;
    }

    static InteractionResult onUseEntity(
            Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.PAPER)
                && tryHeal(serverPlayer, entity instanceof LivingEntity living ? living : null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.IRON_INGOT)
                && entity instanceof ServerPlayer
                && tryIronHeart(serverPlayer, entity instanceof LivingEntity living ? living : null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.ARROW)
                && tryMagicArrow(serverPlayer, entity instanceof LivingEntity living ? living : null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.GOLDEN_SWORD)
                && trySmiteEvil(serverPlayer, entity instanceof LivingEntity living ? living : null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.LAPIS_LAZULI)
                && tryLapisShield(serverPlayer, entity instanceof LivingEntity living ? living : null)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && tryReadTome(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && WorldGate.isModuleActive(serverLevel, ID)
                && serverPlayer.getMainHandItem().is(Items.BOOK)
                && tryWriteTome(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    static void skipGrowAfterUse(Player player, Level level, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID)) {
            return;
        }
        if (serverPlayer.getItemInHand(hand).is(ItemTags.HOES)) {
            markHandled(serverPlayer);
        }
    }

    static boolean tryFlight(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.FEATHER) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (isFlying(player)) {
            markHandled(player);
            return true;
        }
        if (blockedByCooldownOrMana(player, AbilityRules.FLIGHT, "tougher.ability.flight", "Flight")) {
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.FLIGHT, bonus);
        consumeManaAndHeld(player, Items.FEATHER);
        recordAbilityUse(player, AbilityRules.FLIGHT);
        startFlight(player, power);
        castEffect(player, SoundEvents.PHANTOM_FLAP, ParticleTypes.CLOUD, ParticleTypes.END_ROD);
        tellAbilityUse(player, "tougher.ability.flight", "Flight", power);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.flight_start",
                "Jump to rise, sneak in the air to descend. Sneak on a block to stop flying."));
        markHandled(player);
        return true;
    }

    static boolean tryHeal(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.PAPER) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        boolean honey = InventorySearch.hasOneMain(player, Items.HONEY_BOTTLE);
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount())
                + (honey ? AbilityRules.HONEY_HEAL_BONUS : 0);
        double power = abilityPower(player, AbilityRules.HEAL, bonus);
        ServerPlayer target = healTarget(player, clicked, AbilityRules.healRange(power));
        if (target == null) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.HEAL, "tougher.ability.heal", "Healing", honey)) {
            return true;
        }
        if (honey) {
            InventorySearch.consumeOneMain(player, Items.HONEY_BOTTLE);
        }
        spend(player, Items.PAPER, AbilityRules.HEAL);
        startHealSparkle(target, (float) power);
        tellAbilityUse(player, "tougher.ability.heal", "Healing", power);
        return true;
    }

    static boolean tryIronHeart(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.IRON_INGOT) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (remainingIronHeartBuff(player) > 0) {
            cancelIronHeart(player);
            markHandled(player);
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.IRON_HEART, bonus);
        ServerPlayer target = ironHeartTarget(player, clicked, AbilityRules.ironHeartRange(power));
        int duration = AbilityRules.ironHeartDurationTicks(power);
        float health = AbilityRules.ironHeartHealth(power);
        if (duration <= 0 || health <= 0.0F) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.IRON_HEART, "tougher.ability.iron_heart", "Iron Heart")) {
            return true;
        }
        spend(player, Items.IRON_INGOT, AbilityRules.IRON_HEART);
        startIronHeartLock(player, duration);
        applyIronHeartBuff(player, target, health, duration);
        startIronHeartSparkle(target);
        tellAbilityUse(player, "tougher.ability.iron_heart", "Iron Heart", power);
        return true;
    }

    /** Blue sparkle with no healing. Leaves a heal already in progress on its own clock. */
    static void startHealSparkle(ServerPlayer target) {
        ServerLevel level = (ServerLevel) target.level();
        long now = level.getGameTime();
        Long existing = target.getAttachedOrElse(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, -1L);
        float owed = target.getAttachedOrElse(EhmAttachments.EHM_HEAL_REMAINING, 0.0F);
        if (owed <= 0.0F || existing == null || existing <= now) {
            target.setAttached(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, now + AbilityRules.HEAL_SPARKLE_TICKS);
        }
        spawnHealSparkle(target, level);
    }

    /** Adds this cast to the health still owed and spreads it across a fresh sparkle. */
    static void startHealSparkle(ServerPlayer target, float amount) {
        ServerLevel level = (ServerLevel) target.level();
        float owed = target.getAttachedOrElse(EhmAttachments.EHM_HEAL_REMAINING, 0.0F);
        if (owed < 0.0F) {
            owed = 0.0F;
        }
        target.setAttached(EhmAttachments.EHM_HEAL_REMAINING, owed + Math.max(0.0F, amount));
        target.setAttached(
                EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, level.getGameTime() + AbilityRules.HEAL_SPARKLE_TICKS);
        spawnHealSparkle(target, level);
    }

    static void tickHealSparkle(ServerPlayer player) {
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, -1L);
        float remaining = player.getAttachedOrElse(EhmAttachments.EHM_HEAL_REMAINING, 0.0F);
        if ((until == null || until < 0L) && remaining <= 0.0F) {
            return;
        }
        if (!player.isAlive()) {
            clearHealSparkle(player);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long ticksLeft = until == null ? 0L : until - level.getGameTime();
        if (ticksLeft <= 0L) {
            if (remaining > 0.0F) {
                player.heal(remaining);
            }
            clearHealSparkle(player);
            return;
        }
        HealSparkleRules.Slice slice = HealSparkleRules.slice(remaining, ticksLeft);
        if (slice.apply() > 0.0F) {
            player.heal(slice.apply());
        }
        player.setAttached(EhmAttachments.EHM_HEAL_REMAINING, slice.remaining());
        spawnHealSparkle(player, level);
    }

    private static void clearHealSparkle(ServerPlayer player) {
        player.setAttached(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, -1L);
        player.setAttached(EhmAttachments.EHM_HEAL_REMAINING, 0.0F);
    }

    static void spawnHealSparkle(ServerPlayer target, ServerLevel level) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.6;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.GLOW, x, y, z, 3, 0.35, 0.45, 0.35, 0.0);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.3F, 0.55F, 1.0F),
                x,
                y,
                z,
                2,
                0.3,
                0.4,
                0.3,
                0.0);
    }

    static boolean isIronHeartLocked(ServerPlayer player) {
        return remainingIronHeartLock(player) > 0;
    }

    static int remainingIronHeartLock(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_IRON_HEART_LOCK, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static int remainingIronHeartBuff(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_IRON_HEART_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static void startIronHeartLock(ServerPlayer player, int duration) {
        player.setAttached(EhmAttachments.EHM_IRON_HEART_LOCK, duration);
    }

    static void applyIronHeartBuff(ServerPlayer caster, ServerPlayer target, float health, int duration) {
        caster.setAttached(EhmAttachments.EHM_IRON_HEART_TARGET, target.getUUID());
        target.setAttached(EhmAttachments.EHM_IRON_HEART_CASTER, caster.getUUID());
        target.setAttached(EhmAttachments.EHM_IRON_HEART_REMAINING, duration);
        target.setAttached(EhmAttachments.EHM_IRON_HEART_BAR_MAX, duration);
        target.setAttached(EhmAttachments.EHM_IRON_HEART_AMOUNT, health);
        AttributeInstance max = target.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) {
            max.addOrUpdateTransientModifier(new AttributeModifier(
                    ExtraHardModeMod.IRON_HEART, health, AttributeModifier.Operation.ADD_VALUE));
        }
        sendAbilityDurations(target);
        if (target != caster) {
            sendAbilityDurations(caster);
        }
    }

    static void cancelIronHeart(ServerPlayer player) {
        if (remainingIronHeartBuff(player) <= 0) {
            return;
        }
        clearIronHeartBuff(player, false);
        sendAbilityDurations(player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.iron_heart_cancel", "You cancel Iron Heart."));
    }

    static void clearIronHeartBuff(ServerPlayer player, boolean message) {
        boolean had = remainingIronHeartBuff(player) > 0;
        UUID casterId = player.getAttached(EhmAttachments.EHM_IRON_HEART_CASTER);
        player.setAttached(EhmAttachments.EHM_IRON_HEART_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_IRON_HEART_AMOUNT, 0.0F);
        player.setAttached(EhmAttachments.EHM_IRON_HEART_BAR_MAX, 0);
        player.setAttached(EhmAttachments.EHM_IRON_HEART_CASTER, null);
        if (casterId != null && player.level().getServer() != null) {
            ServerPlayer caster = player.level().getServer().getPlayerList().getPlayer(casterId);
            UUID currentTarget = caster == null ? null : caster.getAttached(EhmAttachments.EHM_IRON_HEART_TARGET);
            if (caster != null && player.getUUID().equals(currentTarget)) {
                caster.setAttached(EhmAttachments.EHM_IRON_HEART_TARGET, null);
                caster.setAttached(EhmAttachments.EHM_IRON_HEART_LOCK, 0);
            }
        }
        AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) {
            max.removeModifier(ExtraHardModeMod.IRON_HEART);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        if (message && had) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.iron_heart_end", "Your Iron Heart fades."));
        }
    }

    static void tickIronHeart(ServerPlayer player) {
        int lock = remainingIronHeartLock(player);
        if (lock > 0) {
            player.setAttached(EhmAttachments.EHM_IRON_HEART_LOCK, lock - 1);
        }
        int remaining = remainingIronHeartBuff(player);
        if (remaining <= 0) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            clearIronHeartBuff(player, false);
            sendAbilityDurations(player);
            return;
        }
        remaining--;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_IRON_HEART_REMAINING, remaining);
            return;
        }
        ServerPlayer caster = ironHeartCaster(player);
        if (tryRenewIronHeart(caster != null ? caster : player, player)) {
            return;
        }
        clearIronHeartBuff(player, true);
        sendAbilityDurations(player);
    }

    static ServerPlayer ironHeartCaster(ServerPlayer recipient) {
        UUID id = recipient.getAttached(EhmAttachments.EHM_IRON_HEART_CASTER);
        if (id == null) {
            return recipient;
        }
        if (id.equals(recipient.getUUID())) {
            return recipient;
        }
        var server = recipient.level().getServer();
        if (server == null) {
            return recipient;
        }
        ServerPlayer caster = server.getPlayerList().getPlayer(id);
        return caster != null ? caster : recipient;
    }

    static boolean tryRenewIronHeart(ServerPlayer caster, ServerPlayer target) {
        if (caster == null || target == null || !caster.isAlive() || Achievements.skipPlayer(caster)) {
            return false;
        }
        if (!AbilityRules.hasManaToUse(AbilityRules.IRON_HEART, Achievements.currentMana(caster))) {
            return false;
        }
        caster.setAttached(
                EhmAttachments.EHM_MANA_CURRENT,
                Math.max(0.0, Achievements.currentMana(caster) - AbilityRules.manaCost(AbilityRules.IRON_HEART)));
        Achievements.sendMana(caster);
        recordAbilityUse(caster, AbilityRules.IRON_HEART);
        int bonus = renewCatalystBonus(caster, Items.IRON_INGOT);
        double power = abilityPower(caster, AbilityRules.IRON_HEART, bonus);
        int duration = AbilityRules.ironHeartDurationTicks(power);
        float health = AbilityRules.ironHeartHealth(power);
        if (duration <= 0 || health <= 0.0F) {
            return false;
        }
        consumeRenewCatalyst(caster, Items.IRON_INGOT);
        startIronHeartLock(caster, duration);
        applyIronHeartBuff(caster, target, health, duration);
        startIronHeartSparkle(target);
        tellAbilityUse(caster, "tougher.ability.iron_heart", "Iron Heart", power);
        return true;
    }

    public static boolean masterBuilderActive(ServerPlayer player) {
        return remainingMasterBuilder(player) > 0;
    }

    static int remainingMasterBuilder(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_MASTER_BUILDER_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    /**
     * Right-click with a clay brick. A usable block such as a chest still skips the cast.
     * A second click cancels. Renewal spends mana, and one brick only while it is in hand.
     */
    static boolean tryMasterBuilder(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.BRICK) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (masterBuilderActive(player)) {
            cancelMasterBuilder(player);
            markHandled(player);
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.MASTER_BUILDER, bonus);
        int duration = AbilityRules.masterBuilderDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.MASTER_BUILDER, "tougher.ability.master_builder", "Master Builder")) {
            return true;
        }
        spend(player, Items.BRICK, AbilityRules.MASTER_BUILDER);
        startMasterBuilder(player, duration, power);
        castMasterBuilder(player);
        tellAbilityUse(player, "tougher.ability.master_builder", "Master Builder", power);
        return true;
    }

    static void startMasterBuilder(ServerPlayer player, int duration, double power) {
        player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_POWER, (float) power);
        applyMasterBuilderReach(player);
        sendAbilityDurations(player);
    }

    static void applyMasterBuilderReach(ServerPlayer player) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance reach =
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        if (reach == null) {
            return;
        }
        float power = player.getAttachedOrElse(EhmAttachments.EHM_MASTER_BUILDER_POWER, 0.0F);
        if (power <= 0.0F) {
            reach.removeModifier(ExtraHardModeMod.MASTER_BUILDER_REACH);
            return;
        }
        reach.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                ExtraHardModeMod.MASTER_BUILDER_REACH,
                power,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }

    static void clearMasterBuilderReach(ServerPlayer player) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance reach =
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(ExtraHardModeMod.MASTER_BUILDER_REACH);
        }
    }

    static void cancelMasterBuilder(ServerPlayer player) {
        if (!masterBuilderActive(player)) {
            return;
        }
        clearMasterBuilder(player, false);
        sendAbilityDurations(player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.master_builder_cancel", "You cancel Master Builder."));
    }

    static void clearMasterBuilder(ServerPlayer player, boolean message) {
        boolean had = masterBuilderActive(player);
        player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_BAR_MAX, 0);
        player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_POWER, 0.0F);
        clearMasterBuilderReach(player);
        if (message && had) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.master_builder_end", "Master Builder ends."));
        }
    }

    static void tickMasterBuilder(ServerPlayer player) {
        int remaining = remainingMasterBuilder(player);
        if (remaining <= 0) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            clearMasterBuilder(player, false);
            sendAbilityDurations(player);
            return;
        }
        remaining--;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_MASTER_BUILDER_REMAINING, remaining);
            applyMasterBuilderReach(player);
            return;
        }
        if (tryRenewMasterBuilder(player)) {
            return;
        }
        clearMasterBuilder(player, true);
        sendAbilityDurations(player);
    }

    static boolean tryRenewMasterBuilder(ServerPlayer player) {
        if (player == null || !player.isAlive() || Achievements.skipPlayer(player)) {
            return false;
        }
        if (!AbilityRules.hasManaToUse(AbilityRules.MASTER_BUILDER, Achievements.currentMana(player))) {
            return false;
        }
        player.setAttached(
                EhmAttachments.EHM_MANA_CURRENT,
                Math.max(0.0, Achievements.currentMana(player) - AbilityRules.manaCost(AbilityRules.MASTER_BUILDER)));
        Achievements.sendMana(player);
        recordAbilityUse(player, AbilityRules.MASTER_BUILDER);
        int bonus = renewCatalystBonus(player, Items.BRICK);
        double power = abilityPower(player, AbilityRules.MASTER_BUILDER, bonus);
        int duration = AbilityRules.masterBuilderDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        consumeRenewCatalyst(player, Items.BRICK);
        startMasterBuilder(player, duration, power);
        castMasterBuilder(player);
        tellAbilityUse(player, "tougher.ability.master_builder", "Master Builder", power);
        return true;
    }

    static void castMasterBuilder(ServerPlayer player) {
        castEffect(player, SoundEvents.AMETHYST_BLOCK_CHIME, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.END_ROD);
    }

    public static boolean empowerActive(ServerPlayer player) {
        return remainingEmpower(player) > 0;
    }

    static int remainingEmpower(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_EMPOWER_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static double empowerLevel(ServerPlayer player) {
        if (!empowerActive(player)) {
            return 0.0;
        }
        Float value = player.getAttachedOrElse(EhmAttachments.EHM_EMPOWER_POWER, 0.0F);
        return value == null ? 0.0 : Math.max(0.0, value);
    }

    /**
     * Air use of redstone dust. A click that can place redstone wire never reaches this.
     * A second air click cancels. Renewal spends mana, and one dust only while it is in hand.
     */
    static boolean tryEmpower(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.REDSTONE) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (empowerActive(player)) {
            cancelEmpower(player);
            markHandled(player);
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.EMPOWER, bonus);
        int duration = AbilityRules.empowerDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        if (blockedByCooldownOrMana(player, AbilityRules.EMPOWER, "tougher.ability.empower", "Empower")) {
            return true;
        }
        spend(player, Items.REDSTONE, AbilityRules.EMPOWER);
        startEmpower(player, power, duration);
        castEmpower(player);
        tellAbilityUse(player, "tougher.ability.empower", "Empower", power);
        return true;
    }

    static void startEmpower(ServerPlayer player, double power, int duration) {
        player.setAttached(EhmAttachments.EHM_EMPOWER_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_EMPOWER_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_EMPOWER_POWER, (float) Math.max(0.0, power));
        sendAbilityDurations(player);
    }

    static void cancelEmpower(ServerPlayer player) {
        if (!empowerActive(player)) {
            return;
        }
        clearEmpower(player, false);
        sendAbilityDurations(player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.empower_cancel", "You cancel Empower."));
    }

    static void clearEmpower(ServerPlayer player, boolean message) {
        boolean had = empowerActive(player);
        player.setAttached(EhmAttachments.EHM_EMPOWER_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_EMPOWER_BAR_MAX, 0);
        player.setAttached(EhmAttachments.EHM_EMPOWER_POWER, 0.0F);
        if (message && had) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.empower_end", "Empower ends."));
        }
    }

    static void tickEmpower(ServerPlayer player) {
        int remaining = remainingEmpower(player);
        if (remaining <= 0) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            clearEmpower(player, false);
            sendAbilityDurations(player);
            return;
        }
        remaining--;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_EMPOWER_REMAINING, remaining);
            return;
        }
        clearEmpower(player, true);
        sendAbilityDurations(player);
    }

    static void castEmpower(ServerPlayer player) {
        castEffect(
                player,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                ParticleTypes.CRIT,
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.9F, 0.15F, 0.15F));
    }

    public static boolean batteryActive(ServerPlayer player) {
        return remainingBattery(player) > 0;
    }

    static int remainingBattery(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_BATTERY_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static double batteryLevel(ServerPlayer player) {
        if (!batteryActive(player)) {
            return 0.0;
        }
        Float value = player.getAttachedOrElse(EhmAttachments.EHM_BATTERY_POWER, 0.0F);
        return value == null ? 0.0 : Math.max(0.0, value);
    }

    /** Regen multiplier for this player. Inactive Battery stays at 1. */
    public static double batteryRegenMultiplier(ServerPlayer player) {
        if (player == null || !batteryActive(player)) {
            return 1.0;
        }
        return AbilityRules.batteryRegenMultiplier(batteryLevel(player));
    }

    /**
     * Air use of a quartz block. A click that can place the block never reaches this.
     * A second air click cancels. The cast spends the block and no mana.
     * While mana is below the player's mana level it renews, and that spends a quartz block.
     */
    static boolean tryBattery(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.QUARTZ_BLOCK) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (batteryActive(player)) {
            cancelBattery(player);
            markHandled(player);
            return true;
        }
        if (batteryReachedLevel(player)) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.battery_level", "Your mana has reached your level."));
            markHandled(player);
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.BATTERY, bonus);
        int duration = AbilityRules.batteryDurationTicks(power);
        if (blockedByCooldownOrMana(player, AbilityRules.BATTERY, "tougher.ability.battery", "Battery")) {
            return true;
        }
        spend(player, Items.QUARTZ_BLOCK, AbilityRules.BATTERY);
        startBattery(player, power, duration);
        castBattery(player);
        tellAbilityUse(player, "tougher.ability.battery", "Battery", power);
        return true;
    }

    static void startBattery(ServerPlayer player, double power, int duration) {
        player.setAttached(EhmAttachments.EHM_BATTERY_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_BATTERY_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_BATTERY_POWER, (float) Math.max(0.0, power));
        sendAbilityDurations(player);
    }

    static void cancelBattery(ServerPlayer player) {
        if (!batteryActive(player)) {
            return;
        }
        clearBattery(player, false);
        sendAbilityDurations(player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.battery_cancel", "You cancel Battery."));
    }

    static void clearBattery(ServerPlayer player, boolean message) {
        boolean had = batteryActive(player);
        player.setAttached(EhmAttachments.EHM_BATTERY_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_BATTERY_BAR_MAX, 0);
        player.setAttached(EhmAttachments.EHM_BATTERY_POWER, 0.0F);
        if (message && had) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.battery_end", "Battery ends."));
        }
    }

    static void tickBattery(ServerPlayer player) {
        int remaining = remainingBattery(player);
        if (remaining <= 0) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            clearBattery(player, false);
            sendAbilityDurations(player);
            return;
        }
        if (batteryReachedLevel(player)) {
            clearBattery(player, false);
            sendAbilityDurations(player);
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.battery_level", "Your mana has reached your level."));
            return;
        }
        remaining--;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_BATTERY_REMAINING, remaining);
            return;
        }
        if (tryRenewBattery(player)) {
            return;
        }
        clearBattery(player, true);
        sendAbilityDurations(player);
    }

    /**
     * Continues Battery when mana is still below the player's level.
     * One quartz block in hand is required. A stack of two or more spends two and adds +2.
     */
    static boolean tryRenewBattery(ServerPlayer player) {
        if (player == null || !player.isAlive() || Achievements.skipPlayer(player) || batteryReachedLevel(player)) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(Items.QUARTZ_BLOCK)) {
            return false;
        }
        int count = held.getCount();
        int take = AbilityRules.catalystConsumeCount(count);
        if (take <= 0) {
            return false;
        }
        int bonus = AbilityRules.catalystBonus(count);
        double power = abilityPower(player, AbilityRules.BATTERY, bonus);
        int duration = AbilityRules.batteryDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        held.shrink(take);
        recordAbilityUse(player, AbilityRules.BATTERY);
        startBattery(player, power, duration);
        castBattery(player);
        tellAbilityUse(player, "tougher.ability.battery", "Battery", power);
        return true;
    }

    static boolean batteryReachedLevel(ServerPlayer player) {
        if (player == null) {
            return true;
        }
        return AchievementRules.manaReachedLevel(Achievements.currentMana(player), Achievements.manaLevel(player));
    }

    static void castBattery(ServerPlayer player) {
        castEffect(
                player,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                ParticleTypes.END_ROD,
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.93F, 0.95F, 0.90F));
    }

    /**
     * Right-click with lapis lazuli. The targeted living character gets the shield.
     * With none targeted, the caster does. A second click cancels. It does not renew.
     */
    static boolean tryLapisShield(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.LAPIS_LAZULI) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (player.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET) != null) {
            endOutgoingLapisShield(player, outgoingLapisTicks(player) > 0 ? LAPIS_CANCEL : LAPIS_SILENT);
            markHandled(player);
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.LAPIS_SHIELD, bonus);
        int duration = AbilityRules.lapisShieldDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        LivingEntity target = lapisShieldTarget(player, clicked);
        if (shieldedByOther(target, player)) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.lapis_shield_busy", "That character already has a Lapis Shield."));
            markHandled(player);
            return true;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.LAPIS_SHIELD, "tougher.ability.lapis_shield", "Lapis Shield")) {
            return true;
        }
        spend(player, Items.LAPIS_LAZULI, AbilityRules.LAPIS_SHIELD);
        startLapisShield(player, target, power, duration);
        tellAbilityUse(player, "tougher.ability.lapis_shield", "Lapis Shield", power);
        return true;
    }

    static LivingEntity lapisShieldTarget(ServerPlayer caster, LivingEntity clicked) {
        if (isLapisTarget(clicked)) {
            return clicked;
        }
        EntityHitResult hit = raycast(caster, LAPIS_SHIELD_LOOK_RANGE);
        if (hit != null && hit.getEntity() instanceof LivingEntity living && isLapisTarget(living)) {
            return living;
        }
        return caster;
    }

    static boolean isLapisTarget(LivingEntity target) {
        return target != null && target.isAlive() && !(target instanceof ArmorStand);
    }

    static boolean shieldedByOther(LivingEntity target, ServerPlayer caster) {
        if (target == null || caster == null) {
            return false;
        }
        UUID owner = target.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER);
        if (owner == null || owner.equals(caster.getUUID())) {
            return false;
        }
        return incomingLapisTicks(target) > 0;
    }

    static void startLapisShield(ServerPlayer caster, LivingEntity target, double power, int duration) {
        long until = abilityNow(caster) + duration;
        float stored = (float) Math.max(0.0, power);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET, target.getUUID());
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET_PLAYER, target instanceof ServerPlayer);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_UNTIL, until);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_BAR_MAX, duration);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_POWER, stored);
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER, caster.getUUID());
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_UNTIL, until);
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_BAR_MAX, duration);
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_POWER, stored);
        sendAbilityDurations(caster);
        if (target instanceof ServerPlayer shielded && shielded != caster) {
            sendAbilityDurations(shielded);
        }
        spawnLapisBubble(target);
        if (target.level() instanceof ServerLevel level) {
            level.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.9F,
                    1.35F);
        }
    }

    static void endOutgoingLapisShield(ServerPlayer caster, int kind) {
        if (caster == null) {
            return;
        }
        UUID targetId = caster.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET);
        if (targetId == null) {
            return;
        }
        LivingEntity target = findLiving(caster.level().getServer(), targetId);
        if (target != null && caster.getUUID().equals(target.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER))) {
            clearLapisTargetFields(target);
            if (target instanceof ServerPlayer shielded && shielded != caster) {
                sendAbilityDurations(shielded);
                if (kind != LAPIS_SILENT) {
                    tellLapis(shielded, LAPIS_EXPIRE);
                }
            }
        }
        clearLapisCasterFields(caster);
        sendAbilityDurations(caster);
        tellLapis(caster, kind);
    }

    static void endLapisShieldOn(LivingEntity target, int kind) {
        if (target == null) {
            return;
        }
        UUID casterId = target.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER);
        if (casterId == null) {
            return;
        }
        clearLapisTargetFields(target);
        boolean self = target.getUUID().equals(casterId);
        if (target instanceof ServerPlayer shielded) {
            sendAbilityDurations(shielded);
            if (!self && kind != LAPIS_SILENT) {
                tellLapis(shielded, LAPIS_EXPIRE);
            }
        }
        MinecraftServer server = target.level() instanceof ServerLevel level ? level.getServer() : null;
        ServerPlayer caster = server == null ? null : server.getPlayerList().getPlayer(casterId);
        if (caster != null && target.getUUID().equals(caster.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET))) {
            clearLapisCasterFields(caster);
            sendAbilityDurations(caster);
            tellLapis(caster, kind);
        } else if (self && target instanceof ServerPlayer shielded) {
            tellLapis(shielded, kind);
        }
    }

    static void clearLapisCasterFields(ServerPlayer caster) {
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET, null);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET_PLAYER, Boolean.FALSE);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_UNTIL, -1L);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_BAR_MAX, 0);
        caster.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_POWER, 0.0F);
    }

    static void clearLapisTargetFields(LivingEntity target) {
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER, null);
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_UNTIL, -1L);
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_BAR_MAX, 0);
        target.setAttached(EhmAttachments.EHM_LAPIS_SHIELD_POWER, 0.0F);
    }

    static void tickLapisShield(ServerPlayer player) {
        UUID targetId = player.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET);
        if (targetId == null) {
            if (player.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER) != null && incomingLapisTicks(player) <= 0) {
                endLapisShieldOn(player, LAPIS_SILENT);
            }
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            endOutgoingLapisShield(player, LAPIS_SILENT);
            return;
        }
        if (outgoingLapisTicks(player) <= 0) {
            endOutgoingLapisShield(player, LAPIS_EXPIRE);
            return;
        }
        LivingEntity target = findLiving(player.level().getServer(), targetId);
        if (target == null) {
            if (Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_LAPIS_SHIELD_TARGET_PLAYER, Boolean.FALSE))) {
                endOutgoingLapisShield(player, LAPIS_EXPIRE);
            }
            return;
        }
        if (!target.isAlive()) {
            endOutgoingLapisShield(player, LAPIS_SILENT);
            return;
        }
        if (abilityNow(player) % 4L == 0L) {
            spawnLapisBubble(target);
        }
    }

    /**
     * Damage left after Lapis Shield, before helmet, armor, and health.
     * The caster pays one lapis lazuli per 3 points blocked, rounded up.
     * A short payment blocks nothing and drops the shield.
     */
    public static float absorbLapisShield(LivingEntity victim, ServerLevel level, DamageSource source, float damage) {
        if (victim == null || level == null || !(damage > 0.0F)) {
            return damage;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return damage;
        }
        UUID ownerId = victim.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER);
        if (ownerId == null) {
            return damage;
        }
        if (incomingLapisTicks(victim) <= 0) {
            endLapisShieldOn(victim, LAPIS_SILENT);
            return damage;
        }
        Float power = victim.getAttachedOrElse(EhmAttachments.EHM_LAPIS_SHIELD_POWER, 0.0F);
        float absorbed = LapisShieldRules.absorb(damage, power == null ? 0.0F : power);
        if (!(absorbed > 0.0F)) {
            return damage;
        }
        MinecraftServer server = level.getServer();
        ServerPlayer caster = server == null ? null : server.getPlayerList().getPlayer(ownerId);
        if (caster == null
                || !caster.isAlive()
                || Achievements.skipPlayer(caster)
                || !victim.getUUID().equals(caster.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET))) {
            endLapisShieldOn(victim, LAPIS_SILENT);
            return damage;
        }
        int cost = LapisShieldRules.lapisCost(absorbed);
        if (cost <= 0 || !InventorySearch.consumeMain(caster, Items.LAPIS_LAZULI, cost)) {
            endLapisShieldOn(victim, LAPIS_BROKE);
            return damage;
        }
        spawnLapisHit(victim);
        return LapisShieldRules.damageAfterAbsorb(damage, absorbed);
    }

    static void spawnLapisBubble(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        double rx = target.getBbWidth() * 0.5 + 0.35;
        double ry = target.getBbHeight() * 0.5 + 0.15;
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5;
        double cz = target.getZ();
        double spin = (level.getGameTime() % 40L) * (Math.PI * 2.0 / 40.0);
        var blue = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.22F, 0.42F, 0.95F);
        int points = 8;
        for (int i = 0; i < points; i++) {
            double angle = spin + (Math.PI * 2.0 * i) / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            level.sendParticles(blue, cx + rx * cos, cy, cz + rx * sin, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx + rx * cos, cy + ry * sin, cz, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    static void spawnLapisHit(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.7F, 1.6F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 10, 0.4, 0.45, 0.4, 0.04);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.35F, 0.55F, 1.0F),
                x,
                y,
                z,
                6,
                0.35,
                0.4,
                0.35,
                0.0);
    }

    static long abilityNow(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    static int ticksUntil(long until, long now) {
        if (until <= now) {
            return 0;
        }
        long left = until - now;
        return left > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) left;
    }

    static int outgoingLapisTicks(ServerPlayer player) {
        if (player == null || player.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_TARGET) == null) {
            return 0;
        }
        if (player.level().getServer() == null) {
            return 0;
        }
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_LAPIS_SHIELD_UNTIL, -1L);
        return ticksUntil(until == null ? -1L : until, abilityNow(player));
    }

    static int incomingLapisTicks(LivingEntity entity) {
        if (entity == null || entity.getAttached(EhmAttachments.EHM_LAPIS_SHIELD_CASTER) == null) {
            return 0;
        }
        if (!(entity.level() instanceof ServerLevel level) || level.getServer() == null) {
            return 0;
        }
        Long until = entity.getAttachedOrElse(EhmAttachments.EHM_LAPIS_SHIELD_UNTIL, -1L);
        return ticksUntil(until == null ? -1L : until, level.getServer().overworld().getGameTime());
    }

    static int lapisHudTicks(ServerPlayer player) {
        int outgoing = outgoingLapisTicks(player);
        if (outgoing > 0) {
            return outgoing;
        }
        return incomingLapisTicks(player);
    }

    static LivingEntity findLiving(MinecraftServer server, UUID id) {
        if (server == null || id == null) {
            return null;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online;
        }
        for (ServerLevel world : server.getAllLevels()) {
            Entity found = world.getEntity(id);
            if (found instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    static void tellLapis(ServerPlayer player, int kind) {
        if (player == null || kind == LAPIS_SILENT) {
            return;
        }
        if (kind == LAPIS_CANCEL) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.lapis_shield_cancel", "You cancel Lapis Shield."));
            return;
        }
        if (kind == LAPIS_BROKE) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.lapis_shield_broke", "Lapis Shield ends. Not enough lapis lazuli."));
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.lapis_shield_end", "Lapis Shield ends."));
    }

    public static boolean diamondSkinActive(ServerPlayer player) {
        return remainingDiamondSkin(player) > 0;
    }

    static int remainingDiamondSkin(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_DIAMOND_SKIN_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static double diamondSkinPower(ServerPlayer player) {
        Float value = player.getAttachedOrElse(EhmAttachments.EHM_DIAMOND_SKIN_POWER, 0.0F);
        return AbilityRules.diamondSkinLevel(value == null ? 0.0 : value);
    }

    /** Right-click with diamonds. A second click cancels. Renewal spends mana but not another diamond. */
    static boolean tryDiamondSkin(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.DIAMOND) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (diamondSkinActive(player)) {
            cancelDiamondSkin(player);
            markHandled(player);
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = AbilityRules.diamondSkinLevel(abilityPower(player, AbilityRules.DIAMOND_SKIN, bonus));
        int duration = AbilityRules.diamondSkinDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.DIAMOND_SKIN, "tougher.ability.diamond_skin", "Diamond Skin")) {
            return true;
        }
        spend(player, Items.DIAMOND, AbilityRules.DIAMOND_SKIN);
        startDiamondSkin(player, power, duration);
        castDiamondSkin(player);
        tellAbilityUse(player, "tougher.ability.diamond_skin", "Diamond Skin", power);
        return true;
    }

    static void startDiamondSkin(ServerPlayer player, double power, int duration) {
        player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_POWER, (float) Math.max(0.0, power));
        sendAbilityDurations(player);
    }

    static void cancelDiamondSkin(ServerPlayer player) {
        if (!diamondSkinActive(player)) {
            return;
        }
        clearDiamondSkin(player, false);
        sendAbilityDurations(player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.diamond_skin_cancel", "You cancel Diamond Skin."));
    }

    static void clearDiamondSkin(ServerPlayer player, boolean message) {
        boolean had = diamondSkinActive(player);
        player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_BAR_MAX, 0);
        player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_POWER, 0.0F);
        if (message && had) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.diamond_skin_end", "Diamond Skin ends."));
        }
    }

    static void tickDiamondSkin(ServerPlayer player) {
        int remaining = remainingDiamondSkin(player);
        if (remaining <= 0) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            clearDiamondSkin(player, false);
            sendAbilityDurations(player);
            return;
        }
        remaining--;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_REMAINING, remaining);
            return;
        }
        if (tryRenewDiamondSkin(player)) {
            return;
        }
        clearDiamondSkin(player, true);
        sendAbilityDurations(player);
    }

    static boolean tryRenewDiamondSkin(ServerPlayer player) {
        if (player == null || !player.isAlive() || Achievements.skipPlayer(player)) {
            return false;
        }
        if (!AbilityRules.hasManaToUse(AbilityRules.DIAMOND_SKIN, Achievements.currentMana(player))) {
            return false;
        }
        player.setAttached(
                EhmAttachments.EHM_MANA_CURRENT,
                Math.max(0.0, Achievements.currentMana(player) - AbilityRules.manaCost(AbilityRules.DIAMOND_SKIN)));
        Achievements.sendMana(player);
        recordAbilityUse(player, AbilityRules.DIAMOND_SKIN);
        int bonus = renewCatalystBonus(player, Items.DIAMOND);
        double power = AbilityRules.diamondSkinLevel(abilityPower(player, AbilityRules.DIAMOND_SKIN, bonus));
        int duration = AbilityRules.diamondSkinDurationTicks(power);
        if (duration <= 0) {
            return false;
        }
        consumeRenewCatalyst(player, Items.DIAMOND);
        startDiamondSkin(player, power, duration);
        castDiamondSkin(player);
        tellAbilityUse(player, "tougher.ability.diamond_skin", "Diamond Skin", power);
        return true;
    }

    static void castDiamondSkin(ServerPlayer player) {
        castEffect(player, SoundEvents.AMETHYST_BLOCK_CHIME, ParticleTypes.END_ROD, null);
        spawnDiamondSkinSparkle(player, (ServerLevel) player.level());
    }

    /**
     * Damage that is about to be subtracted from health, after armor and absorption.
     * Divided by the ability level stored when Diamond Skin was cast. Poison, drowning,
     * and suffocation are left alone.
     */
    public static float scaleDiamondSkin(
            ServerPlayer player, DamageSource source, float healthDamage, boolean sparkle) {
        if (player == null || source == null || healthDamage <= 0.0F || !diamondSkinActive(player)) {
            return healthDamage;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            return healthDamage;
        }
        boolean bypass = DiamondSkinRules.isPoisonVictim(player)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL);
        float scaled = DiamondSkinRules.scaleHealthDamage(healthDamage, diamondSkinPower(player), bypass);
        if (sparkle && DiamondSkinRules.reduced(healthDamage, scaled)) {
            startDiamondSkinSparkle(player);
            absorbDiamondSkinDuration(player, healthDamage - scaled);
        }
        return scaled;
    }

    /** Each point of health damage prevented removes 3 seconds. Running out tries to renew. */
    static void absorbDiamondSkinDuration(ServerPlayer player, float absorbed) {
        int loss = DiamondSkinRules.durationLossTicks(absorbed);
        if (loss <= 0 || !diamondSkinActive(player)) {
            return;
        }
        int remaining = DiamondSkinRules.remainingAfterAbsorption(remainingDiamondSkin(player), absorbed);
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_REMAINING, remaining);
            sendAbilityDurations(player);
            return;
        }
        if (tryRenewDiamondSkin(player)) {
            return;
        }
        clearDiamondSkin(player, true);
        sendAbilityDurations(player);
    }

    static void startDiamondSkinSparkle(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long until = level.getGameTime() + AbilityRules.DIAMOND_SKIN_SPARKLE_TICKS;
        Long current = player.getAttachedOrElse(EhmAttachments.EHM_DIAMOND_SKIN_SPARKLE_UNTIL, -1L);
        if (current == null || current < until) {
            player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_SPARKLE_UNTIL, until);
        }
        spawnDiamondSkinSparkle(player, level);
    }

    static void tickDiamondSkinSparkle(ServerPlayer player) {
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_DIAMOND_SKIN_SPARKLE_UNTIL, -1L);
        if (until == null || until < 0L) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (!player.isAlive() || until <= level.getGameTime()) {
            player.setAttached(EhmAttachments.EHM_DIAMOND_SKIN_SPARKLE_UNTIL, -1L);
            return;
        }
        spawnDiamondSkinSparkle(player, level);
    }

    static void spawnDiamondSkinSparkle(ServerPlayer target, ServerLevel level) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.6;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.28, 0.4, 0.28, 0.01);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.15F, 0.95F, 1.0F),
                x,
                y,
                z,
                4,
                0.3,
                0.4,
                0.3,
                0.0);
    }

    static void startIronHeartSparkle(ServerPlayer target) {
        ServerLevel level = (ServerLevel) target.level();
        target.setAttached(
                EhmAttachments.EHM_IRON_HEART_SPARKLE_UNTIL,
                level.getGameTime() + AbilityRules.IRON_HEART_SPARKLE_TICKS);
        spawnIronHeartSparkle(target, level);
    }

    static void tickIronHeartSparkle(ServerPlayer player) {
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_IRON_HEART_SPARKLE_UNTIL, -1L);
        if (until == null || until < 0L) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (!player.isAlive() || until <= level.getGameTime()) {
            player.setAttached(EhmAttachments.EHM_IRON_HEART_SPARKLE_UNTIL, -1L);
            return;
        }
        spawnIronHeartSparkle(player, level);
    }

    static void spawnIronHeartSparkle(ServerPlayer target, ServerLevel level) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.6;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 3, 0.35, 0.45, 0.35, 0.01);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 4, 0.3, 0.4, 0.3, 0.02);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.85F, 0.9F, 0.95F),
                x,
                y,
                z,
                2,
                0.3,
                0.4,
                0.3,
                0.0);
    }

    static boolean tryMagicArrow(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.ARROW) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        boolean charcoal = InventorySearch.hasOneMain(player, Items.CHARCOAL);
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount())
                + AbilityRules.charcoalArrowBonus(charcoal);
        double power = abilityPower(player, AbilityRules.MAGIC_ARROW, bonus);
        double range = AbilityRules.fireRange(power);
        LivingEntity target = fireTarget(player, clicked, range);
        if (target == null) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.MAGIC_ARROW, "tougher.ability.magic_arrow", "Magic arrow")) {
            return true;
        }
        hurlArrowVolley(player, power, range, target, charcoal);
        castEffect(player, SoundEvents.ARROW_SHOOT, ParticleTypes.CRIT, ParticleTypes.ENCHANTED_HIT);
        spend(player, Items.ARROW, AbilityRules.MAGIC_ARROW);
        if (charcoal) {
            InventorySearch.consumeOneMain(player, Items.CHARCOAL);
        }
        tellAbilityUse(player, "tougher.ability.magic_arrow", "Magic arrow", power);
        return true;
    }

    static boolean trySmiteEvil(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.GOLDEN_SWORD) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        int bonus = 0;
        double power = abilityPower(player, AbilityRules.SMITE_EVIL, bonus);
        double range = AbilityRules.smiteRange(power);
        LivingEntity target = fireTarget(player, clicked, range);
        if (target == null) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.SMITE_EVIL, "tougher.ability.smite_evil", "Smite Evil")) {
            return true;
        }
        spend(player, null, AbilityRules.SMITE_EVIL);
        player.attack(target);
        if (target.isAlive() && isUndead(target)) {
            float extra = AbilityRules.smiteUndeadBonus(power);
            if (extra > 0.0F) {
                target.setInvulnerableTime(0);
                target.hurt(player.damageSources().playerAttack(player), extra);
            }
        }
        spawnSmiteFlash(target, (ServerLevel) player.level());
        tellAbilityUse(player, "tougher.ability.smite_evil", "Smite Evil", power);
        return true;
    }

    static boolean isUndead(LivingEntity target) {
        return target != null && target.is(EntityTypeTags.UNDEAD);
    }

    static void spawnSmiteFlash(LivingEntity target, ServerLevel level) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.6;
        double z = target.getZ();
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.FLASH, 1.0F, 1.0F, 0.85F), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 6, 0.2, 0.35, 0.2, 0.02);
        level.sendParticles(ParticleTypes.GLOW, x, y, z, 4, 0.25, 0.4, 0.25, 0.0);
    }

    static boolean tryGrow(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ItemTags.HOES)) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        if (GrowPlants.findGrowable(level, origin, AbilityRules.GROW_RANGE).isEmpty()) {
            return false;
        }
        if (blockedByCooldownOrMana(player, AbilityRules.GROW, "tougher.ability.grow", "Let it grow")) {
            return true;
        }
        int hoeBonus = AbilityRules.growHoeBonus(heldItemId(held));
        double power = abilityPower(player, AbilityRules.GROW, hoeBonus);
        int times = Math.max(1, (int) Math.round(power));
        if (GrowPlants.apply(level, origin, AbilityRules.GROW_RANGE, times) <= 0) {
            return false;
        }
        return finishGrow(player, power);
    }

    /**
     * Right-click a plant: grow it by ability level stages (overflow to the
     * nearest plant within 2 blocks), and still apply the random scatter.
     */
    static boolean tryGrowFocused(ServerPlayer player, BlockPos target) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ItemTags.HOES) || Achievements.skipPlayer(player)) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        boolean canTarget = GrowPlants.canGrow(level, target)
                || GrowPlants.nearestGrowable(level, target, AbilityRules.GROW_RANGE, Set.of(target))
                        != null;
        boolean canScatter = !GrowPlants.findGrowable(level, origin, AbilityRules.GROW_RANGE).isEmpty();
        if (!canTarget && !canScatter) {
            return false;
        }
        if (blockedByCooldownOrMana(player, AbilityRules.GROW, "tougher.ability.grow", "Let it grow")) {
            return true;
        }
        int hoeBonus = AbilityRules.growHoeBonus(heldItemId(held));
        double power = abilityPower(player, AbilityRules.GROW, hoeBonus);
        int times = Math.max(1, (int) Math.round(power));
        Set<BlockPos> focused = new HashSet<>();
        int grown = GrowPlants.applyFocused(level, target, AbilityRules.GROW_RANGE, times, focused);
        grown += GrowPlants.apply(level, origin, AbilityRules.GROW_RANGE, times, focused);
        if (grown <= 0) {
            return false;
        }
        return finishGrow(player, power);
    }

    static boolean finishGrow(ServerPlayer player, double power) {
        spend(player, null, AbilityRules.GROW);
        castEffect(player, SoundEvents.BONE_MEAL_USE, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.COMPOSTER);
        ItemStack hoe = player.getMainHandItem();
        if (hoe.is(ItemTags.HOES) && hoe.isDamageableItem()) {
            hurtGrowHoe(player, hoe);
        }
        tellAbilityUse(player, "tougher.ability.grow", "Let it grow", power);
        return true;
    }

    /**
     * One Unbreaking roll, same as tilling once. Skip the whole ability cost
     * when that roll would have skipped 1 durability; otherwise take all 3.
     */
    static void hurtGrowHoe(ServerPlayer player, ItemStack hoe) {
        if (!hoe.isDamageableItem() || player.hasInfiniteMaterials()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        int useTaken = EnchantmentHelper.processDurabilityChange(level, hoe, 1);
        if (useTaken <= 0) {
            return;
        }
        applyGrowHoeDamage(player, hoe, AbilityRules.GROW_HOE_DAMAGE);
    }

    static void applyGrowHoeDamage(ServerPlayer player, ItemStack hoe, int amount) {
        if (amount <= 0) {
            return;
        }
        int newDamage = hoe.getDamageValue() + amount;
        CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, hoe, newDamage);
        hoe.setDamageValue(newDamage);
        if (hoe.isBroken()) {
            Item item = hoe.getItem();
            hoe.shrink(1);
            player.onEquippedItemBroken(new ItemStack(item), EquipmentSlot.MAINHAND);
        }
    }

    static boolean tryPowerMine(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ItemTags.PICKAXES) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (isPowerMining(player)) {
            markHandled(player);
            return true;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.POWER_MINE, "tougher.ability.power_mine", "Power mining")) {
            return true;
        }
        int bonus = AbilityRules.pickaxeBonus(heldItemId(held));
        double power = abilityPower(player, AbilityRules.POWER_MINE, bonus);
        int duration = AbilityRules.powerMineDurationTicks(power);
        spend(player, null, AbilityRules.POWER_MINE);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_ITEM, heldItemId(held));
        sendPowerMine(player);
        sendAbilityDurations(player);
        castEffect(player, SoundEvents.AMETHYST_BLOCK_CHIME, ParticleTypes.CRIT, ParticleTypes.ELECTRIC_SPARK);
        tellAbilityUse(player, "tougher.ability.power_mine", "Power mining", power);
        return true;
    }

    static void tickPowerMine(ServerPlayer player) {
        if (!isPowerMining(player)) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            stopPowerMine(player, true);
            return;
        }
        int remaining = remainingPowerMine(player) - 1;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_POWER_MINE_REMAINING, remaining);
            if (remaining % 5 == 0) {
                sendPowerMine(player);
            }
            return;
        }
        stopPowerMine(player, true);
    }

    static void stopPowerMine(ServerPlayer player, boolean message) {
        boolean wasActive = isPowerMining(player);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_BAR_MAX, 0);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_ITEM, "");
        sendPowerMineInactive(player);
        sendAbilityDurations(player);
        if (message && wasActive) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.power_mine_end", "Power mining ended."));
        }
    }

    static boolean tryDetectOre(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(Items.COMPASS) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (DetectOre.targetingLodestone(player)) {
            return false;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.DETECT_ORE, "tougher.ability.detect_ore", "Detect ore")) {
            return true;
        }
        int quartzStack = InventorySearch.largestMainStack(player, Items.QUARTZ);
        int bonus = AbilityRules.detectOreQuartzBonus(quartzStack);
        double power = abilityPower(player, AbilityRules.DETECT_ORE, bonus);
        int range = AbilityRules.detectOreScanRange(power);
        AbilityRules.OreDeposit best = DetectOre.findBest(player, range, power);
        InventorySearch.consumeLargestMainStack(
                player, Items.QUARTZ, AbilityRules.detectOreQuartzConsumeCount(quartzStack));
        spend(player, null, AbilityRules.DETECT_ORE);
        castEffect(player, SoundEvents.AMETHYST_BLOCK_RESONATE, ParticleTypes.ENCHANT, ParticleTypes.END_ROD);
        tellAbilityUse(player, "tougher.ability.detect_ore", "Detect ore", power);
        if (best == null) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.detect_ore_none", "The compass finds no ore nearby."));
        } else {
            DetectOre.lookAt(player, best);
            int blocks = AbilityRules.depositDistanceBlocks(best);
            String family = AbilityRules.oreFamilyLabel(best.family());
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.detect_ore_found",
                    AbilityRules.detectOreFoundFallback(family, blocks),
                    Component.literal(family),
                    Component.literal(Integer.toString(blocks))));
        }
        return true;
    }

    static boolean trySlow(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.STRING) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (blockedByCooldownOrMana(player, AbilityRules.SLOW, "tougher.ability.slow", "Slow")) {
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.SLOW, bonus);
        spend(player, Items.STRING, AbilityRules.SLOW);
        SlowWeb.launch(player, power);
        castEffect(player, SoundEvents.COBWEB_PLACE, ParticleTypes.ITEM_COBWEB, ParticleTypes.WHITE_ASH);
        tellAbilityUse(player, "tougher.ability.slow", "Slow", power);
        return true;
    }

    static boolean trySenseEvil(ServerPlayer player) {
        if (!player.getMainHandItem().is(Items.SPIDER_EYE) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.SENSE_EVIL, "tougher.ability.sense_evil", "Sense Evil")) {
            return true;
        }
        int bonus = AbilityRules.catalystBonus(player.getMainHandItem().getCount());
        double power = abilityPower(player, AbilityRules.SENSE_EVIL, bonus);
        spend(player, Items.SPIDER_EYE, AbilityRules.SENSE_EVIL);
        tellAbilityUse(player, "tougher.ability.sense_evil", "Sense Evil", power);
        if (!BiomeBosses.anySpawned(player.level().getServer())) {
            startHealSparkle(player);
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.sense_evil_none", AbilityRules.senseEvilNoneFallback()));
            return true;
        }
        startCreepySparkle(player);
        Entity nearest = BiomeBosses.nearestBoss(player);
        if (nearest != null && nearest.level() == player.level()) {
            lookAt(player, nearest.getX(), nearest.getY() + nearest.getBbHeight() * 0.5, nearest.getZ());
            double distance = Math.sqrt(player.distanceToSqr(nearest));
            if (AbilityRules.senseEvilShowsDistance(distance, power)) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.message.sense_evil_found_near",
                        AbilityRules.senseEvilFoundNearFallback(AbilityRules.roughBlocks(distance)),
                        Component.literal(Integer.toString(AbilityRules.roughBlocks(distance)))));
                return true;
            }
        } else if (nearest != null) {
            lookAt(player, nearest.getX(), nearest.getY() + nearest.getBbHeight() * 0.5, nearest.getZ());
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.sense_evil_found", AbilityRules.senseEvilFoundFallback()));
        return true;
    }

    static void lookAt(ServerPlayer player, double x, double y, double z) {
        Vec3 eye = player.getEyePosition();
        float yaw = AbilityRules.lookYaw(x - eye.x, z - eye.z);
        float pitch = AbilityRules.lookPitch(x - eye.x, y - eye.y, z - eye.z);
        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch, true);
    }

    static void startCreepySparkle(ServerPlayer target) {
        ServerLevel level = (ServerLevel) target.level();
        target.setAttached(
                EhmAttachments.EHM_SENSE_SPARKLE_UNTIL, level.getGameTime() + AbilityRules.SENSE_EVIL_SPARKLE_TICKS);
        spawnCreepySparkle(target, level);
    }

    static void tickSenseSparkle(ServerPlayer player) {
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_SENSE_SPARKLE_UNTIL, -1L);
        if (until == null || until < 0L) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (!player.isAlive() || until <= level.getGameTime()) {
            player.setAttached(EhmAttachments.EHM_SENSE_SPARKLE_UNTIL, -1L);
            return;
        }
        spawnCreepySparkle(player, level);
    }

    static void spawnCreepySparkle(ServerPlayer target, ServerLevel level) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.6;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 4, 0.35, 0.5, 0.35, 0.01);
        level.sendParticles(ParticleTypes.WITCH, x, y, z, 3, 0.3, 0.4, 0.3, 0.0);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.45F, 0.05F, 0.55F),
                x,
                y,
                z,
                2,
                0.3,
                0.4,
                0.3,
                0.0);
    }

    public static boolean tryLetThereBeLight(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!isLightCoal(held) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        if (alreadyHandled(player, level)) {
            return true;
        }
        if (remainingPlayerLight(player) > 0) {
            stopPlayerLight(player);
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.light_off", "You snuff the light."));
            markHandled(player);
            return true;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.LIGHT, "tougher.ability.light", "Let there be light")) {
            return true;
        }
        int bonus = AbilityRules.catalystBonus(held.getCount());
        double power = abilityPower(player, AbilityRules.LIGHT, bonus);
        spend(player, held.getItem(), AbilityRules.LIGHT);
        startPlayerLight(player, power);
        castEffect(player, SoundEvents.CONDUIT_ACTIVATE, ParticleTypes.END_ROD, ParticleTypes.GLOW);
        tellAbilityUse(player, "tougher.ability.light", "Let there be light", power);
        return true;
    }

    static boolean isLightCoal(ItemStack stack) {
        return stack != null && !stack.isEmpty() && AbilityRules.isLightCoal(heldItemId(stack));
    }

    static void startPlayerLight(ServerPlayer player, double power) {
        int duration = AbilityRules.lightDurationTicks(power);
        player.setAttached(EhmAttachments.EHM_LIGHT_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_LIGHT_BAR_MAX, duration);
        placeFollowLight(player);
        sendAbilityDurations(player);
    }

    static void tickPlayerLight(ServerPlayer player) {
        int remaining = remainingPlayerLight(player);
        if (remaining <= 0) {
            clearPlayerLight(player);
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            stopPlayerLight(player);
            return;
        }
        remaining--;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_LIGHT_REMAINING, remaining);
            placeFollowLight(player);
            return;
        }
        if (AbilityRules.hasManaToUse(AbilityRules.LIGHT, Achievements.currentMana(player))) {
            int bonus = renewCatalystBonus(player, Items.COAL);
            double power = abilityPower(player, AbilityRules.LIGHT, bonus);
            consumeRenewCatalyst(player, Items.COAL);
            player.setAttached(
                    EhmAttachments.EHM_MANA_CURRENT,
                    Math.max(0.0, Achievements.currentMana(player) - AbilityRules.manaCost(AbilityRules.LIGHT)));
            Achievements.sendMana(player);
            recordAbilityUse(player, AbilityRules.LIGHT);
            startPlayerLight(player, power);
            castEffect(player, SoundEvents.CONDUIT_ACTIVATE, ParticleTypes.END_ROD, ParticleTypes.GLOW);
            tellAbilityUse(player, "tougher.ability.light", "Let there be light", power);
            return;
        }
        stopPlayerLight(player);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.light_end", "Your light fades."));
    }

    static int remainingPlayerLight(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    static void stopPlayerLight(ServerPlayer player) {
        boolean wasLit = remainingPlayerLight(player) > 0;
        player.setAttached(EhmAttachments.EHM_LIGHT_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_LIGHT_BAR_MAX, 0);
        clearPlayerLight(player);
        sendAbilityDurations(player);
        if (wasLit && player.level().getServer() != null) {
            putLong(
                    player,
                    EhmAttachments.EHM_ABILITY_LAST_USE,
                    AbilityRules.LIGHT,
                    player.level().getServer().overworld().getGameTime());
        }
    }

    public static void clearPlayerLight(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Long packed = player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_POS, Long.MIN_VALUE);
        String dim = player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_DIM, "");
        player.setAttached(EhmAttachments.EHM_LIGHT_POS, Long.MIN_VALUE);
        player.setAttached(EhmAttachments.EHM_LIGHT_DIM, "");
        if (packed == null || packed == Long.MIN_VALUE) {
            return;
        }
        Identifier dimId = dim == null || dim.isEmpty() ? null : Identifier.tryParse(dim);
        ServerLevel level = player.level() instanceof ServerLevel current ? current : null;
        var server = player.level().getServer();
        if (dimId != null && server != null) {
            for (ServerLevel candidate : server.getAllLevels()) {
                if (candidate.dimension().identifier().equals(dimId)) {
                    level = candidate;
                    break;
                }
            }
        }
        if (level != null) {
            removeOurLight(level, BlockPos.of(packed));
        }
    }

    static void placeFollowLight(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        Long last = player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_POS, Long.MIN_VALUE);
        String lastDim = player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_DIM, "");
        String nowDim = level.dimension().identifier().toString();
        if (last != null && last != Long.MIN_VALUE && (!nowDim.equals(lastDim) || last != pos.asLong())) {
            clearPlayerLight(player);
        }
        if (placeOurLight(level, pos, remainingPlayerLight(player))) {
            player.setAttached(EhmAttachments.EHM_LIGHT_POS, pos.asLong());
            player.setAttached(EhmAttachments.EHM_LIGHT_DIM, nowDim);
        }
    }

    static boolean placeOurLight(ServerLevel level, BlockPos pos, int remainingTicks) {
        BlockState current = level.getBlockState(pos);
        BlockState light = Blocks.LIGHT
                .defaultBlockState()
                .setValue(LightBlock.LEVEL, AbilityRules.lightLevel(remainingTicks));
        if (current.is(Blocks.LIGHT)) {
            if (current.hasProperty(LightBlock.WATERLOGGED) && current.getValue(LightBlock.WATERLOGGED)) {
                light = light.setValue(LightBlock.WATERLOGGED, true);
            }
            if (!current.equals(light)) {
                level.setBlock(pos, light, 3);
            }
            return true;
        }
        if (current.isAir() || current.canBeReplaced()) {
            level.setBlock(pos, light, 3);
            return true;
        }
        if (current.is(Blocks.WATER) && current.getFluidState().isSource()) {
            level.setBlock(pos, light.setValue(LightBlock.WATERLOGGED, true), 3);
            return true;
        }
        return false;
    }

    static void removeOurLight(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(Blocks.LIGHT)) {
            return;
        }
        if (current.hasProperty(LightBlock.WATERLOGGED) && current.getValue(LightBlock.WATERLOGGED)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        } else {
            level.removeBlock(pos, false);
        }
    }

    static void applySlow(LivingEntity target, double power, int durationTicks, ServerLevel level) {
        AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        double amount = AbilityRules.slowAmount(power, target.getMaxHealth());
        if (amount <= 0.0) {
            return;
        }
        speed.addOrUpdateTransientModifier(new AttributeModifier(
                ExtraHardModeMod.ABILITY_SLOW, -amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        target.setAttached(EhmAttachments.EHM_SLOW_UNTIL, level.getGameTime() + durationTicks);
        SLOWED.put(target.getUUID(), level.dimension().identifier());
        SlowWeb.showOn(target, level);
    }

    static void clearSlow(LivingEntity target) {
        AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ExtraHardModeMod.ABILITY_SLOW);
        }
        target.setAttached(EhmAttachments.EHM_SLOW_UNTIL, -1L);
        SLOWED.remove(target.getUUID());
        SlowWeb.clearFrom(target);
    }

    static void tickSlow(ServerLevel level) {
        if (SLOWED.isEmpty()) {
            return;
        }
        Identifier dim = level.dimension().identifier();
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Identifier>> it = SLOWED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Identifier> entry = it.next();
            if (!dim.equals(entry.getValue())) {
                continue;
            }
            Entity entity = level.getEntityInAnyDimension(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.level() != level) {
                if (entity instanceof LivingEntity dead) {
                    clearSlow(dead);
                } else {
                    SlowWeb.discard(entry.getKey(), level);
                    it.remove();
                }
                continue;
            }
            Long until = living.getAttachedOrElse(EhmAttachments.EHM_SLOW_UNTIL, -1L);
            if (until == null || until < 0L || until <= now) {
                clearSlow(living);
                continue;
            }
            SlowWeb.showOn(living, level);
        }
    }

    public static int supplyBonus(ServerPlayer player, String ability) {
        ItemStack held = player.getMainHandItem();
        return AbilityRules.supplyBonus(
                ability,
                heldItemId(held),
                held.getCount(),
                false,
                InventorySearch.largestMainStack(player, Items.QUARTZ),
                InventorySearch.hasOneMain(player, Items.CHARCOAL));
    }

    public static String meLineValue(ServerPlayer player, AbilityRules.AbilitySkill skill) {
        int bonus = supplyBonus(player, skill.id());
        double effective = abilityPower(player, skill.id(), bonus);
        return AbilityRules.powerLabel(skill.skill()) + " (" + AbilityRules.powerLabel(effective) + ")";
    }

    /** Mana level, then used abilities with skill (effective). */
    public static int sendMeReport(ServerPlayer player, Consumer<Component> send) {
        int manaLevel = Achievements.manaLevel(player);
        Component mana = Component.literal(Integer.toString(manaLevel));
        var skills = AbilityRules.playerSkills(usesMap(player));
        if (skills.isEmpty()) {
            send.accept(Component.translatableWithFallback(
                    "tougher.command.me.paragraph_empty",
                    "Mana level: %s. You have not used any mana abilities yet.",
                    mana));
            return 0;
        }
        var list = Component.empty();
        boolean first = true;
        for (AbilityRules.AbilitySkill skill : skills) {
            if (!first) {
                list.append("; ");
            }
            first = false;
            Component name = Component.translatableWithFallback(AbilityRules.nameKey(skill.id()), skill.name());
            if (!AbilityRules.isLearned(learnedSet(player), skill.id())) {
                name = Component.empty().append(name).append(AbilityRules.UNLEARNED_MARK);
            }
            list.append(Component.translatableWithFallback(
                    "tougher.command.me.line",
                    "%s: %s",
                    name,
                    Component.literal(meLineValue(player, skill))));
        }
        send.accept(Component.translatableWithFallback(
                "tougher.command.me.paragraph",
                "Mana level: %s. Your mana abilities: %s",
                mana,
                list));
        return skills.size();
    }

    static boolean hasItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        return InventorySearch.has(player, item);
    }

    static boolean hasItem(ServerPlayer player, java.util.function.Predicate<ItemStack> match) {
        return InventorySearch.has(player, match);
    }

    static boolean consumeOne(ServerPlayer player, java.util.function.Predicate<ItemStack> match) {
        return InventorySearch.consumeOne(player, match);
    }

    public static boolean isPowerMining(ServerPlayer player) {
        return remainingPowerMine(player) > 0;
    }

    static int remainingPowerMine(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_POWER_MINE_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    public static void sendPowerMine(ServerPlayer player) {
        int remaining = remainingPowerMine(player);
        int max = Math.max(remaining, player.getAttachedOrElse(EhmAttachments.EHM_POWER_MINE_BAR_MAX, 0));
        ServerPlayNetworking.send(player, new ClientboundPowerMinePayload(remaining, max));
    }

    public static void sendPowerMineInactive(ServerPlayer player) {
        ServerPlayNetworking.send(player, ClientboundPowerMinePayload.INACTIVE);
    }

    static void onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            if (isFlying(player)) {
                stopFlight(player, false);
            }
            if (isPowerMining(player)) {
                stopPowerMine(player, false);
            }
            stopPlayerLight(player);
            player.setAttached(EhmAttachments.EHM_IRON_HEART_LOCK, 0);
            clearIronHeartBuff(player, false);
            clearMasterBuilder(player, false);
            clearDiamondSkin(player, false);
            clearEmpower(player, false);
            clearBattery(player, false);
            endOutgoingLapisShield(player, LAPIS_SILENT);
            sendAbilityDurations(player);
        }
        endLapisShieldOn(entity, LAPIS_SILENT);
        if (SLOWED.containsKey(entity.getUUID())) {
            clearSlow(entity);
        }
    }

    static void tickFlight(ServerPlayer player) {
        if (!isFlying(player)) {
            return;
        }
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive(player.level(), ID)) {
            stopFlight(player, false);
            return;
        }
        if (AbilityRules.flightStops(player.isShiftKeyDown(), player.onGround())) {
            stopFlight(player, false);
            return;
        }
        Abilities kept = player.getAbilities();
        if (!kept.mayfly || !kept.flying) {
            Float speed = player.getAttached(EhmAttachments.EHM_FLIGHT_SPEED);
            applyFlightAbilities(player, speed == null ? AbilityRules.flightSpeed(1) : speed);
        }
        int remaining = remainingFlight(player) - 1;
        if (remaining > 0) {
            player.setAttached(EhmAttachments.EHM_FLIGHT_REMAINING, remaining);
            if (remaining % 5 == 0) {
                sendFlight(player);
            }
            return;
        }
        if (Achievements.currentMana(player) >= AbilityRules.MANA_COST) {
            int bonus = renewCatalystBonus(player, Items.FEATHER);
            double power = abilityPower(player, AbilityRules.FLIGHT, bonus);
            consumeRenewCatalyst(player, Items.FEATHER);
            consumeManaAndHeld(player, null);
            recordAbilityUse(player, AbilityRules.FLIGHT);
            int extra = AbilityRules.flightDurationTicks(power);
            float speed = AbilityRules.flightSpeed(power);
            player.setAttached(EhmAttachments.EHM_FLIGHT_REMAINING, extra);
            player.setAttached(EhmAttachments.EHM_FLIGHT_BAR_MAX, extra);
            player.setAttached(EhmAttachments.EHM_FLIGHT_SPEED, speed);
            applyFlightAbilities(player, speed);
            sendFlight(player);
            sendAbilityDurations(player);
            castEffect(player, SoundEvents.PHANTOM_FLAP, ParticleTypes.CLOUD, ParticleTypes.END_ROD);
            tellAbilityUse(player, "tougher.ability.flight", "Flight", power);
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.flight_fall", "You are out of mana and fall."));
        stopFlight(player, false);
    }

    static void startFlight(ServerPlayer player, double power) {
        int duration = AbilityRules.flightDurationTicks(power);
        player.setAttached(EhmAttachments.EHM_FLIGHT_ACTIVE, Boolean.TRUE);
        player.setAttached(EhmAttachments.EHM_FLIGHT_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_FLIGHT_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_FLIGHT_SAVED_SPEED, player.getAbilities().getFlyingSpeed());
        player.setAttached(EhmAttachments.EHM_FLIGHT_SPEED, AbilityRules.flightSpeed(power));
        applyFlightAbilities(player, AbilityRules.flightSpeed(power));
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, AbilityRules.flightLaunchY(motion.y), motion.z);
        player.setOnGround(false);
        ((dev.extrahardmode.mixin.EntityHurtAccess) player).tougher$markHurt();
        sendFlight(player);
        sendAbilityDurations(player);
    }

    static void stopFlight(ServerPlayer player, boolean silent) {
        if (!isFlying(player) && remainingFlight(player) <= 0) {
            sendFlightInactive(player);
            sendAbilityDurations(player);
            return;
        }
        player.setAttached(EhmAttachments.EHM_FLIGHT_ACTIVE, Boolean.FALSE);
        player.setAttached(EhmAttachments.EHM_FLIGHT_REMAINING, 0);
        player.setAttached(EhmAttachments.EHM_FLIGHT_BAR_MAX, 0);
        Abilities abilities = player.getAbilities();
        abilities.flying = false;
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.mayfly = false;
        }
        Float saved = player.getAttached(EhmAttachments.EHM_FLIGHT_SAVED_SPEED);
        abilities.setFlyingSpeed(saved == null ? 0.05F : saved);
        player.onUpdateAbilities();
        putLong(
                player,
                EhmAttachments.EHM_ABILITY_LAST_USE,
                AbilityRules.FLIGHT,
                player.level().getServer().overworld().getGameTime());
        sendFlightInactive(player);
        sendAbilityDurations(player);
    }

    static void applyFlightAbilities(ServerPlayer player, float speed) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.setFlyingSpeed(Math.max(0.001F, speed));
        player.onUpdateAbilities();
    }

    static void tickAbilityHint(ServerPlayer player) {
        if (Achievements.skipPlayer(player) || !WorldGate.isModuleActive((ServerLevel) player.level(), ID)) {
            return;
        }
        if (!AbilityRules.showsAbilityHints(Achievements.manaLevel(player))) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        String ability = abilityForHeld(held);
        if (ability == null) {
            return;
        }
        if (held.is(Items.FEATHER) && isFlying(player)) {
            return;
        }
        if (held.is(Items.IRON_INGOT) && remainingIronHeartBuff(player) > 0) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_iron_heart_off",
                    "Right click to cancel %s...",
                    abilityName(ability)));
            return;
        }
        if (held.is(Items.BRICK) && masterBuilderActive(player)) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_iron_heart_off",
                    "Right click to cancel %s...",
                    abilityName(ability)));
            return;
        }
        if (held.is(Items.DIAMOND) && diamondSkinActive(player)) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_iron_heart_off",
                    "Right click to cancel %s...",
                    abilityName(ability)));
            return;
        }
        if (held.is(Items.REDSTONE) && empowerActive(player)) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_iron_heart_off",
                    "Right click to cancel %s...",
                    abilityName(ability)));
            return;
        }
        if (held.is(Items.QUARTZ_BLOCK) && batteryActive(player)) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_iron_heart_off",
                    "Right click to cancel %s...",
                    abilityName(ability)));
            return;
        }
        if (held.is(Items.LAPIS_LAZULI) && outgoingLapisTicks(player) > 0) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_iron_heart_off",
                    "Right click to cancel %s...",
                    abilityName(ability)));
            return;
        }
        if (!AbilityRules.hasManaToUse(ability, Achievements.currentMana(player))) {
            return;
        }
        int hoeBonus = 0;
        if (held.is(ItemTags.HOES)) {
            hoeBonus += AbilityRules.growHoeBonus(heldItemId(held));
        } else if (held.is(ItemTags.PICKAXES)) {
            if (isPowerMining(player)) {
                return;
            }
            hoeBonus += AbilityRules.pickaxeBonus(heldItemId(held));
        } else if (held.is(Items.COMPASS)) {
            hoeBonus += AbilityRules.detectOreQuartzBonus(
                    InventorySearch.largestMainStack(player, Items.QUARTZ));
        } else if (held.is(Items.ARROW)) {
            hoeBonus += AbilityRules.catalystBonus(held.getCount());
            hoeBonus += AbilityRules.charcoalArrowBonus(InventorySearch.hasOneMain(player, Items.CHARCOAL));
        } else if (held.is(Items.STRING)
                || held.is(Items.IRON_INGOT)
                || held.is(Items.BRICK)
                || held.is(Items.DIAMOND)
                || held.is(Items.REDSTONE)
                || held.is(Items.QUARTZ_BLOCK)
                || held.is(Items.LAPIS_LAZULI)
                || held.is(Items.PAPER)
                || held.is(Items.FEATHER)
                || held.is(Items.SPIDER_EYE)
                || held.is(Items.BOOK)) {
            hoeBonus += AbilityRules.catalystBonus(held.getCount());
        } else if (isLightCoal(held)) {
            hoeBonus += AbilityRules.catalystBonus(held.getCount());
        }
        Component abilityName = abilityName(ability);
        if (held.is(ItemTags.HOES)) {
            if (hoeBonus > 0) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        "tougher.message.ability_hint_air_bonus",
                        "Right-click a plant to activate %s... (+%s)",
                        abilityName,
                        Component.literal(Integer.toString(hoeBonus))));
            } else {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        "tougher.message.ability_hint_air",
                        "Right-click a plant to activate %s...",
                        abilityName));
            }
            return;
        }
        if (isLightCoal(held) && remainingPlayerLight(player) > 0) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_torch_off",
                    "Right click to snuff %s...",
                    abilityName));
            return;
        }
        if (held.is(ItemTags.PICKAXES)
                || held.is(Items.COMPASS)
                || held.is(Items.PAPER)
                || held.is(Items.IRON_INGOT)
                || held.is(Items.FEATHER)
                || held.is(Items.ARROW)
                || held.is(Items.STRING)
                || held.is(Items.GOLDEN_SWORD)
                || held.is(Items.BRICK)
                || held.is(Items.DIAMOND)
                || held.is(Items.REDSTONE)
                || held.is(Items.QUARTZ_BLOCK)
                || held.is(Items.LAPIS_LAZULI)
                || held.is(Items.BOOK)
                || tomeXp(held) > 0
                || isLightCoal(held)) {
            if (hoeBonus > 0) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        "tougher.message.ability_hint_right_bonus",
                        "Right click to activate %s... (+%s)",
                        abilityName,
                        Component.literal(Integer.toString(hoeBonus))));
            } else {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        "tougher.message.ability_hint_right",
                        "Right click to activate %s...",
                        abilityName));
            }
            return;
        }
        if (hoeBonus > 0) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    "tougher.message.ability_hint_bonus",
                    "Left click to activate %s... (+%s)",
                    abilityName,
                    Component.literal(Integer.toString(hoeBonus))));
            return;
        }
        player.sendOverlayMessage(Component.translatableWithFallback(
                "tougher.message.ability_hint", "Left click to activate %s...", abilityName));
    }

    static String heldItemId(ItemStack held) {
        if (held == null || held.isEmpty()) {
            return "";
        }
        return held.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
    }

    static void castEffect(ServerPlayer player, SoundEvent sound, ParticleOptions primary, ParticleOptions secondary) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, 1.0F);
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.6;
        double z = player.getZ();
        level.sendParticles(primary, x, y, z, 8, 0.35, 0.45, 0.35, 0.02);
        if (secondary != null) {
            level.sendParticles(secondary, x, y, z, 6, 0.3, 0.4, 0.3, 0.0);
        }
    }

    static void tellAbilityUse(ServerPlayer player, String nameKey, String fallback, double power) {
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.ability_use",
                "You are using the %s ability at level %s!",
                Component.translatableWithFallback(nameKey, fallback),
                Component.literal(AbilityRules.powerLabel(power))));
    }

    static void consumeManaAndHeld(ServerPlayer player, net.minecraft.world.item.Item consumed) {
        tryConsumeHeld(player, consumed);
        player.setAttached(
                EhmAttachments.EHM_MANA_CURRENT,
                Math.max(0.0, Achievements.currentMana(player) - AbilityRules.MANA_COST));
        Achievements.sendMana(player);
    }

    /**
     * Fresh cast. One held catalyst is spent. Two or more spend two, which is what
     * grants the +2. Tools and weapons are not passed in.
     */
    static void tryConsumeHeld(ServerPlayer player, net.minecraft.world.item.Item consumed) {
        if (consumed == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(consumed)) {
            return;
        }
        int take = AbilityRules.catalystConsumeCount(held.getCount());
        if (take > 0) {
            held.shrink(take);
        }
    }

    /** +2 when a renew will spend one catalyst that is still in the main hand. */
    static int renewCatalystBonus(ServerPlayer player, net.minecraft.world.item.Item catalyst) {
        if (player == null || catalyst == null) {
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(catalyst)) {
            return 0;
        }
        return AbilityRules.renewCatalystBonus(held.getCount());
    }

    /** Spends one held catalyst on a renew. Does nothing when that item is not in hand. */
    static void consumeRenewCatalyst(ServerPlayer player, net.minecraft.world.item.Item catalyst) {
        if (player == null || catalyst == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(catalyst)) {
            return;
        }
        int take = AbilityRules.renewConsumeCount(held.getCount());
        if (take > 0) {
            held.shrink(take);
        }
    }

    public static void sendFlight(ServerPlayer player) {
        int remaining = remainingFlight(player);
        int max = Math.max(remaining, player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_BAR_MAX, 0));
        ServerPlayNetworking.send(player, new ClientboundFlightPayload(remaining, max));
    }

    public static void sendFlightInactive(ServerPlayer player) {
        ServerPlayNetworking.send(player, ClientboundFlightPayload.INACTIVE);
    }

    static void maybeSendAbilityDurations(ServerPlayer player) {
        if (shouldPushDurations(player)) {
            sendAbilityDurations(player);
        }
    }

    static boolean shouldPushDurations(ServerPlayer player) {
        int flight = remainingFlight(player);
        int mine = remainingPowerMine(player);
        int light = remainingPlayerLight(player);
        int heart = remainingIronHeartBuff(player);
        int builder = remainingMasterBuilder(player);
        int skin = remainingDiamondSkin(player);
        int empower = remainingEmpower(player);
        int battery = remainingBattery(player);
        int lapis = lapisHudTicks(player);
        int warn = AbilityDurationRules.WARN_TICKS;
        return inDurationWindow(flight, warn)
                || inDurationWindow(mine, warn)
                || inDurationWindow(light, warn)
                || inDurationWindow(heart, warn)
                || inDurationWindow(builder, warn)
                || inDurationWindow(skin, warn)
                || inDurationWindow(empower, warn)
                || inDurationWindow(battery, warn)
                || inDurationWindow(lapis, warn);
    }

    static boolean inDurationWindow(int remaining, int warn) {
        return remaining > 0 && (remaining <= warn || remaining % 5 == 0);
    }

    public static void sendAbilityDurations(ServerPlayer player) {
        List<ClientboundAbilityDurationsPayload.Entry> effects = new ArrayList<>();
        addDuration(
                effects,
                AbilityRules.FLIGHT,
                remainingFlight(player),
                player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_BAR_MAX, 0),
                AbilityRules.autoRenews(AbilityRules.FLIGHT)
                        && AbilityRules.hasManaToUse(AbilityRules.FLIGHT, Achievements.currentMana(player)));
        addDuration(
                effects,
                AbilityRules.POWER_MINE,
                remainingPowerMine(player),
                player.getAttachedOrElse(EhmAttachments.EHM_POWER_MINE_BAR_MAX, 0),
                false);
        addDuration(
                effects,
                AbilityRules.LIGHT,
                remainingPlayerLight(player),
                player.getAttachedOrElse(EhmAttachments.EHM_LIGHT_BAR_MAX, 0),
                AbilityRules.autoRenews(AbilityRules.LIGHT)
                        && AbilityRules.hasManaToUse(AbilityRules.LIGHT, Achievements.currentMana(player)));
        int heart = remainingIronHeartBuff(player);
        if (heart > 0) {
            ServerPlayer caster = ironHeartCaster(player);
            boolean auto = AbilityRules.autoRenews(AbilityRules.IRON_HEART)
                    && AbilityRules.hasManaToUse(AbilityRules.IRON_HEART, Achievements.currentMana(caster));
            addDuration(
                    effects,
                    AbilityRules.IRON_HEART,
                    heart,
                    player.getAttachedOrElse(EhmAttachments.EHM_IRON_HEART_BAR_MAX, 0),
                    auto);
        }
        addDuration(
                effects,
                AbilityRules.MASTER_BUILDER,
                remainingMasterBuilder(player),
                player.getAttachedOrElse(EhmAttachments.EHM_MASTER_BUILDER_BAR_MAX, 0),
                AbilityRules.autoRenews(AbilityRules.MASTER_BUILDER)
                        && AbilityRules.hasManaToUse(
                                AbilityRules.MASTER_BUILDER, Achievements.currentMana(player)));
        addDuration(
                effects,
                AbilityRules.DIAMOND_SKIN,
                remainingDiamondSkin(player),
                player.getAttachedOrElse(EhmAttachments.EHM_DIAMOND_SKIN_BAR_MAX, 0),
                AbilityRules.autoRenews(AbilityRules.DIAMOND_SKIN)
                        && AbilityRules.hasManaToUse(
                                AbilityRules.DIAMOND_SKIN, Achievements.currentMana(player)));
        addDuration(
                effects,
                AbilityRules.EMPOWER,
                remainingEmpower(player),
                player.getAttachedOrElse(EhmAttachments.EHM_EMPOWER_BAR_MAX, 0),
                AbilityRules.autoRenews(AbilityRules.EMPOWER)
                        && AbilityRules.hasManaToUse(AbilityRules.EMPOWER, Achievements.currentMana(player)));
        addDuration(
                effects,
                AbilityRules.BATTERY,
                remainingBattery(player),
                player.getAttachedOrElse(EhmAttachments.EHM_BATTERY_BAR_MAX, 0),
                AbilityRules.autoRenews(AbilityRules.BATTERY)
                        && !batteryReachedLevel(player)
                        && player.getMainHandItem().is(Items.QUARTZ_BLOCK));
        addDuration(
                effects,
                AbilityRules.LAPIS_SHIELD,
                lapisHudTicks(player),
                player.getAttachedOrElse(EhmAttachments.EHM_LAPIS_SHIELD_BAR_MAX, 0),
                false);
        ServerPlayNetworking.send(
                player,
                effects.isEmpty()
                        ? ClientboundAbilityDurationsPayload.INACTIVE
                        : new ClientboundAbilityDurationsPayload(effects));
    }

    static void addDuration(
            List<ClientboundAbilityDurationsPayload.Entry> effects,
            String ability,
            int remaining,
            int max,
            boolean autoContinue) {
        if (remaining <= 0 || max <= 0) {
            return;
        }
        effects.add(new ClientboundAbilityDurationsPayload.Entry(ability, remaining, max, autoContinue));
    }

    public static void onJoin(ServerPlayer player) {
        if (isFlying(player) && remainingFlight(player) > 0) {
            Float speed = player.getAttached(EhmAttachments.EHM_FLIGHT_SPEED);
            applyFlightAbilities(player, speed == null ? 0.05F : speed);
            sendFlight(player);
        } else if (isFlying(player)) {
            stopFlight(player, true);
        } else {
            sendFlightInactive(player);
        }
        if (isPowerMining(player)) {
            sendPowerMine(player);
        } else {
            sendPowerMineInactive(player);
        }
        if (remainingPlayerLight(player) > 0) {
            placeFollowLight(player);
        }
        if (remainingIronHeartBuff(player) > 0) {
            Float amount = player.getAttachedOrElse(EhmAttachments.EHM_IRON_HEART_AMOUNT, 0.0F);
            if (amount != null && amount > 0.0F) {
                AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
                if (max != null) {
                    max.addOrUpdateTransientModifier(new AttributeModifier(
                            ExtraHardModeMod.IRON_HEART, amount, AttributeModifier.Operation.ADD_VALUE));
                }
            }
        }
        sendAbilityDurations(player);
        syncStrongCastHud(player, true);
    }

    private static final Map<UUID, String> STRONG_CAST_SENT = new ConcurrentHashMap<>();

    public static void clearStrongCastHud(ServerPlayer player) {
        if (player != null) {
            STRONG_CAST_SENT.remove(player.getUUID());
        }
    }

    static void syncStrongCastHud(ServerPlayer player, boolean force) {
        List<AbilityReadyRules.HudIcon> casts = List.of();
        if (!Achievements.skipPlayer(player) && WorldGate.isModuleActive(player.level(), ID)) {
            long now = 0L;
            if (player.level().getServer() != null) {
                now = player.level().getServer().overworld().getGameTime();
            }
            Map<String, Long> lastUse = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LAST_USE, Map.of());
            casts = AbilityReadyRules.hudIcons(
                    Achievements.manaLevel(player),
                    usesMap(player),
                    learnedSet(player),
                    0, // manaLevel already includes ready gold
                    supplyStacks(player),
                    InventorySearch.hasOneMain(player, Items.HONEY_BOTTLE),
                    now,
                    lastUse == null ? Map.of() : lastUse,
                    holdingBook(player),
                    abilityForHeld(player.getMainHandItem()),
                    activeAbilities(player),
                    empowerActive(player) ? AbilityRules.empowerCastBonus(empowerLevel(player)) : 0.0);
        }
        String key = hudIconKey(casts);
        if (!force && key.equals(STRONG_CAST_SENT.get(player.getUUID()))) {
            return;
        }
        STRONG_CAST_SENT.put(player.getUUID(), key);
        List<ClientboundAbilityReadyPayload.Entry> entries = new ArrayList<>();
        for (AbilityReadyRules.HudIcon icon : casts) {
            int size = AbilityReadyRules.hudIconPixels(icon.level(), true);
            int level = AbilityReadyRules.hudLevelNumber(icon.level());
            if (size <= 0) {
                continue;
            }
            entries.add(new ClientboundAbilityReadyPayload.Entry(
                    icon.ability(), size, level, icon.learned(), icon.cooldownSeconds(), (float) icon.level()));
        }
        ServerPlayNetworking.send(
                player,
                entries.isEmpty() ? ClientboundAbilityReadyPayload.EMPTY : new ClientboundAbilityReadyPayload(entries));
    }

    static boolean holdingBook(ServerPlayer player) {
        return AbilityReadyRules.isBook(heldItemId(player.getMainHandItem()))
                || AbilityReadyRules.isBook(heldItemId(player.getOffhandItem()));
    }

    static java.util.Set<String> activeAbilities(ServerPlayer player) {
        java.util.Set<String> active = new java.util.HashSet<>();
        if (remainingFlight(player) > 0) {
            active.add(AbilityRules.FLIGHT);
        }
        if (remainingPowerMine(player) > 0) {
            active.add(AbilityRules.POWER_MINE);
        }
        if (remainingPlayerLight(player) > 0) {
            active.add(AbilityRules.LIGHT);
        }
        if (remainingIronHeartBuff(player) > 0) {
            active.add(AbilityRules.IRON_HEART);
        }
        if (remainingMasterBuilder(player) > 0) {
            active.add(AbilityRules.MASTER_BUILDER);
        }
        if (remainingDiamondSkin(player) > 0) {
            active.add(AbilityRules.DIAMOND_SKIN);
        }
        if (remainingEmpower(player) > 0) {
            active.add(AbilityRules.EMPOWER);
        }
        if (remainingBattery(player) > 0) {
            active.add(AbilityRules.BATTERY);
        }
        if (lapisHudTicks(player) > 0) {
            active.add(AbilityRules.LAPIS_SHIELD);
        }
        return active;
    }

    static String hudIconKey(List<AbilityReadyRules.HudIcon> icons) {
        StringBuilder key = new StringBuilder();
        for (AbilityReadyRules.HudIcon icon : icons) {
            key.append(icon.ability())
                    .append(':')
                    .append(AbilityReadyRules.hudIconPixels(icon.level(), true))
                    .append(':')
                    .append(AbilityReadyRules.hudLevelNumber(icon.level()))
                    .append(':')
                    .append(Math.round(icon.level() * 1000.0))
                    .append(':')
                    .append(icon.learned())
                    .append(':')
                    .append(icon.cooldownSeconds())
                    .append(';');
        }
        return key.toString();
    }

    static List<AbilityReadyRules.SupplyStack> supplyStacks(ServerPlayer player) {
        List<AbilityReadyRules.SupplyStack> stacks = new ArrayList<>();
        var inventory = player.getInventory();
        int main = Math.min(AbilityRules.MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            collectSupply(inventory.getItem(slot), stacks, false, slot < main);
        }
        return stacks;
    }

    /**
     * Nether quartz and charcoal count only as their own stacks in the hotbar or backpack.
     * Either one in a bundle, worn as armor, or held in the offhand is left out.
     * Other supplies still count from the whole inventory, including bundles.
     */
    static void collectSupply(
            ItemStack stack,
            List<AbilityReadyRules.SupplyStack> out,
            boolean insideBundle,
            boolean mainInventory) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String id = heldItemId(stack);
        boolean mainOnly = AbilityReadyRules.QUARTZ_ID.equals(id) || AbilityRules.CHARCOAL_ID.equals(id);
        if (!id.isEmpty()
                && AbilityReadyRules.countsForAbilitySupply(id, insideBundle)
                && (!mainOnly || (mainInventory && !insideBundle))) {
            out.add(new AbilityReadyRules.SupplyStack(id, stack.getCount()));
        }
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (ItemStackTemplate template : contents.items()) {
            collectSupply(template.create(), out, true, mainInventory);
        }
    }

    static boolean isFlying(ServerPlayer player) {
        return Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_ACTIVE, Boolean.FALSE));
    }

    static int remainingFlight(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    /**
     * A plain book at experience level 7 or higher. One book makes one tome. Two or more
     * add +2, and each further book makes another tome, up to four tomes from five books.
     * The saved experience is split across those tomes. Above level 30 only the excess is
     * banked and the player returns to level 30. At level 30 or below, all experience is
     * banked and the player returns to 0.
     */
    static boolean tryWriteTome(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(Items.BOOK) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        if (!TomeRules.canWrite(player.experienceLevel)) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.tome_level", "You must be at least experience level 7."));
            markHandled(player);
            return true;
        }
        int heldCount = held.getCount();
        int bonus = AbilityRules.catalystBonus(heldCount);
        double power = abilityPower(player, AbilityRules.WRITE_TOME, bonus);
        int visible = TomeRules.totalXp(player.experienceLevel, player.experienceProgress);
        int keptLevel = TomeRules.levelAfterWrite(player.experienceLevel);
        int saved = TomeRules.share(TomeRules.bankedXp(player.experienceLevel, visible), power);
        int[] parts = TomeRules.split(saved, TomeRules.tomeCount(heldCount));
        if (saved <= 0 || parts.length == 0) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.tome_empty", "That would save no experience."));
            markHandled(player);
            return true;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.WRITE_TOME, "tougher.ability.write_tome", "Write Tome of the Wise")) {
            return true;
        }
        String author = player.getScoreboardName();
        held.shrink(TomeRules.booksConsumed(heldCount));
        spend(player, null, AbilityRules.WRITE_TOME);
        int written = 0;
        for (int part : parts) {
            if (part <= 0) {
                continue;
            }
            giveTome(player, writtenTome(author, part));
            written++;
        }
        dropToKeptLevel(player, keptLevel);
        castTome(player);
        tellAbilityUse(player, "tougher.ability.write_tome", "Write Tome of the Wise", power);
        if (written <= 1) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.tome_write",
                    "The tome holds %s experience.",
                    Component.literal(Integer.toString(saved))));
        } else {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.tome_write_many",
                    "The tomes hold %s experience.",
                    Component.literal(TomeRules.amountList(parts))));
        }
        return true;
    }

    /**
     * A written book that stores experience. Costs 1 mana and that book, and grants
     * 80% of the stored experience, plus 2% per ability level. Sneak opens the book instead.
     */
    static boolean tryReadTome(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        int saved = tomeXp(held);
        if (saved <= 0 || Achievements.skipPlayer(player) || player.isSecondaryUseActive()) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        double power = abilityPower(player, AbilityRules.READ_TOME, 0);
        int grant = TomeRules.share(saved, power);
        if (grant <= 0) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.tome_small",
                    "That tome's experience is too small to read at this level."));
            markHandled(player);
            return true;
        }
        if (blockedByCooldownOrMana(
                player, AbilityRules.READ_TOME, "tougher.ability.read_tome", "Read Tome of the Wise")) {
            return true;
        }
        held.shrink(1);
        spend(player, null, AbilityRules.READ_TOME);
        player.giveExperiencePoints(grant);
        castTome(player);
        tellAbilityUse(player, "tougher.ability.read_tome", "Read Tome of the Wise", power);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.tome_read",
                "The tome grants %s experience.",
                Component.literal(Integer.toString(grant))));
        return true;
    }

    static ItemStack writtenTome(String author, int xp) {
        String who = TomeRules.authorName(author);
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        Component page = Component.literal(TomeRules.note(who, xp));
        stack.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(TomeRules.TITLE),
                        who,
                        0,
                        java.util.List.of(Filterable.passThrough(page)),
                        true));
        CompoundTag tag = new CompoundTag();
        tag.putInt(TomeRules.XP_KEY, Math.max(0, xp));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        stack.set(DataComponents.LORE, new ItemLore(java.util.List.of(page)));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(TomeRules.TITLE));
        return stack;
    }

    static int tomeXp(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.WRITTEN_BOOK)) {
            return 0;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        return Math.max(0, data.copyTag().getIntOr(TomeRules.XP_KEY, 0));
    }

    /** Leaves the player at exactly {@code keptLevel} with an empty bar. */
    static void dropToKeptLevel(ServerPlayer player, int keptLevel) {
        int target = Math.max(0, keptLevel);
        int kept = TomeRules.xpToReachLevel(target);
        int visible = TomeRules.totalXp(player.experienceLevel, player.experienceProgress);
        int excess = Math.max(0, visible - kept);
        if (excess > 0) {
            player.giveExperiencePoints(-excess);
        }
        int after = TomeRules.totalXp(player.experienceLevel, player.experienceProgress);
        if (player.experienceLevel != target || player.experienceProgress != 0.0F || after != kept) {
            player.totalExperience = Math.max(0, player.totalExperience - (after - kept));
            player.setExperienceLevels(target);
            player.setExperiencePoints(0);
        }
    }

    static void giveTome(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            ItemEntity dropped = player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
            }
        }
    }

    static void castTome(ServerPlayer player) {
        castEffect(
                player,
                SoundEvents.ENCHANTMENT_TABLE_USE,
                ParticleTypes.ENCHANT,
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.55F, 0.35F, 0.85F));
    }

    static String abilityForHeld(ItemStack held) {
        if (held == null || held.isEmpty()) {
            return null;
        }
        if (held.is(Items.FEATHER)) {
            return AbilityRules.FLIGHT;
        }
        if (held.is(Items.PAPER)) {
            return AbilityRules.HEAL;
        }
        if (held.is(Items.IRON_INGOT)) {
            return AbilityRules.IRON_HEART;
        }
        if (held.is(Items.ARROW)) {
            return AbilityRules.MAGIC_ARROW;
        }
        if (held.is(ItemTags.HOES)) {
            return AbilityRules.GROW;
        }
        if (held.is(ItemTags.PICKAXES)) {
            return AbilityRules.POWER_MINE;
        }
        if (held.is(Items.COMPASS)) {
            return AbilityRules.DETECT_ORE;
        }
        if (held.is(Items.STRING)) {
            return AbilityRules.SLOW;
        }
        if (isLightCoal(held)) {
            return AbilityRules.LIGHT;
        }
        if (held.is(Items.SPIDER_EYE)) {
            return AbilityRules.SENSE_EVIL;
        }
        if (held.is(Items.GOLDEN_SWORD)) {
            return AbilityRules.SMITE_EVIL;
        }
        if (held.is(Items.BRICK)) {
            return AbilityRules.MASTER_BUILDER;
        }
        if (held.is(Items.DIAMOND)) {
            return AbilityRules.DIAMOND_SKIN;
        }
        if (held.is(Items.REDSTONE)) {
            return AbilityRules.EMPOWER;
        }
        if (held.is(Items.QUARTZ_BLOCK)) {
            return AbilityRules.BATTERY;
        }
        if (held.is(Items.LAPIS_LAZULI)) {
            return AbilityRules.LAPIS_SHIELD;
        }
        if (held.is(Items.BOOK)) {
            return AbilityRules.WRITE_TOME;
        }
        if (tomeXp(held) > 0) {
            return AbilityRules.READ_TOME;
        }
        return null;
    }

    static Component abilityName(String ability) {
        return Component.translatableWithFallback(AbilityRules.nameKey(ability), AbilityRules.nameFallback(ability));
    }

    static boolean blockedByCooldownOrMana(ServerPlayer player, String ability, String nameKey, String fallback) {
        return blockedByCooldownOrMana(player, ability, nameKey, fallback, false);
    }

    static boolean blockedByCooldownOrMana(
            ServerPlayer player, String ability, String nameKey, String fallback, boolean honeyHeal) {
        tickGoldWear(player);
        ServerLevel overworld = player.level().getServer().overworld();
        long now = overworld.getGameTime();
        int remaining = AbilityRules.remainingCooldownTicks(
                now,
                lastUse(player, ability),
                AbilityRules.cooldownTicks(ability, Achievements.manaLevel(player), uses(player, ability)));
        if (remaining > 0) {
            Component name = Component.translatableWithFallback(nameKey, fallback);
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.ability_cooldown",
                    "%s is ready in %s.",
                    name,
                    AbilityRules.cooldownLabel(remaining)));
            markHandled(player);
            return true;
        }
        boolean enoughMana = AbilityRules.HEAL.equals(ability)
                ? AbilityRules.hasManaToHeal(Achievements.currentMana(player), honeyHeal)
                : AbilityRules.hasManaToUse(ability, Achievements.currentMana(player));
        if (!enoughMana) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.ability_no_mana", "You need mana to do that."));
            markHandled(player);
            return true;
        }
        return false;
    }

    static void spend(ServerPlayer player, net.minecraft.world.item.Item consumed, String ability) {
        tryConsumeHeld(player, consumed);
        double current = Math.max(0.0, Achievements.currentMana(player) - AbilityRules.manaCost(ability));
        player.setAttached(EhmAttachments.EHM_MANA_CURRENT, current);
        Achievements.sendMana(player);
        recordAbilityUse(player, ability);
        putLong(
                player,
                EhmAttachments.EHM_ABILITY_LAST_USE,
                ability,
                player.level().getServer().overworld().getGameTime());
        markHandled(player);
    }

    static void hurlArrowVolley(
            ServerPlayer player, double power, double range, LivingEntity primary, boolean burn) {
        int burnSeconds = burn ? AbilityRules.projectileBurnSeconds(power) : 0;
        hurlMagicArrow(player, power, primary, player.getLookAngle(), burnSeconds);
        for (LivingEntity extra : extraFireTargets(player, range, primary, AbilityRules.extraProjectileCount(power))) {
            hurlMagicArrow(player, power, extra, aimAt(player, extra), burnSeconds);
        }
    }

    static void hurlMagicArrow(
            ServerPlayer player, double power, LivingEntity target, Vec3 direction, int burnSeconds) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 dir = direction == null || direction.lengthSqr() < 1.0e-6 ? player.getLookAngle() : direction.normalize();
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
        arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        arrow.setDeltaMovement(dir.scale(AbilityRules.FIRE_BOLT_SPEED));
        arrow.setNoGravity(true);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        tagSeeker(arrow, power, target, burnSeconds);
        level.addFreshEntity(arrow);
    }

    static Vec3 aimAt(ServerPlayer player, LivingEntity target) {
        Vec3 aim = target.getEyePosition().subtract(player.getEyePosition());
        return aim.lengthSqr() < 1.0e-6 ? player.getLookAngle() : aim;
    }

    /**
     * Other living creatures within range, nearest the aim first.
     * Fewer creatures means fewer extra shots. The primary target is skipped.
     */
    static List<LivingEntity> extraFireTargets(ServerPlayer caster, double range, LivingEntity primary, int count) {
        if (count <= 0 || range <= 0.0) {
            return List.of();
        }
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 eye = caster.getEyePosition();
        AABB box = caster.getBoundingBox().inflate(range);
        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                living -> isFireable(caster, living)
                        && living != primary
                        && eye.distanceTo(living.getEyePosition()) <= range));
        if (found.isEmpty()) {
            return List.of();
        }
        Vec3 look = caster.getLookAngle();
        found.sort((a, b) -> {
            int aim = Double.compare(aimScore(eye, look, a), aimScore(eye, look, b));
            if (aim != 0) {
                return aim;
            }
            return Double.compare(eye.distanceTo(a.getEyePosition()), eye.distanceTo(b.getEyePosition()));
        });
        int n = Math.min(count, found.size());
        return new ArrayList<>(found.subList(0, n));
    }

    /** Perpendicular distance from the look ray. Creatures behind the caster sort last. */
    static double aimScore(Vec3 start, Vec3 look, LivingEntity target) {
        Vec3 eye = target.getEyePosition();
        double dx = eye.x - start.x;
        double dy = eye.y - start.y;
        double dz = eye.z - start.z;
        double lookLen = look.length();
        if (lookLen < 1.0e-6) {
            return dx * dx + dy * dy + dz * dz;
        }
        double lx = look.x / lookLen;
        double ly = look.y / lookLen;
        double lz = look.z / lookLen;
        double t = dx * lx + dy * ly + dz * lz;
        double offx = dx - lx * t;
        double offy = dy - ly * t;
        double offz = dz - lz * t;
        double off = offx * offx + offy * offy + offz * offz;
        return t < 0.0 ? off + 1_000_000.0 : off;
    }

    static void tagSeeker(Entity bolt, double power, LivingEntity target, int burnSeconds) {
        bolt.setAttached(EhmAttachments.EHM_FIREBOLT_POWER, AbilityRules.projectileDamage(power));
        bolt.setAttached(EhmAttachments.EHM_FIREBOLT_BURN, Math.max(0, burnSeconds));
        if (target != null) {
            bolt.setAttached(EhmAttachments.EHM_FIREBOLT_TARGET, target.getUUID());
        }
    }

    public static void steerSeeker(Entity bolt) {
        Integer power = bolt.getAttached(EhmAttachments.EHM_FIREBOLT_POWER);
        if (power == null || !(bolt.level() instanceof ServerLevel level)) {
            return;
        }
        UUID id = bolt.getAttached(EhmAttachments.EHM_FIREBOLT_TARGET);
        if (id == null) {
            return;
        }
        Entity entity = level.getEntity(id);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        Vec3 pos = bolt.position();
        Vec3 motion = bolt.getDeltaMovement();
        Vec3 aim = living.getEyePosition();
        double[] next = AbilityRules.seekDelta(
                pos.x,
                pos.y,
                pos.z,
                motion.x,
                motion.y,
                motion.z,
                aim.x,
                aim.y,
                aim.z,
                AbilityRules.FIRE_BOLT_HOMING);
        bolt.setDeltaMovement(next[0], next[1], next[2]);
    }

    static ServerPlayer healTarget(ServerPlayer caster, LivingEntity clicked, double range) {
        if (clicked instanceof ServerPlayer player && isHealable(caster, player, range)) {
            return player;
        }
        EntityHitResult hit = raycast(caster, range);
        if (hit != null && hit.getEntity() instanceof ServerPlayer player && isHealable(caster, player, range)) {
            return player;
        }
        if (isSelfHealable(caster)) {
            return caster;
        }
        return null;
    }

    static ServerPlayer ironHeartTarget(ServerPlayer caster, LivingEntity clicked, double range) {
        if (clicked instanceof ServerPlayer player && isIronHeartable(caster, player, range)) {
            return player;
        }
        EntityHitResult hit = raycast(caster, range);
        if (hit != null && hit.getEntity() instanceof ServerPlayer player && isIronHeartable(caster, player, range)) {
            return player;
        }
        return caster;
    }

    static boolean isIronHeartable(ServerPlayer caster, ServerPlayer target, double range) {
        if (target == caster || Achievements.skipPlayer(target) || !target.isAlive()) {
            return false;
        }
        if (remainingIronHeartBuff(target) > 0) {
            return false;
        }
        if (caster.distanceTo(target) > range) {
            return false;
        }
        return friendly(caster, target);
    }

    static LivingEntity fireTarget(ServerPlayer caster, LivingEntity clicked, double range) {
        if (clicked != null && isFireable(caster, clicked) && caster.distanceTo(clicked) <= range) {
            return clicked;
        }
        EntityHitResult hit = raycast(caster, range);
        if (hit != null && hit.getEntity() instanceof LivingEntity living && isFireable(caster, living)) {
            return living;
        }
        return nearestAlongAim(caster, range);
    }

    static LivingEntity nearestAlongAim(ServerPlayer caster, double range) {
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        AABB box = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(3.0);
        List<LivingEntity> found = level.getEntitiesOfClass(
                LivingEntity.class, box, living -> isFireable(caster, living));
        if (found.isEmpty()) {
            return null;
        }
        double[] xs = new double[found.size()];
        double[] ys = new double[found.size()];
        double[] zs = new double[found.size()];
        for (int i = 0; i < found.size(); i++) {
            Vec3 pos = found.get(i).getEyePosition();
            xs[i] = pos.x;
            ys[i] = pos.y;
            zs[i] = pos.z;
        }
        int index = AbilityRules.nearestAlongAim(
                start.x, start.y, start.z, look.x, look.y, look.z, range, xs, ys, zs);
        if (index < 0) {
            return null;
        }
        return found.get(index);
    }

    static boolean isHealable(ServerPlayer caster, ServerPlayer target, double range) {
        if (target == caster || Achievements.skipPlayer(target) || !target.isAlive()) {
            return false;
        }
        if (target.getHealth() >= target.getMaxHealth()) {
            return false;
        }
        if (caster.distanceTo(target) > range) {
            return false;
        }
        return friendly(caster, target);
    }

    static boolean isSelfHealable(ServerPlayer caster) {
        return caster.isAlive() && caster.getHealth() < caster.getMaxHealth();
    }

    static boolean isFireable(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive();
    }

    static boolean friendly(ServerPlayer caster, ServerPlayer target) {
        PlayerTeam casterTeam = caster.getTeam();
        PlayerTeam targetTeam = target.getTeam();
        if (casterTeam != null && targetTeam != null && casterTeam != targetTeam) {
            return false;
        }
        return true;
    }

    /** First living creature the look hits before a block, within {@code range}. */
    public static EntityHitResult raycast(Entity player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        BlockHitResult block = player.level()
                .clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(clipEnd.subtract(start)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(
                player,
                start,
                clipEnd,
                box,
                entity -> entity instanceof LivingEntity living && living.isAlive() && living != player,
                range * range);
    }

    static boolean targetingUsableBlock(ServerPlayer player) {
        if (player.isSecondaryUseActive()) {
            return false;
        }
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level()
                .clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && skipsManaAbilityUse(player, hit.getBlockPos());
    }

    static boolean skipsManaAbilityUse(ServerPlayer player, BlockPos pos) {
        if (player.isSecondaryUseActive()) {
            return false;
        }
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getMenuProvider(level, pos) != null) {
            return true;
        }
        return state.is(EhmTags.SKIPS_MANA_ABILITY_USE);
    }

    static boolean alreadyHandled(ServerPlayer player, ServerLevel level) {
        Long tick = player.getAttached(EhmAttachments.EHM_ABILITY_HANDLED_TICK);
        return tick != null && tick == level.getGameTime();
    }

    static void markHandled(ServerPlayer player) {
        player.setAttached(EhmAttachments.EHM_ABILITY_HANDLED_TICK, player.level().getGameTime());
    }

    static int uses(ServerPlayer player, String ability) {
        return usesMap(player).getOrDefault(ability, 0);
    }

    static Map<String, Integer> usesMap(ServerPlayer player) {
        foldRetiredFireBolt(player);
        return player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_USES, Map.of());
    }

    /** Moves a saved Fire bolt record onto Magic Arrow. Does nothing once that id is gone. */
    static void foldRetiredFireBolt(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Map<String, Integer> uses = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_USES, Map.of());
        List<String> learned = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LEARNED, List.of());
        List<String> taught = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_TAUGHT, List.of());
        Map<String, Long> lastUse = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LAST_USE, Map.of());
        boolean usesHit = uses != null && uses.containsKey(AbilityRules.FIRE_BOLT);
        boolean learnedHit = learned != null && learned.contains(AbilityRules.FIRE_BOLT);
        boolean taughtHit = taught != null && taught.contains(AbilityRules.FIRE_BOLT);
        boolean lastHit = lastUse != null && lastUse.containsKey(AbilityRules.FIRE_BOLT);
        if (!usesHit && !learnedHit && !taughtHit && !lastHit) {
            return;
        }
        if (usesHit) {
            player.setAttached(EhmAttachments.EHM_ABILITY_USES, AbilityRules.foldFireBoltUses(uses));
        }
        if (learnedHit) {
            player.setAttached(EhmAttachments.EHM_ABILITY_LEARNED, AbilityRules.foldFireBoltList(learned));
        }
        if (taughtHit) {
            player.setAttached(EhmAttachments.EHM_ABILITY_TAUGHT, AbilityRules.foldFireBoltList(taught));
        }
        if (lastHit) {
            player.setAttached(EhmAttachments.EHM_ABILITY_LAST_USE, AbilityRules.foldFireBoltLastUse(lastUse));
        }
    }

    static double abilityPower(ServerPlayer player, String ability, int itemBonus) {
        tickGoldWear(player);
        int manaLevel = Achievements.manaLevel(player);
        double power = AbilityRules.power(
                ability,
                manaLevel,
                uses(player, ability),
                Math.max(0, itemBonus),
                AbilityRules.isTrained(manaLevel, learnedSet(player), ability));
        if (empowerActive(player)) {
            power += AbilityRules.empowerCastBonus(empowerLevel(player));
        }
        return power;
    }

    static void tickGoldWear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        Map<String, String> now = new HashMap<>();
        putGoldSlot(now, "head", player.getItemBySlot(EquipmentSlot.HEAD), true);
        putGoldSlot(now, "chest", player.getItemBySlot(EquipmentSlot.CHEST), true);
        putGoldSlot(now, "legs", player.getItemBySlot(EquipmentSlot.LEGS), true);
        putGoldSlot(now, "feet", player.getItemBySlot(EquipmentSlot.FEET), true);
        putGoldSlot(now, "main", player.getMainHandItem(), false);
        putGoldSlot(now, "off", player.getOffhandItem(), false);
        Map<String, String> previous = player.getAttachedOrElse(EhmAttachments.EHM_GOLD_WEAR, Map.of());
        long gameTime = level.getGameTime();
        Map<String, String> next = AbilityRules.updateGoldWear(previous, now, gameTime);
        if (!next.equals(previous)) {
            player.setAttached(EhmAttachments.EHM_GOLD_WEAR, next);
        }
        int ready = AbilityRules.readyGoldBonus(next, gameTime);
        int shown = Math.max(0, player.getAttachedOrElse(EhmAttachments.EHM_GOLD_BONUS_SHOWN, 0));
        if (ready == shown) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_GOLD_BONUS_SHOWN, ready);
        Achievements.sendMana(player);
        List<String> messages = AbilityRules.goldBonusMessages(shown, ready);
        if (messages.isEmpty()) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_GOLD_SHIMMER_UNTIL, gameTime + AbilityRules.GOLD_SHIMMER_TICKS);
        spawnGoldShimmer(player, level);
        for (String message : messages) {
            player.sendSystemMessage(Component.translatableWithFallback("tougher.message.gold_ability", message));
        }
    }

    static void tickGoldShimmer(ServerPlayer player) {
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_GOLD_SHIMMER_UNTIL, -1L);
        if (until == null || until < 0L || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!player.isAlive() || until <= level.getGameTime()) {
            player.setAttached(EhmAttachments.EHM_GOLD_SHIMMER_UNTIL, -1L);
            return;
        }
        spawnGoldShimmer(player, level);
    }

    static void spawnGoldShimmer(ServerPlayer player, ServerLevel level) {
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.6;
        double z = player.getZ();
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 4, 0.3, 0.45, 0.3, 0.01);
        level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 6, 0.35, 0.5, 0.35, 0.0);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 1.0F, 0.84F, 0.25F),
                x,
                y,
                z,
                4,
                0.3,
                0.45,
                0.3,
                0.0);
    }

    private static void putGoldSlot(Map<String, String> slots, String slot, ItemStack stack, boolean armor) {
        String id = heldItemId(stack);
        if (armor ? AbilityRules.isGoldArmor(id) : AbilityRules.isHeldGoldenItem(id)) {
            slots.put(slot, id);
        }
    }

    /**
     * Learn an ability from a wise teacher, or exempt one the player already knows.
     * The lesson does not use a self-learned slot. False when it was already taught or is not a real ability.
     */
    public static boolean teachAbility(ServerPlayer player, String ability) {
        if (player == null || !AbilityRules.ABILITY_IDS.contains(ability)) {
            return false;
        }
        ensureLearnedMigrated(player);
        if (taughtByWise(player, ability)) {
            return false;
        }
        if (!AbilityRules.isLearned(learnedSet(player), ability)) {
            List<String> next = new ArrayList<>(learnedSet(player));
            next.add(ability);
            player.setAttached(EhmAttachments.EHM_ABILITY_LEARNED, next);
        }
        List<String> taught = new ArrayList<>(player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_TAUGHT, List.of()));
        taught.add(ability);
        player.setAttached(EhmAttachments.EHM_ABILITY_TAUGHT, taught);
        return true;
    }

    public static boolean taughtByWise(ServerPlayer player, String ability) {
        return AbilityRules.isLearned(taughtSet(player), ability);
    }

    public static boolean knowsAbility(ServerPlayer player, String ability) {
        return AbilityRules.isLearned(learnedSet(player), ability);
    }

    static void recordAbilityUse(ServerPlayer player, String ability) {
        ensureLearnedMigrated(player);
        putInt(player, EhmAttachments.EHM_ABILITY_USES, ability, uses(player, ability) + 1);
        int manaLevel = Achievements.manaLevel(player);
        Set<String> learned = new HashSet<>(learnedSet(player));
        if (!AbilityRules.isLearned(learned, ability)
                && AbilityRules.canLearnAbility(manaLevel, learned, taughtSet(player))) {
            List<String> next = new ArrayList<>(learned);
            next.add(ability);
            player.setAttached(EhmAttachments.EHM_ABILITY_LEARNED, next);
        }
    }

    static void ensureLearnedMigrated(ServerPlayer player) {
        if (Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LEARNED_MIGRATED, Boolean.FALSE))) {
            return;
        }
        List<String> learned = new ArrayList<>();
        Map<String, Integer> uses = usesMap(player);
        for (String ability : AbilityRules.ABILITY_IDS) {
            if (uses.getOrDefault(ability, 0) > 0) {
                learned.add(ability);
            }
        }
        player.setAttached(EhmAttachments.EHM_ABILITY_LEARNED, learned);
        player.setAttached(EhmAttachments.EHM_ABILITY_LEARNED_MIGRATED, Boolean.TRUE);
    }

    static Set<String> taughtSet(ServerPlayer player) {
        foldRetiredFireBolt(player);
        List<String> taught = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_TAUGHT, List.of());
        if (taught == null || taught.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(taught);
    }

    static Set<String> learnedSet(ServerPlayer player) {
        foldRetiredFireBolt(player);
        ensureLearnedMigrated(player);
        List<String> learned = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LEARNED, List.of());
        if (learned == null || learned.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(learned);
    }

    static long lastUse(ServerPlayer player, String ability) {
        foldRetiredFireBolt(player);
        return player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LAST_USE, Map.of()).getOrDefault(ability, -1L);
    }

    static void putInt(
            ServerPlayer player,
            net.fabricmc.fabric.api.attachment.v1.AttachmentType<Map<String, Integer>> type,
            String key,
            int value) {
        Map<String, Integer> map = TutorialCounts.mutableCopy(player.getAttachedOrElse(type, Map.of()));
        map.put(key, value);
        player.setAttached(type, map);
    }

    static void putLong(
            ServerPlayer player,
            net.fabricmc.fabric.api.attachment.v1.AttachmentType<Map<String, Long>> type,
            String key,
            long value) {
        Map<String, Long> stored = player.getAttachedOrElse(type, Map.of());
        Map<String, Long> map = stored == null || stored.isEmpty() ? new java.util.HashMap<>() : new java.util.HashMap<>(stored);
        map.put(key, value);
        player.setAttached(type, map);
    }
}
