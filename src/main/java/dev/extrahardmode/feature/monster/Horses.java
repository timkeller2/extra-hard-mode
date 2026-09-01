package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Block donkey/mule/llama chests below the cave band so they cannot be used as
 * portable chests underground.
 */
public final class Horses implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("horses");
    public static final int DEFAULT_BLOCK_CHEST_BELOW_Y = 48;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseEntityCallback.EVENT, ID, Horses::onUseEntity);
    }

    static InteractionResult onUseEntity(
            Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
        if (level.isClientSide()
                || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID) || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!(entity instanceof AbstractChestedHorse horse)) {
            return InteractionResult.PASS;
        }
        WorldConfig config = ConfigManager.world(serverLevel);
        if (denyChestInteract(horse, player, hand, config.horseBlockChest(), config.horseBlockChestBelowY())) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public static boolean denyChestInteract(
            AbstractChestedHorse horse, Player player, InteractionHand hand, boolean enable, int belowY) {
        if (!shouldBlockChest(horse.getBlockY(), enable, belowY)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean attaching = !horse.hasChest() && stack.getItem() == Items.CHEST;
        boolean opening = horse.hasChest() && player.isSecondaryUseActive();
        return attaching || opening;
    }

    public static boolean shouldBlockChest(AbstractChestedHorse horse, WorldConfig config) {
        return shouldBlockChest(horse.getBlockY(), config.horseBlockChest(), config.horseBlockChestBelowY());
    }

    public static boolean shouldBlockChest(int y, boolean enable, int belowY) {
        if (!enable) {
            return false;
        }
        return belowChestLimit(y, belowY);
    }

    /**
     * {@code Integer.MIN_VALUE} disables the Y gate without turning the module off.
     */
    public static boolean belowChestLimit(int y, int belowY) {
        if (belowY == Integer.MIN_VALUE) {
            return false;
        }
        return y < belowY;
    }
}
