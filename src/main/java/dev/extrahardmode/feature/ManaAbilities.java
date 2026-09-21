package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.module.TutorialCounts;
import dev.extrahardmode.network.ClientboundAbilityDurationsPayload;
import dev.extrahardmode.network.ClientboundFlightPayload;
import dev.extrahardmode.network.ClientboundPowerMinePayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
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
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/** Mana abilities: heal, iron heart, fire bolt, magic arrow, flight, let it grow, let there be light, power mining, detect ore, slow, sense evil, smite evil. */
public final class ManaAbilities implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("mana_abilities");
    private static final float MINING_REACH = 5.0F;
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
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
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
            tickFlight(player);
            tickPowerMine(player);
            tickPlayerLight(player);
            tickAbilityHint(player);
            tickHealSparkle(player);
            tickIronHeart(player);
            tickIronHeartSparkle(player);
            tickSenseSparkle(player);
            maybeSendAbilityDurations(player);
        }
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
            if (serverPlayer.getMainHandItem().is(Items.CHARCOAL) && tryFireBolt(serverPlayer, null)) {
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
                && serverPlayer.getMainHandItem().is(Items.CHARCOAL)
                && tryFireBolt(serverPlayer, null)) {
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
                && serverPlayer.getMainHandItem().is(Items.CHARCOAL)
                && tryFireBolt(serverPlayer, entity instanceof LivingEntity living ? living : null)) {
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
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()));
        double power = abilityPower(player, AbilityRules.FLIGHT, bonus);
        consumeManaAndHeld(player, Items.FEATHER);
        recordAbilityUse(player, AbilityRules.FLIGHT);
        startFlight(player, power);
        tellAbilityUse(player, "tougher.ability.flight", "Flight", power);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.flight_start", "Jump to rise, sneak to descend. Land to stop flying."));
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
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()))
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
        if (honey && InventorySearch.consumeOneMain(player, Items.HONEY_BOTTLE)) {
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE), net.minecraft.util.Prediction.SERVER_ONLY);
        }
        target.heal((float) power);
        spend(player, Items.PAPER, AbilityRules.HEAL);
        startHealSparkle(target);
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
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()));
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

    static void startHealSparkle(ServerPlayer target) {
        ServerLevel level = (ServerLevel) target.level();
        target.setAttached(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, level.getGameTime() + AbilityRules.HEAL_SPARKLE_TICKS);
        spawnHealSparkle(target, level);
    }

    static void tickHealSparkle(ServerPlayer player) {
        Long until = player.getAttachedOrElse(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, -1L);
        if (until == null || until < 0L) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (!player.isAlive() || until <= level.getGameTime()) {
            player.setAttached(EhmAttachments.EHM_HEAL_SPARKLE_UNTIL, -1L);
            return;
        }
        spawnHealSparkle(player, level);
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
        double power = abilityPower(caster, AbilityRules.IRON_HEART, 0);
        int duration = AbilityRules.ironHeartDurationTicks(power);
        float health = AbilityRules.ironHeartHealth(power);
        if (duration <= 0 || health <= 0.0F) {
            return false;
        }
        startIronHeartLock(caster, duration);
        applyIronHeartBuff(caster, target, health, duration);
        startIronHeartSparkle(target);
        tellAbilityUse(caster, "tougher.ability.iron_heart", "Iron Heart", power);
        return true;
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

    static boolean tryFireBolt(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.CHARCOAL) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()));
        double power = abilityPower(player, AbilityRules.FIRE_BOLT, bonus);
        double range = AbilityRules.fireRange(power);
        LivingEntity target = fireTarget(player, clicked, range);
        if (target == null) {
            return false;
        }
        if (blockedByCooldownOrMana(player, AbilityRules.FIRE_BOLT, "tougher.ability.fire_bolt", "Fire bolt")) {
            return true;
        }
        hurlFireBolt(player, power, target);
        spend(player, Items.CHARCOAL, AbilityRules.FIRE_BOLT);
        tellAbilityUse(player, "tougher.ability.fire_bolt", "Fire bolt", power);
        return true;
    }

    static boolean tryMagicArrow(ServerPlayer player, LivingEntity clicked) {
        if (!player.getMainHandItem().is(Items.ARROW) || Achievements.skipPlayer(player)) {
            return false;
        }
        if (alreadyHandled(player, (ServerLevel) player.level())) {
            return true;
        }
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()));
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
        hurlMagicArrow(player, power, target);
        spend(player, Items.ARROW, AbilityRules.MAGIC_ARROW);
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
        int bonus = withRedstoneBonus(player, 0);
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
        int hoeBonus = withRedstoneBonus(player, AbilityRules.growHoeBonus(heldItemId(held)));
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
        int hoeBonus = withRedstoneBonus(player, AbilityRules.growHoeBonus(heldItemId(held)));
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
        int bonus = withRedstoneBonus(player, AbilityRules.pickaxeBonus(heldItemId(held)));
        double power = abilityPower(player, AbilityRules.POWER_MINE, bonus);
        int duration = AbilityRules.powerMineDurationTicks(power);
        spend(player, null, AbilityRules.POWER_MINE);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_REMAINING, duration);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_BAR_MAX, duration);
        player.setAttached(EhmAttachments.EHM_POWER_MINE_ITEM, heldItemId(held));
        sendPowerMine(player);
        sendAbilityDurations(player);
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
        int bonus = detectOreInventoryBonus(player);
        double power = abilityPower(player, AbilityRules.DETECT_ORE, bonus);
        int range = AbilityRules.detectOreRange(power);
        AbilityRules.OreDeposit best = DetectOre.findBest(player, range, power);
        consumeDetectOreQuartz(player);
        spend(player, null, AbilityRules.DETECT_ORE);
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
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()));
        double power = abilityPower(player, AbilityRules.SLOW, bonus);
        ServerLevel level = (ServerLevel) player.level();
        double range = AbilityRules.slowRange(power);
        int duration = AbilityRules.slowDurationTicks(power);
        if (range > 0.0 && duration > 0) {
            AABB box = player.getBoundingBox().inflate(range);
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, box, target -> isSlowTarget(player, target, range))) {
                applySlow(mob, power, duration, level);
            }
        }
        spend(player, Items.STRING, AbilityRules.SLOW);
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
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(player.getMainHandItem().getCount()));
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
        int bonus = withRedstoneBonus(player, AbilityRules.catalystBonus(held.getCount()));
        double power = abilityPower(player, AbilityRules.LIGHT, bonus);
        spend(player, held.getItem(), AbilityRules.LIGHT);
        startPlayerLight(player, power);
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
            double power = abilityPower(player, AbilityRules.LIGHT, 0);
            player.setAttached(
                    EhmAttachments.EHM_MANA_CURRENT,
                    Math.max(0.0, Achievements.currentMana(player) - AbilityRules.manaCost(AbilityRules.LIGHT)));
            Achievements.sendMana(player);
            recordAbilityUse(player, AbilityRules.LIGHT);
            startPlayerLight(player, power);
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

    static boolean isSlowTarget(ServerPlayer caster, LivingEntity target, double range) {
        if (target == caster || !target.isAlive() || !(target instanceof Enemy)) {
            return false;
        }
        return caster.distanceTo(target) <= range;
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
    }

    static void clearSlow(LivingEntity target) {
        AttributeInstance speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ExtraHardModeMod.ABILITY_SLOW);
        }
        target.setAttached(EhmAttachments.EHM_SLOW_UNTIL, -1L);
        SLOWED.remove(target.getUUID());
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
                    it.remove();
                }
                continue;
            }
            Long until = living.getAttachedOrElse(EhmAttachments.EHM_SLOW_UNTIL, -1L);
            if (until == null || until < 0L || until <= now) {
                clearSlow(living);
            }
        }
    }

    static int detectOreInventoryBonus(ServerPlayer player) {
        return AbilityRules.detectOreItemBonus(hasItem(player, Items.QUARTZ), hasRedstoneDust(player));
    }

    static void consumeDetectOreQuartz(ServerPlayer player) {
        consumeOne(player, stack -> stack.is(Items.QUARTZ));
    }

    static int withRedstoneBonus(ServerPlayer player, int itemBonus) {
        return Math.max(0, itemBonus) + AbilityRules.redstoneDustBonus(hasRedstoneDust(player));
    }

    public static int supplyBonus(ServerPlayer player, String ability) {
        ItemStack held = player.getMainHandItem();
        return AbilityRules.supplyBonus(
                ability,
                heldItemId(held),
                held.getCount(),
                hasRedstoneDust(player),
                hasItem(player, Items.QUARTZ));
    }

    public static String meLineValue(ServerPlayer player, AbilityRules.AbilitySkill skill) {
        int manaLevel = Achievements.manaLevel(player);
        int bonus = supplyBonus(player, skill.id());
        double effective = AbilityRules.power(
                skill.id(),
                manaLevel,
                skill.uses(),
                bonus,
                AbilityRules.isTrained(manaLevel, learnedSet(player), skill.id()));
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

    static void consumeRedstoneDustBooster(ServerPlayer player) {
        consumeOne(player, stack -> stack.is(Items.REDSTONE));
    }

    static boolean hasRedstoneDust(ServerPlayer player) {
        return hasItem(player, Items.REDSTONE);
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
        }
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
        if (!player.onGround()) {
            player.setAttached(EhmAttachments.EHM_FLIGHT_LEFT_GROUND, Boolean.TRUE);
        }
        if (Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_LEFT_GROUND, Boolean.FALSE))
                && player.onGround()) {
            stopFlight(player, false);
            return;
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
            ItemStack held = player.getMainHandItem();
            boolean feather = held.is(Items.FEATHER);
            int bonus = withRedstoneBonus(player, feather ? AbilityRules.catalystBonus(held.getCount()) : 0);
            double power = abilityPower(player, AbilityRules.FLIGHT, bonus);
            consumeManaAndHeld(player, feather ? Items.FEATHER : null);
            recordAbilityUse(player, AbilityRules.FLIGHT);
            int extra = AbilityRules.flightDurationTicks(power);
            float speed = AbilityRules.flightSpeed(power);
            player.setAttached(EhmAttachments.EHM_FLIGHT_REMAINING, extra);
            player.setAttached(EhmAttachments.EHM_FLIGHT_BAR_MAX, extra);
            player.setAttached(EhmAttachments.EHM_FLIGHT_SPEED, speed);
            applyFlightAbilities(player, speed);
            sendFlight(player);
            sendAbilityDurations(player);
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
        player.setAttached(EhmAttachments.EHM_FLIGHT_LEFT_GROUND, Boolean.FALSE);
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
        player.setAttached(EhmAttachments.EHM_FLIGHT_LEFT_GROUND, Boolean.FALSE);
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
        if (!AbilityRules.hasManaToUse(ability, Achievements.currentMana(player))) {
            return;
        }
        int hoeBonus = AbilityRules.redstoneDustBonus(hasRedstoneDust(player));
        if (held.is(ItemTags.HOES)) {
            hoeBonus += AbilityRules.growHoeBonus(heldItemId(held));
        } else if (held.is(ItemTags.PICKAXES)) {
            if (isPowerMining(player)) {
                return;
            }
            hoeBonus += AbilityRules.pickaxeBonus(heldItemId(held));
        } else if (held.is(Items.COMPASS)) {
            hoeBonus += AbilityRules.detectOreItemBonus(hasItem(player, Items.QUARTZ), false);
        } else if (held.is(Items.STRING)) {
            hoeBonus += AbilityRules.catalystBonus(held.getCount());
        } else if (held.is(Items.ARROW) || held.is(Items.CHARCOAL) || held.is(Items.IRON_INGOT)) {
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
                || held.is(Items.CHARCOAL)
                || held.is(Items.STRING)
                || held.is(Items.GOLDEN_SWORD)
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

    static void tellAbilityUse(ServerPlayer player, String nameKey, String fallback, double power) {
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.ability_use",
                "You are using the %s ability at level %s!",
                Component.translatableWithFallback(nameKey, fallback),
                Component.literal(AbilityRules.powerLabel(power))));
    }

    static void consumeManaAndHeld(ServerPlayer player, net.minecraft.world.item.Item consumed) {
        tryConsumeHeld(player, consumed);
        consumeRedstoneDustBooster(player);
        player.setAttached(
                EhmAttachments.EHM_MANA_CURRENT,
                Math.max(0.0, Achievements.currentMana(player) - AbilityRules.MANA_COST));
        Achievements.sendMana(player);
    }

    /** Leaves the last item in the stack so the ability can still be triggered. */
    static boolean tryConsumeHeld(ServerPlayer player, net.minecraft.world.item.Item consumed) {
        if (consumed == null) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(consumed) || !AbilityRules.consumesCatalyst(held.getCount())) {
            return false;
        }
        held.shrink(1);
        return true;
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
        int warn = AbilityDurationRules.WARN_TICKS;
        return inDurationWindow(flight, warn)
                || inDurationWindow(mine, warn)
                || inDurationWindow(light, warn)
                || inDurationWindow(heart, warn);
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
    }

    static boolean isFlying(ServerPlayer player) {
        return Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_ACTIVE, Boolean.FALSE));
    }

    static int remainingFlight(ServerPlayer player) {
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_FLIGHT_REMAINING, 0);
        return value == null ? 0 : Math.max(0, value);
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
        if (held.is(Items.CHARCOAL)) {
            return AbilityRules.FIRE_BOLT;
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
        consumeRedstoneDustBooster(player);
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

    static void hurlFireBolt(ServerPlayer player, double power, LivingEntity target) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = player.getLookAngle().normalize();
        SmallFireball bolt = new SmallFireball(level, player, direction.scale(AbilityRules.FIRE_BOLT_SPEED));
        bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        tagSeeker(bolt, power, target);
        level.addFreshEntity(bolt);
    }

    static void hurlMagicArrow(ServerPlayer player, double power, LivingEntity target) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 direction = player.getLookAngle().normalize();
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
        arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        arrow.setDeltaMovement(direction.scale(AbilityRules.FIRE_BOLT_SPEED));
        arrow.setNoGravity(true);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        tagSeeker(arrow, power, target);
        level.addFreshEntity(arrow);
    }

    static void tagSeeker(Entity bolt, double power, LivingEntity target) {
        bolt.setAttached(EhmAttachments.EHM_FIREBOLT_POWER, Math.max(1, (int) Math.round(power)));
        if (target != null) {
            bolt.setAttached(EhmAttachments.EHM_FIREBOLT_TARGET, target.getUUID());
        }
    }

    public static void steerFireBolt(SmallFireball bolt) {
        steerSeeker(bolt);
    }

    public static void steerSeeker(Entity bolt) {
        Integer power = bolt.getAttached(EhmAttachments.EHM_FIREBOLT_POWER);
        if (power == null || power <= 0 || !(bolt.level() instanceof ServerLevel level)) {
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

    static EntityHitResult raycast(ServerPlayer player, double range) {
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
        Vec3 end = start.add(player.getViewVector(1.0F).scale(MINING_REACH));
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
        return player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_USES, Map.of());
    }

    static double abilityPower(ServerPlayer player, String ability, int itemBonus) {
        int manaLevel = Achievements.manaLevel(player);
        return AbilityRules.power(
                ability,
                manaLevel,
                uses(player, ability),
                itemBonus,
                AbilityRules.isTrained(manaLevel, learnedSet(player), ability));
    }

    /**
     * Learn an ability from a wise teacher. Ignores the mana-level slot cap.
     * False when it is already known or not a real ability.
     */
    public static boolean teachAbility(ServerPlayer player, String ability) {
        if (player == null || !AbilityRules.ABILITY_IDS.contains(ability)) {
            return false;
        }
        ensureLearnedMigrated(player);
        if (AbilityRules.isLearned(learnedSet(player), ability)) {
            return false;
        }
        List<String> next = new ArrayList<>(learnedSet(player));
        next.add(ability);
        player.setAttached(EhmAttachments.EHM_ABILITY_LEARNED, next);
        return true;
    }

    public static boolean knowsAbility(ServerPlayer player, String ability) {
        return AbilityRules.isLearned(learnedSet(player), ability);
    }

    static void recordAbilityUse(ServerPlayer player, String ability) {
        ensureLearnedMigrated(player);
        putInt(player, EhmAttachments.EHM_ABILITY_USES, ability, uses(player, ability) + 1);
        int manaLevel = Achievements.manaLevel(player);
        Set<String> learned = new HashSet<>(learnedSet(player));
        if (!AbilityRules.isLearned(learned, ability) && AbilityRules.canLearnAbility(manaLevel, learned)) {
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

    static Set<String> learnedSet(ServerPlayer player) {
        ensureLearnedMigrated(player);
        List<String> learned = player.getAttachedOrElse(EhmAttachments.EHM_ABILITY_LEARNED, List.of());
        if (learned == null || learned.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(learned);
    }

    static long lastUse(ServerPlayer player, String ability) {
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
