package dev.extrahardmode.feature;

import dev.extrahardmode.tag.EhmTags;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Flood-fill a bed's interior and score amenities. */
public final class ResidenceScan {
    public record Result(
            BlockPos bed,
            int volume,
            boolean enclosed,
            boolean hasBed,
            boolean doorOutside,
            boolean lit,
            boolean roofed,
            InhabitantRules.AmenityCounts amenities,
            InhabitantRules.GateResult gates) {}

    private ResidenceScan() {}

    public static Result inspect(
            ServerLevel level, BlockPos bed, List<BlockPos> occupiedBeds, int minLight, int spacing) {
        LongOpenHashSet interior = new LongOpenHashSet();
        boolean moreQueued = fill(level, bed, interior);
        boolean enclosed = InhabitantRules.enclosed(interior.size(), moreQueued);
        int volume = interior.size();
        BlockPos anchor = canonicalBed(level, bed, interior);
        boolean hasBed = countBeds(level, interior) > 0;
        boolean doorOutside = hasDoorOutside(level, interior);
        boolean lit = floorsLit(level, interior, minLight);
        boolean roofed = hasRoof(level, interior);
        InhabitantRules.AmenityCounts amenities = amenities(level, interior);
        boolean far = farEnough(anchor, occupiedBeds, spacing);
        InhabitantRules.GateResult gates = InhabitantRules.gates(
                enclosed, volume, hasBed, doorOutside, lit, roofed, far, amenities);
        return new Result(anchor, volume, enclosed, hasBed, doorOutside, lit, roofed, amenities, gates);
    }

