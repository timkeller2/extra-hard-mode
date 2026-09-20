package dev.extrahardmode.task;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.PlayerSettings;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.player.WeightFormula;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Every 20 ticks: if the player is in water and overencumbered (or in a 1-wide waterfall),
 * roll drownRate + extra*2 and apply bubble drain and downward velocity (no extra HP damage).
 */
public final class WeightCheckTask {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private WeightCheckTask() {}

    public static void run(ServerLevel level, WorldConfig world) {
        PlayerSettings settings = world.player();
        if (!settings.weightEnable()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            check(level, player, settings);
        }
    }

    static void check(ServerLevel level, ServerPlayer player, PlayerSettings settings) {
        if (EhmApi.playerBypasses(player) || player.isSpectator() || player.isPassenger()) {
            return;
        }
        double weight = inventoryWeight(player, settings);
        player.setAttached(EhmAttachments.EHM_WEIGHT_CACHE, weight);
        if (ConfigManager.global().debug()) {
            ExtraHardModeMod.LOGGER.debug("EHM weight {}/{} for {}", weight, settings.maxPoints(), player.getScoreboardName());
        }

        boolean inWater = player.isInWater();
        boolean waterfall = settings.blockWaterfalls() && isOneWideWaterColumn(level, player.blockPosition());
        if (!inWater && !waterfall) {
            return;
        }
        boolean over = weight > settings.maxPoints();
        if (!over && !waterfall) {
            return;
        }
        double effective = over ? weight : settings.maxPoints();
        int rate = WeightFormula.drownRatePercent(
                effective, settings.maxPoints(), settings.drownRate(), settings.overencumbranceAdds());
        if (rate <= 0 || player.getRandom().nextInt(100) >= rate) {
            return;
        }
        player.setAirSupply(Math.max(-20, player.getAirSupply() - 60));
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.min(motion.y, -0.5), motion.z);
        ((dev.extrahardmode.mixin.EntityHurtAccess) player).tougher$markHurt();
    }

    public static double inventoryWeight(ServerPlayer player, PlayerSettings settings) {
        int armorPieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                armorPieces++;
            }
        }
        double stackUnits = 0.0;
        int tools = 0;
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (isTool(stack)) {
                tools++;
            } else {
                stackUnits += stack.getCount() / (double) Math.max(1, stack.getMaxStackSize());
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            if (isTool(offhand)) {
                tools++;
            } else {
                stackUnits += offhand.getCount() / (double) Math.max(1, offhand.getMaxStackSize());
            }
        }
        return WeightFormula.weight(
                armorPieces, stackUnits, tools, settings.armorPiece(), settings.stack(), settings.tool());
    }

    static boolean isTool(ItemStack stack) {
        if (stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.SPEARS)) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_axe")
                || path.endsWith("_shovel")
                || path.endsWith("_sword")
                || path.endsWith("_hoe")
                || path.endsWith("_bucket")
                || path.equals("bucket")
                || path.equals("bow")
                || path.equals("crossbow")
                || path.equals("fishing_rod")
                || path.equals("clock")
                || path.equals("compass")
                || path.equals("flint_and_steel");
    }

    static boolean isOneWideWaterColumn(ServerLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return false;
            }
        }
        return level.getFluidState(pos.above()).is(FluidTags.WATER)
                || level.getFluidState(pos.below()).is(FluidTags.WATER);
    }
}
