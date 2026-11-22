package dev.extrahardmode.feature;

import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;

/**
 * Looking at a clock in an item frame shows the lines a used clock shows.
 * The same frame is reported once until the crosshair leaves it.
 */
public final class ClockFrames {
    private ClockFrames() {}

    public static void tick(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = Inhabitants.entityOnCrosshair(player);
        String id = framedClockId(target);
        String last = player.getAttachedOrElse(EhmAttachments.EHM_CLOCK_FRAME_LOOK, "");
        if (id.equals(last)) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_CLOCK_FRAME_LOOK, id);
        if (!shouldTell(id, last) || !(target instanceof ItemFrame frame)) {
            return;
        }
        tell(player, level, frame);
    }

    /** A new framed clock, not the one already reported. */
    static boolean shouldTell(String lookedId, String lastId) {
        return lookedId != null && !lookedId.isEmpty() && !lookedId.equals(lastId);
    }

    static String framedClockId(Entity entity) {
        if (entity instanceof ItemFrame frame && frame.getItem().is(Items.CLOCK)) {
            return frame.getUUID().toString();
        }
        return "";
    }

    static void tell(ServerPlayer player, ServerLevel level, ItemFrame frame) {
        if (WorldGate.isModuleActive(level, AntiFarming.ID)) {
            AntiFarming.tellCropLoss(player, level);
        }
        if (WorldGate.isModuleActive(level, Inhabitants.ID)) {
            BlockPos bed = ResidenceScan.bedOfHouseContaining(level, frame.blockPosition());
            if (bed == null) {
                bed = ResidenceScan.bedOfHouseContaining(level, player.blockPosition());
            }
            if (bed != null) {
                Inhabitants.tellHouseAt(player, level, bed);
            }
        }
        if (WorldGate.isModuleActive(level, AntiFarming.ID)) {
            AntiFarming.tellHiveHoney(player, level, supportBlock(frame));
        }
    }

    /** The block the frame hangs on. */
    static BlockPos supportBlock(ItemFrame frame) {
        Direction facing = frame.getDirection();
        if (facing == null) {
            return null;
        }
        return frame.blockPosition().relative(facing.getOpposite());
    }
}