    static boolean fill(ServerLevel level, BlockPos bed, LongOpenHashSet interior) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        for (BlockPos start : starts(level, bed)) {
            long key = start.asLong();
            if (seen.add(key)) {
                queue.add(start.immutable());
            }
        }
        boolean more = false;
        while (!queue.isEmpty()) {
            if (interior.size() >= InhabitantRules.FILL_CAP) {
                more = true;
                break;
            }
            BlockPos pos = queue.removeFirst();
            if (!walkable(level, pos)) {
                continue;
            }
            interior.add(pos.asLong());
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!level.isLoaded(next)) {
                    continue;
                }
                long key = next.asLong();
                if (seen.add(key)) {
                    queue.add(next);
                }
            }
        }
        return more || !queue.isEmpty();
    }

    static List<BlockPos> starts(ServerLevel level, BlockPos bed) {
        List<BlockPos> starts = new java.util.ArrayList<>();
        LongOpenHashSet added = new LongOpenHashSet();
        addStart(level, bed.above(), starts, added);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos other = bed.relative(dir);
            if (level.getBlockState(other).is(BlockTags.BEDS)) {
                addStart(level, other.above(), starts, added);
            }
            addStart(level, other, starts, added);
            addStart(level, bed.above().relative(dir), starts, added);
        }
        return starts;
    }

    static void addStart(ServerLevel level, BlockPos pos, List<BlockPos> starts, LongOpenHashSet added) {
        if (!walkable(level, pos) || !added.add(pos.asLong())) {
            return;
        }
        starts.add(pos.immutable());
    }

    static boolean walkable(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        boolean opening = state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES);
        return InhabitantRules.interiorCell(
                opening,
                state.is(EhmTags.RESIDENCE_RUGS),
                state.isAir() || state.canBeReplaced(),
                state.getCollisionShape(level, pos).isEmpty());
    }

    static BlockPos canonicalBed(ServerLevel level, BlockPos fallback, LongOpenHashSet interior) {
        BlockPos best = fallback.immutable();
        long bestKey = Long.MAX_VALUE;
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (level.getBlockState(neighbor).is(BlockTags.BEDS) && neighbor.asLong() < bestKey) {
                    best = neighbor.immutable();
                    bestKey = neighbor.asLong();
                }
            }
        }
        if (level.getBlockState(fallback).is(BlockTags.BEDS) && fallback.asLong() <= bestKey) {
            return fallback.immutable();
        }
        return best;
    }

    static int countBeds(ServerLevel level, LongOpenHashSet interior) {
        LongOpenHashSet beds = new LongOpenHashSet();
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            if (level.getBlockState(pos).is(BlockTags.BEDS)) {
                beds.add(pos.asLong());
            }
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (level.getBlockState(neighbor).is(BlockTags.BEDS)) {
                    beds.add(neighbor.asLong());
                }
            }
        }
        return beds.size();
    }

    static boolean hasDoorOutside(ServerLevel level, LongOpenHashSet interior) {
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            BlockState state = level.getBlockState(pos);
            if (isDoorLike(state) && doorFacesOut(interior, pos)) {
                return true;
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                if (isDoorLike(level.getBlockState(neighbor)) && doorFacesOut(interior, neighbor)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isDoorLike(BlockState state) {
        return state.is(BlockTags.DOORS) || state.is(BlockTags.FENCE_GATES);
    }

    static boolean doorFacesOut(LongOpenHashSet interior, BlockPos door) {
        boolean inside = false;
        boolean outside = false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (interior.contains(door.relative(dir).asLong())) {
                inside = true;
            } else {
                outside = true;
            }
        }
        return inside && outside;
    }

    static boolean floorsLit(ServerLevel level, LongOpenHashSet interior, int minLight) {
        int need = Math.max(0, minLight);
        boolean any = false;
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            if (!isFloorAir(level, pos)) {
                continue;
            }
            any = true;
            if (level.getBrightness(LightLayer.BLOCK, pos) < need) {
                return false;
            }
        }
        return any;
    }

    static boolean hasRoof(ServerLevel level, LongOpenHashSet interior) {
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            if (!isFloorAir(level, pos)) {
                continue;
            }
            boolean covered = false;
            for (int up = 1; up <= 8; up++) {
                BlockPos above = pos.above(up);
                BlockState state = level.getBlockState(above);
                if (state.is(EhmTags.RESIDENCE_WINDOWS) || state.isSolid()) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    static boolean isFloorAir(ServerLevel level, BlockPos pos) {
        return walkable(level, pos) && level.getBlockState(pos.below()).isSolid();
    }

    static InhabitantRules.AmenityCounts amenities(ServerLevel level, LongOpenHashSet interior) {
        int windows = 0;
        int carpets = 0;
        int seating = 0;
        int storage = 0;
        int workstations = 0;
        int extraLights = 0;
        int plants = 0;
        int books = 0;
        int extraBeds = Math.max(0, countBeds(level, interior) - 1);
        boolean heat = false;
        boolean water = false;
        LongOpenHashSet counted = new LongOpenHashSet();
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            BlockState state = level.getBlockState(pos);
            if (state.is(EhmTags.RESIDENCE_RUGS)) {
                carpets++;
            }
            if (state.is(EhmTags.RESIDENCE_SEATING)) {
                seating++;
            }
            if (state.is(EhmTags.RESIDENCE_LIGHTS)) {
                extraLights++;
            }
            if (state.is(EhmTags.RESIDENCE_PLANTS)) {
                plants++;
            }
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                long nkey = neighbor.asLong();
                if (interior.contains(nkey) || !counted.add(nkey)) {
                    continue;
                }
                BlockState wall = level.getBlockState(neighbor);
                if (wall.is(EhmTags.RESIDENCE_WINDOWS)
                        && !interior.contains(neighbor.relative(dir).asLong())) {
                    windows++;
                }
                if (wall.is(EhmTags.RESIDENCE_STORAGE)) {
                    storage++;
                }
                if (wall.is(EhmTags.RESIDENCE_WORKSTATIONS)) {
                    workstations++;
                }
                if (wall.is(EhmTags.RESIDENCE_BOOKS)) {
                    books++;
                }
                if (wall.is(EhmTags.RESIDENCE_LIGHTS)) {
                    extraLights++;
                }
                if (wall.is(EhmTags.RESIDENCE_PLANTS)) {
                    plants++;
                }
                if (wall.is(EhmTags.RESIDENCE_SEATING)) {
                    seating++;
                }
                if (wall.is(EhmTags.RESIDENCE_KITCHEN_HEAT)) {
                    heat = true;
                }
                if (wall.is(EhmTags.RESIDENCE_KITCHEN_WATER)) {
                    water = true;
                }
            }
        }
        int art = artCount(level, interior);
        return new InhabitantRules.AmenityCounts(
                windows, art, carpets, seating, storage, workstations, extraLights, plants, books, extraBeds, heat && water);
    }

    static int artCount(ServerLevel level, LongOpenHashSet interior) {
        if (interior.isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (long key : interior) {
            BlockPos pos = BlockPos.of(key);
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        AABB box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(1.0);
        int art = 0;
        for (Painting painting : level.getEntitiesOfClass(Painting.class, box)) {
            if (interior.contains(BlockPos.containing(painting.position()).asLong())
                    || nearInterior(interior, painting.blockPosition())) {
                art++;
            }
        }
        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, box)) {
            if (frame.getItem().isEmpty()) {
                continue;
            }
            if (interior.contains(BlockPos.containing(frame.position()).asLong())
                    || nearInterior(interior, frame.blockPosition())) {
                art++;
            }
        }
        return art;
    }

    static boolean nearInterior(LongOpenHashSet interior, BlockPos pos) {
        if (interior.contains(pos.asLong())) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            if (interior.contains(pos.relative(dir).asLong())) {
                return true;
            }
        }
        return false;
    }

    static boolean farEnough(BlockPos bed, List<BlockPos> occupied, int spacing) {
        if (occupied == null || occupied.isEmpty()) {
            return true;
        }
        for (BlockPos other : occupied) {
            if (other.equals(bed)) {
                continue;
            }
            int dx = bed.getX() - other.getX();
            int dz = bed.getZ() - other.getZ();
            if (!InhabitantRules.farEnough(dx, dz, spacing)) {
                return false;
            }
        }
        return true;
    }

    public static String homeId(BlockPos bed) {
        return Long.toString(bed.asLong());
    }

    /**
     * Feet block a villager can occupy without clipping a 2-high ceiling.
     * Prefers floor cells beside the bed; {@code bed.above()} is last.
     */
    public static BlockPos standableNear(ServerLevel level, BlockPos bed) {
        for (int[] offset : InhabitantRules.STAND_OFFSETS) {
            BlockPos feet = bed.offset(offset[0], offset[1], offset[2]);
            if (villagerCanStand(level, feet)) {
                return feet.immutable();
            }
        }
        LongOpenHashSet interior = new LongOpenHashSet();
        fill(level, bed, interior);
        BlockPos fallback = null;
        int best = Integer.MAX_VALUE;
        for (long key : interior) {
            BlockPos feet = BlockPos.of(key);
            if (!villagerCanStand(level, feet)) {
                continue;
            }
            int dx = feet.getX() - bed.getX();
            int dy = feet.getY() - bed.getY();
            int dz = feet.getZ() - bed.getZ();
            int dist = dx * dx + dy * dy + dz * dz;
            if (dist < best) {
                best = dist;
                fallback = feet.immutable();
            }
        }
        return fallback;
    }

    static boolean villagerCanStand(ServerLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        if (!level.isLoaded(feet) || !level.isLoaded(head) || !level.isLoaded(feet.below())) {
            return false;
        }
        return InhabitantRules.villagerFits(
                walkable(level, feet), walkable(level, head), level.getBlockState(feet.below()).isSolid());
    }
}
