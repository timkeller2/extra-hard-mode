package dev.extrahardmode.feature;

import dev.extrahardmode.mixin.CompoundContainerAccess;
import dev.extrahardmode.mixin.FurnaceRecipesAccess;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Moves furnace cooking XP with hopper output and grants it when a player takes the items. */
public final class FurnaceXp {
    private FurnaceXp() {}

    public static int countItems(Container container) {
        if (container == null) {
            return 0;
        }
        int total = 0;
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            total += container.getItem(i).getCount();
        }
        return total;
    }

    public static int getMilli(Container container) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            return getMilli(access.tougher$container1()) + getMilli(access.tougher$container2());
        }
        AttachmentTarget target = target(container);
        if (target == null) {
            return 0;
        }
        Integer value = target.getAttachedOrElse(EhmAttachments.EHM_COOKING_XP_MILLI, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    public static void addMilli(Container container, int milli) {
        if (milli == 0) {
            return;
        }
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            if (milli > 0) {
                addMilli(access.tougher$container1(), milli);
            } else {
                int taken = takeMilli(access.tougher$container1(), -milli);
                takeMilli(access.tougher$container2(), -milli - taken);
            }
            return;
        }
        AttachmentTarget target = target(container);
        if (target == null) {
            return;
        }
        int next = getMilli(container) + milli;
        if (next <= 0) {
            target.setAttached(EhmAttachments.EHM_COOKING_XP_MILLI, 0);
            return;
        }
        target.setAttached(EhmAttachments.EHM_COOKING_XP_MILLI, next);
    }

    public static int takeMilli(Container container, int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            int first = takeMilli(access.tougher$container1(), amount);
            return first + takeMilli(access.tougher$container2(), amount - first);
        }
        int have = getMilli(container);
        int take = Math.min(have, amount);
        addMilli(container, -take);
        return take;
    }

    public static void onItemsMoved(Container source, Container destination, int moved, int sourceCountBefore) {
        if (moved <= 0 || source instanceof AbstractFurnaceBlockEntity) {
            return;
        }
        ServerLevel level = levelOf(source);
        if (level == null) {
            level = levelOf(destination);
        }
        if (level == null || !WorldGate.isActive(level)) {
            return;
        }
        int give = FurnaceXpRules.transferMilli(getMilli(source), moved, sourceCountBefore);
        if (give <= 0) {
            return;
        }
        takeMilli(source, give);
        addMilli(destination, give);
    }

    public static int takeOneRecipeMilli(AbstractFurnaceBlockEntity furnace, ServerLevel level) {
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> used =
                ((FurnaceRecipesAccess) furnace).tougher$recipesUsed();
        if (used == null || used.isEmpty()) {
            return 0;
        }
        ResourceKey<Recipe<?>> key = null;
        int count = 0;
        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : used.reference2IntEntrySet()) {
            if (entry.getIntValue() > 0) {
                key = entry.getKey();
                count = entry.getIntValue();
                break;
            }
        }
        if (key == null) {
            return 0;
        }
        if (count <= 1) {
            used.removeInt(key);
        } else {
            used.put(key, count - 1);
        }
        furnace.setChanged();
        RecipeHolder<?> holder = level.recipeAccess().byKey(key).orElse(null);
        if (holder == null || !(holder.value() instanceof AbstractCookingRecipe cooking)) {
            return 0;
        }
        return FurnaceXpRules.toMilli(cooking.experience());
    }

    public static void onFurnaceExtracted(AbstractFurnaceBlockEntity furnace, Container destination, ServerLevel level) {
        int milli = FurnaceXpRules.withAutomationBonus(takeOneRecipeMilli(furnace, level));
        addMilli(destination, milli);
    }

    public static void onComposterExtracted(Container destination, ServerLevel level) {
        if (level == null || !WorldGate.isActive(level)) {
            return;
        }
        int milli = FurnaceXpRules.withAutomationBonus(FurnaceXpRules.toMilli(FurnaceXpRules.COMPOSTER_EXPERIENCE));
        addMilli(destination, milli);
    }

    public static void giveToPlayer(ServerPlayer player, Container container, int taken, int leftAfterTake) {
        if (taken <= 0 || container instanceof AbstractFurnaceBlockEntity) {
            return;
        }
        int pool = getMilli(container);
        int give = FurnaceXpRules.transferMilli(pool, taken, leftAfterTake + taken);
        give = takeMilli(container, give);
        award(player, give);
    }

    public static void award(ServerPlayer player, int milli) {
        if (milli <= 0 || !(player.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        int orbs = FurnaceXpRules.orbsFromMilli(milli, player.getRandom().nextFloat());
        if (orbs > 0) {
            ExperienceOrb.award(level, player.position(), orbs);
        }
    }

    private static ServerLevel levelOf(Container container) {
        if (container instanceof BlockEntity blockEntity && blockEntity.getLevel() instanceof ServerLevel level) {
            return level;
        }
        if (container instanceof Entity entity && entity.level() instanceof ServerLevel level) {
            return level;
        }
        return null;
    }

    private static AttachmentTarget target(Container container) {
        if (container instanceof BlockEntity blockEntity) {
            return blockEntity;
        }
        if (container instanceof Entity entity) {
            return entity;
        }
        return null;
    }
}
