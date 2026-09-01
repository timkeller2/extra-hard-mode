package dev.extrahardmode.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Placement predicates shared by server callbacks and the client place-cancel mixin.
 * Client mixins must not read TOML; pass payload/tag results in.
 */
public final class PlacementRules {
    private PlacementRules() {}

    /** Disable with the enable boolean, never Y=0 / world-min as a sentinel. */
    public static boolean denyTorchY(boolean enable, int noPlacementUnderY, int placeY) {
        return enable && placeY < noPlacementUnderY;
    }

    public static boolean denyTorchSoft(boolean enable, boolean clickedIsSoft) {
        return enable && clickedIsSoft;
    }

    /**
     * Jump-place under feet only: {@code place} equals {@code player.blockPosition().below()}
     * while airborne. Does not deny the whole column.
     */
    public static boolean denyPillar(boolean enable, boolean onGround, BlockPos place, BlockPos underFeet) {
        return enable && !onGround && place.equals(underFeet);
    }

    /**
     * Unsupported sky bridge: no solid support within 1 below, and no solid neighbor
     * except the clicked face / the block the player is standing on.
     */
    public static boolean denySkyBridge(
            boolean enable,
            BlockGetter level,
            BlockPos place,
            BlockPos clicked,
            BlockPos standingOn) {
        if (!enable) {
            return false;
        }
        if (hasSupportWithinOneBelow(level, place)) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = place.relative(direction);
            if (neighbor.equals(clicked) || neighbor.equals(standingOn)) {
                continue;
            }
            if (isSolidSupport(level, neighbor)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSupportWithinOneBelow(BlockGetter level, BlockPos place) {
        BlockPos below = place.below();
        if (isSolidSupport(level, below)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isSolidSupport(level, below.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSolidSupport(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && (state.isSolid() || state.blocksMotion());
    }

    /** The block the player clicked, not the placement cell. */
    public static BlockPos againstBlock(BlockPlaceContext context) {
        if (context.replacingClickedOnBlock()) {
            return context.getClickedPos();
        }
        return context.getClickedPos().relative(context.getClickedFace().getOpposite());
    }
}
