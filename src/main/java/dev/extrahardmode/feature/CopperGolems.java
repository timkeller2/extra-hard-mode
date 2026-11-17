package dev.extrahardmode.feature;

import dev.extrahardmode.mixin.CompoundContainerAccess;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * A copper golem remembers the player who summoned it. That golem can take from and
 * put into a copper chest that player placed. Other copper golems leave that chest
 * alone. A chest nobody owns stays open to every golem.
 */
public final class CopperGolems {
    /** The player whose block placement is spawning a golem, if that placement is a player's. */
    private static final ThreadLocal<ServerPlayer> PLACING = new ThreadLocal<>();
    /** The golem currently choosing a container. */
    private static final ThreadLocal<Entity> SEARCH = new ThreadLocal<>();

    private CopperGolems() {}

    public static void beginPlace(BlockPlaceContext context) {
        PLACING.remove();
        if (context != null && context.getPlayer() instanceof ServerPlayer player && !player.isSpectator()) {
            PLACING.set(player);
        }
    }

    public static void endPlace() {
        PLACING.remove();
    }

    public static void beginSearch(Entity entity) {
        SEARCH.set(entity);
    }

    public static void endSearch() {
        SEARCH.remove();
    }

    /** Marks a golem spawned while a player is placing the pumpkin. */
    public static void stamp(Entity entity) {
        stampSummoned(entity, PLACING.get());
    }

    /** Marks a copper golem a player summoned. A dispenser or command summon stays unmarked. */
    public static void stampSummoned(Entity entity, LivingEntity user) {
        if (!(entity instanceof CopperGolem golem) || !(user instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        golem.setAttached(EhmAttachments.EHM_COPPER_GOLEM_SUMMONER, player.getUUID());
    }

    /** The copper block in the build pattern becomes this player's copper chest. */
    public static void claimSummonChest(Level level, BlockPattern.BlockPatternMatch match) {
        ServerPlayer player = PLACING.get();
        if (player == null || !(level instanceof ServerLevel server) || match == null) {
            return;
        }
        BlockPos pos = match.getBlock(0, 1, 0).getPos();
        CopperChests.claim(server, pos, player);
    }

    /** True when the golem that is searching must not target this position. */
    public static boolean refuses(Level level, BlockPos pos) {
        return refusesWork(SEARCH.get(), level, pos);
    }

    /** True when this golem must not take from or put into the container. */
    public static boolean refusesContainer(Entity entity, Container container) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            return refusesContainer(entity, access.tougher$container1())
                    || refusesContainer(entity, access.tougher$container2());
        }
        if (container instanceof BlockEntity block) {
            return refusesWork(entity, block.getLevel(), block.getBlockPos());
        }
        return false;
    }

    /**
     * True when this position is a locked copper chest this golem did not get from its
     * summoner. A public chest, and a chest that summoner placed, are allowed. A double
     * chest is refused when either half belongs to someone else.
     */
    public static boolean refusesWork(Entity entity, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || pos == null || !WorldGate.isModuleActive(server, CopperChests.ID)) {
            return false;
        }
        String summoner = summonerId(entity);
        return !CopperGolemRules.mayUse(summoner, CopperChests.ownerId(server, pos))
                || !CopperGolemRules.mayUse(summoner, otherOwner(server, pos));
    }

    private static String summonerId(Entity entity) {
        if (!(entity instanceof CopperGolem golem)) {
            return null;
        }
        UUID id = golem.getAttached(EhmAttachments.EHM_COPPER_GOLEM_SUMMONER);
        return id == null ? null : id.toString();
    }

    /** Owner of the other half, or null when this chest is single or that half is public. */
    private static String otherOwner(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return null;
        }
        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return CopperChests.ownerId(level, ChestBlock.getConnectedBlockPos(pos, state));
    }
}
