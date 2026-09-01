package dev.extrahardmode.test;

import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.api.event.EhmExplosionEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.RealisticChopping;
import dev.extrahardmode.feature.MoreTnt;
import dev.extrahardmode.feature.monster.Zombies;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.feature.monster.Blazes;
import dev.extrahardmode.feature.monster.PigMen;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.feature.monster.Silverfish;
import dev.extrahardmode.feature.monster.Skeletons;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.PhysicsSkip;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.NetherrackFire;
import dev.extrahardmode.feature.Torches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dev.extrahardmode.feature.Water;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EhmGameTests {
    private static final ThreadLocal<Boolean> CANCEL_NEXT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    static {
        EhmExplosionEvent.EVENT.register(event -> {
            if (Boolean.TRUE.equals(CANCEL_NEXT.get())) {
                event.cancel();
            }
        });
    }

    @GameTest
    public void firstApplyPerDimension(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        boolean toml = ConfigManager.global().enabledByDefault();
        helper.assertValueEqual(
                toml, overworld.getGameRules().get(WorldGate.ENABLED), "overworld gamerule matches TOML");

        WorldGate.applyIfNeeded(server, overworld);
        ExtraHardModeBootData boot = WorldGate.bootData(server);
        if (boot == null) {
            helper.fail("boot SavedData exists");
            return;
        }
        helper.assertTrue(boot.contains(Level.OVERWORLD.identifier()), "boot contains overworld");
        boolean netherAlreadyApplied = boot.contains(Level.NETHER.identifier());
        if (!netherAlreadyApplied) {
            helper.assertFalse(
                    boot.contains(Level.NETHER.identifier()), "boot contains overworld does not imply nether");
        }

        boolean flipped = !overworld.getGameRules().get(WorldGate.ENABLED);
        overworld.getGameRules().set(WorldGate.ENABLED, flipped, server);

        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension() == Level.NETHER) {
                    nether = level;
                    break;
                }
            }
        }
        if (nether == null) {
            helper.fail("nether not loaded");
            return;
        }

        WorldGate.applyIfNeeded(server, nether);
        helper.assertValueEqual(
                flipped,
                overworld.getGameRules().get(WorldGate.ENABLED),
                "gamerule unchanged after nether first-apply");
        helper.assertTrue(boot.contains(Level.NETHER.identifier()), "nether stamped independently");
        helper.assertTrue(boot.contains(Level.OVERWORLD.identifier()), "overworld still stamped");
        helper.assertTrue(WorldGate.dimensionEnabled(nether), "nether dim flag defaults true (opt-out)");
        helper.succeed();
    }

    @GameTest
    public void woodenPickCannotHarvestHardened(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        helper.assertTrue(
                player.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 0.0F, "wooden pick destroy speed 0 on stone");
        helper.assertFalse(player.hasCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "wooden pick cannot harvest stone");
        helper.assertTrue(
                player.getDestroySpeed(Blocks.TUFF.defaultBlockState()) == 0.0F, "wooden pick destroy speed 0 on tuff");
        helper.succeed();
    }

    @GameTest
    public void ironPickBreaksAfter128Hardened(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(mock instanceof ServerPlayer player)) {
            helper.fail("mock server player");
            return;
        }
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (int i = 0; i < 127; i++) {
            HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
            helper.assertFalse(pick.isEmpty(), "iron pick survives break " + (i + 1));
        }
        helper.assertValueEqual(127, pick.getOrDefault(EhmComponents.HARDENED_MINED, 0), "127 hardened breaks counted");
        HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
        helper.assertTrue(pick.isEmpty(), "iron pick consumed on 128th hardened break");
    public void torchDenyBelowY0(GameTestHelper helper) {
        ConfigManager.world(level).setEnabled(true);
        BlockPos abs = helper.absolutePos(BlockPos.ZERO);
        BlockPos stone = new BlockPos(abs.getX(), -2, abs.getZ());
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(stone.above(), Blocks.AIR.defaultBlockState(), 3);
        if (!(helper.makeMockServerPlayer(GameType.SURVIVAL) instanceof ServerPlayer player)) {
            helper.fail("expected ServerPlayer");
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false);
        ItemStack torch = new ItemStack(Items.TORCH);
        BlockPlaceContext torchContext =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, torch, hit));
                torch.getItem() instanceof BlockItem blockItem
                        && Torches.shouldDeny(level, torchContext, blockItem),
                "torch denied below Y=0");
        ItemStack redstone = new ItemStack(Items.REDSTONE_TORCH);
        BlockPlaceContext redstoneContext =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, redstone, hit));
        helper.assertFalse(
                redstone.getItem() instanceof BlockItem redstoneItem
                        && Torches.shouldDeny(level, redstoneContext, redstoneItem),
                "redstone torch allowed below Y=0");
        BlockPos stoneAtMinus1 = new BlockPos(abs.getX(), -1, abs.getZ());
        level.setBlock(stoneAtMinus1, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(stoneAtMinus1.above(), Blocks.AIR.defaultBlockState(), 3);
        BlockHitResult hitAtY0 =
                new BlockHitResult(Vec3.atCenterOf(stoneAtMinus1).add(0, 0.5, 0), Direction.UP, stoneAtMinus1, false);
        BlockPlaceContext y0Context =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, torch, hitAtY0));
                torch.getItem() instanceof BlockItem y0Item && Torches.shouldDeny(level, y0Context, y0Item),
                "torch allowed at Y=0");
        WorldConfig config = ConfigManager.world(level);
        boolean previousYDeny = config.torchYDeny();
        config.setTorchYDeny(false);
                torch.getItem() instanceof BlockItem disabledItem
                        && Torches.shouldDeny(level, torchContext, disabledItem),
                "torch Y deny disabled with enable boolean");
        config.setTorchYDeny(previousYDeny);
        helper.succeed();
    }

    @GameTest
    public void ironPickWithUnbreakingBreaksAfter128(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(mock instanceof ServerPlayer player)) {
            helper.fail("mock server player");
            return;
        }
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        var unbreaking = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        pick.enchant(unbreaking, 3);
        helper.assertTrue(pick.isEnchanted(), "Unbreaking III applied");
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (int i = 0; i < 127; i++) {
            HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
            helper.assertFalse(pick.isEmpty(), "Unbreaking iron pick survives break " + (i + 1));
        }
        HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
        helper.assertTrue(pick.isEmpty(), "Unbreaking III does not extend N; 128th break consumes pick");
    public void netherrackFireOnNetherrackBelow(GameTestHelper helper) {
        WorldConfig config = ConfigManager.world(level);
        config.setEnabled(true);
        int previous = config.netherrackFirePercent();
        config.setNetherrackFirePercent(100);

        helper.setBlock(new BlockPos(1, 1, 1), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        BlockPos emptyOnNetherrack = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(
                NetherrackFire.tryIgnite(level, null, emptyOnNetherrack), "fire in empty cell above remaining netherrack");
        helper.assertBlockPresent(Blocks.FIRE, new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.AIR);
        helper.assertFalse(
                NetherrackFire.tryIgnite(level, null, helper.absolutePos(new BlockPos(3, 2, 1))),
                "no fire when block below is not netherrack");
        helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(3, 2, 1));
        config.setNetherrackFirePercent(previous);
        helper.succeed();
    public void waterFromBucketIsNotSource(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, Blocks.AIR);
        BlockPos abs = helper.absolutePos(rel);
        BucketItem bucket = (BucketItem) Items.WATER_BUCKET;
        helper.assertTrue(bucket.emptyContents(null, level, abs, null), "emptied water bucket");
        FluidState immediately = level.getFluidState(abs);
        helper.assertTrue(immediately.is(FluidTags.WATER), "bucket placed water");
        helper.assertFalse(immediately.isSource(), "water from bucket is not a source");
        helper.assertValueEqual(
                1, helper.getBlockState(rel).getValue(LiquidBlock.LEVEL), "first write is block LEVEL=1 not 7");
        helper.assertTrue(Water.isMarked(level, abs), "placed water is marked non-source");
        BlockPos slabRel = new BlockPos(2, 2, 1);
        helper.setBlock(slabRel, Blocks.STONE_SLAB.defaultBlockState());
                bucket.emptyContents(null, level, helper.absolutePos(slabRel), null), "emptied onto slab");
        BlockState slab = helper.getBlockState(slabRel);
        helper.assertTrue(slab.getBlock() == Blocks.STONE_SLAB, "slab was not replaced with water");
        helper.assertTrue(slab.getValue(BlockStateProperties.WATERLOGGED), "slab stays waterlogged");
        helper.runAfterDelay(2, () -> {
            FluidState later = level.getFluidState(abs);
            helper.assertFalse(later.isSource(), "still not a source after ticks");
            helper.assertValueEqual(1, helper.getBlockState(rel).getValue(LiquidBlock.LEVEL), "still LEVEL=1");
            helper.succeed();
        });
    public void spawnProcessedSurvivesChunkReload(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, pos, EntitySpawnReason.COMMAND);
        zombie.setPersistenceRequired();
        zombie.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        UUID id = zombie.getUUID();
        ChunkPos chunkPos = ChunkPos.containing(zombie.blockPosition());
        level.getChunkSource().save(true);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        if (!zombie.save(output)) {
            helper.fail("processed zombie did not save");
        CompoundTag tag = output.buildResult();
        zombie.discard();
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        Entity loaded = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.LOAD, entity -> {
            if (!level.addFreshEntity(entity)) {
                return null;
            }
            return entity;
        if (!(loaded instanceof Zombie reloaded)) {
            helper.fail("reloaded entity was not a zombie");
        helper.assertValueEqual(id, reloaded.getUUID(), "uuid");
                Boolean.TRUE.equals(reloaded.getAttached(EhmAttachments.EHM_SPAWN_PROCESSED)),
                "spawn_processed persisted across chunk reload");
                level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) != null, "chunk reloaded");
        SpawnReplaceService.replaceIfNeeded(reloaded, level, EntitySpawnReason.NATURAL);
        helper.assertTrue(reloaded.getType() == EntityTypes.ZOMBIE && !reloaded.isRemoved(), "not replaced again");
        helper.assertEntityNotPresent(EntityTypes.WITCH);
    public void spawnReplaceSkippedWhenWorldGateInactive(GameTestHelper helper) {
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, false, server);
        try {
            Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.NATURAL);
            SpawnReplaceService.replaceIfNeeded(zombie, level, EntitySpawnReason.NATURAL);
            helper.assertFalse(
                    Boolean.TRUE.equals(
                            zombie.getAttachedOrElse(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.FALSE)),
                    "spawn_processed not stamped when inactive");
            helper.assertTrue(zombie.getType() == EntityTypes.ZOMBIE, "zombie not replaced when inactive");
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
    }

    @GameTest
    public void coalOreCaveIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos ore = new BlockPos(2, 3, 2);
        helper.setBlock(ore, Blocks.COAL_ORE);
        for (Direction direction : Direction.values()) {
            helper.setBlock(ore.relative(direction), Blocks.STONE);
        }
        BlockPos abs = helper.absolutePos(ore);
        level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        CaveIns.onOreBroken(level, abs, Blocks.COAL_ORE.defaultBlockState());
        PhysicsQueue.tick(level);
        helper.succeedWhen(() -> {
            helper.assertTrue(caveInProducedCobble(helper, ore), "coal-ore cave-in softened stone to cobble");
        });
    }

    @GameTest
    public void deepDarkSkipsCaveIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        helper.setBiome(Biomes.DEEP_DARK);
        BlockPos ore = new BlockPos(2, 3, 2);
        helper.setBlock(ore, Blocks.COAL_ORE);
        for (Direction direction : Direction.values()) {
            helper.setBlock(ore.relative(direction), Blocks.STONE);
        }
        BlockPos abs = helper.absolutePos(ore);
        level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        CaveIns.onOreBroken(level, abs, Blocks.COAL_ORE.defaultBlockState());
        PhysicsQueue.tick(level);
        helper.runAfterDelay(5, () -> {
            for (Direction direction : Direction.values()) {
                helper.assertBlockPresent(Blocks.STONE, ore.relative(direction));
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 200, padding = 8)
    public void copperBlobNoTickTimeout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        int droppedBefore = PhysicsQueue.of(level).dropped();
        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                BlockPos rel = new BlockPos(x, 2, z);
                helper.setBlock(rel, Blocks.COPPER_ORE);
                helper.setBlock(rel.above(), Blocks.STONE);
                helper.setBlock(rel.below(), Blocks.STONE);
            }
        }
        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                BlockPos rel = new BlockPos(x, 2, z);
                BlockPos abs = helper.absolutePos(rel);
                level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
                CaveIns.onOreBroken(level, abs, Blocks.COPPER_ORE.defaultBlockState());
            }
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(PhysicsQueue.of(level).queueDepth() == 0, "copper blob physics queue drained");
            helper.assertTrue(
                    PhysicsQueue.of(level).dropped() == droppedBefore,
                    "16-block copper blob must not overflow physics queue");
        });
    }

    private static boolean caveInProducedCobble(GameTestHelper helper, BlockPos ore) {
        for (Direction direction : Direction.values()) {
            if (helper.getBlockState(ore.relative(direction)).is(Blocks.COBBLESTONE)) {
                return true;
            }
        }
        ServerLevel level = helper.getLevel();
        AABB box = new AABB(helper.absolutePos(ore)).inflate(3.0);
        for (FallingBlockEntity falling : level.getEntities(EntityTypes.FALLING_BLOCK, box, entity -> true)) {
            if (falling.getBlockState().is(Blocks.COBBLESTONE)) {
                return true;
            }
        }
        return false;
    }

    @GameTest
    public void convertDoesNotOverwriteUnrelatedBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos rel = new BlockPos(2, 2, 2);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);
        PhysicsQueue.of(level)
                .enqueueConvert(
                        level,
                        abs,
                        Blocks.STONE.defaultBlockState(),
                        Blocks.COBBLESTONE.defaultBlockState(),
                        true);
        helper.setBlock(rel, Blocks.CHEST);
        PhysicsQueue.tick(level);
        helper.assertBlockPresent(Blocks.CHEST, rel);
        helper.succeed();
    }

    @GameTest
    public void fallingDamageGatedToEhmTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos rel = new BlockPos(2, 4, 2);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);

        FallingBlockEntity cobble = FallingBlockEntity.fall(level, abs, Blocks.COBBLESTONE.defaultBlockState());
        cobble.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        helper.assertTrue(FallingBlocks.appliesFallDamage(cobble), "EHM cobble is gated in");

        helper.setBlock(rel, Blocks.STONE);
        FallingBlockEntity sand = FallingBlockEntity.fall(level, abs, Blocks.SAND.defaultBlockState());
        helper.assertFalse(FallingBlocks.appliesFallDamage(sand), "sand/gravel stay vanilla (no extra)");

        helper.setBlock(rel, Blocks.STONE);
        FallingBlockEntity anvil = FallingBlockEntity.fall(level, abs, Blocks.ANVIL.defaultBlockState());
        helper.assertFalse(FallingBlocks.appliesFallDamage(anvil), "anvil stays vanilla");
        helper.assertTrue(FallingBlocks.isVanillaFallDamage(anvil.getBlockState()), "anvil is vanilla fall-damage");
        helper.succeed();
    }

    @GameTest
    public void fallingCobbleDealsDamageAfterRealFall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(mock instanceof ServerPlayer player)) {
            helper.fail("mock server player");
            return;
        }
        BlockPos rel = new BlockPos(2, 3, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        FallingBlockEntity cobble = FallingBlockEntity.fall(level, abs.above(4), Blocks.COBBLESTONE.defaultBlockState());
        cobble.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        cobble.setHurtsEntities(2.0F, 2);
        cobble.snapTo(player.getX(), player.getY(), player.getZ());
        float before = player.getHealth();
        cobble.causeFallDamage(4.0, 1.0F, cobble.damageSources().fallingBlock(cobble));
        helper.assertTrue(player.getHealth() <= before - 2.0F + 0.001F, "real cobble fall deals 2");
        helper.succeed();
    }

    @GameTest
    public void physicsProtectedStructuresTagContainsAncientCity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var tag = structures.get(EhmTags.PHYSICS_PROTECTED_STRUCTURES);
        helper.assertTrue(tag.isPresent(), "#physics_protected_structures loaded");
        boolean ancient = tag.get().stream().anyMatch(holder -> holder.is(BuiltinStructures.ANCIENT_CITY));
        boolean trials = tag.get().stream().anyMatch(holder -> holder.is(BuiltinStructures.TRIAL_CHAMBERS));
        helper.assertTrue(ancient, "tag includes minecraft:ancient_city");
        helper.assertTrue(trials, "tag includes minecraft:trial_chambers");
        BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertFalse(
                PhysicsSkip.never(level, abs) && !level.getBiome(abs).is(EhmTags.NO_PHYSICS),
                "empty test platform is not a protected structure piece");
        helper.succeed();
    }

    @GameTest
    public void skeletonSpecialShooterFilter(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.assertTrue(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.SKELETON, pos, EntitySpawnReason.COMMAND)),
                "skeleton is special shooter");
        helper.assertTrue(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.BOGGED, pos, EntitySpawnReason.COMMAND)),
                "bogged shares skeleton table");
        helper.assertFalse(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.STRAY, pos, EntitySpawnReason.COMMAND)),
                "stray excluded");
        helper.assertFalse(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.WITHER_SKELETON, pos, EntitySpawnReason.COMMAND)),
                "wither skeleton excluded");
        helper.assertFalse(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.PARCHED, pos, EntitySpawnReason.COMMAND)),
                "parched excluded");
        helper.succeed();
    }

    @GameTest
    public void silverfishDropsCobble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, true, server);
        try {
            BlockPos pos = new BlockPos(1, 2, 1);
            var fish = helper.spawn(EntityTypes.SILVERFISH, pos, EntitySpawnReason.COMMAND);
            helper.kill(fish);
            helper.succeedWhen(() -> helper.assertItemEntityPresent(net.minecraft.world.item.Items.COBBLESTONE, pos, 3.0));
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
        }
    }

    @GameTest
    public void silverfishDoesNotEnterStoneWhenActive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, true, server);
        try {
            helper.assertTrue(Silverfish.cantEnterBlocks(level), "cantEnterBlocks when WorldGate on");
            var fish = helper.spawn(EntityTypes.SILVERFISH, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
            BlockPos hostPos = hostBeside(fish, Direction.NORTH);
            level.setBlock(hostPos, Blocks.STONE.defaultBlockState(), 3);
            invokeMergeStart(fish, Direction.NORTH);
            helper.assertTrue(level.getBlockState(hostPos).is(Blocks.STONE), "host not infested when active");
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            helper.fail("merge goal reflect: " + e.getMessage());
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
    public void killerBunnyReplacesWhenPercent100(GameTestHelper helper) {
        boolean previousRule = level.getGameRules().get(WorldGate.ENABLED);
        WorldConfig config = ConfigManager.world(level);
        int previousPercent = config.killerBunnyPercent();
        config.setKillerBunnyPercent(100);
            Rabbit rabbit = helper.spawn(EntityTypes.RABBIT, new BlockPos(1, 2, 1), EntitySpawnReason.NATURAL);
            SpawnReplaceService.replaceIfNeeded(rabbit, level, EntitySpawnReason.NATURAL);
            helper.assertTrue(rabbit.getVariant() == Rabbit.Variant.EVIL, "killer bunny variant");
            helper.assertFalse(rabbit.isBaby(), "adult");
            config.setKillerBunnyPercent(previousPercent);
            level.getGameRules().set(WorldGate.ENABLED, previousRule, server);
        }
    }

    @GameTest
    public void silverfishEntersStoneWhenWorldGateOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, false, server);
        try {
            helper.assertFalse(Silverfish.cantEnterBlocks(level), "cantEnterBlocks off when WorldGate off");
            var fish = helper.spawn(EntityTypes.SILVERFISH, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
            BlockPos hostPos = hostBeside(fish, Direction.NORTH);
            level.setBlock(hostPos, Blocks.STONE.defaultBlockState(), 3);
            invokeMergeStart(fish, Direction.NORTH);
            helper.assertTrue(
                    level.getBlockState(hostPos).is(Blocks.INFESTED_STONE), "vanilla infest when WorldGate off");
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            helper.fail("merge goal reflect: " + e.getMessage());
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
        }
    }

    private static BlockPos hostBeside(net.minecraft.world.entity.monster.Silverfish fish, Direction direction) {
        return BlockPos.containing(fish.getX(), fish.getY() + 0.5, fish.getZ()).relative(direction);
    }

    private static void invokeMergeStart(net.minecraft.world.entity.monster.Silverfish fish, Direction direction)
            throws ReflectiveOperationException {
        Class<?> goalClass = Class.forName("net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal");
        var ctor = goalClass.getDeclaredConstructor(net.minecraft.world.entity.monster.Silverfish.class);
        ctor.setAccessible(true);
        Object goal = ctor.newInstance(fish);
        var doMerge = goalClass.getDeclaredField("doMerge");
        doMerge.setAccessible(true);
        doMerge.setBoolean(goal, true);
        var selected = goalClass.getDeclaredField("selectedDirection");
        selected.setAccessible(true);
        selected.set(goal, direction);
        goalClass.getMethod("start").invoke(goal);
    }
    public void killerBunnySkippedWhenPercentZero(GameTestHelper helper) {
        boolean previousRule = level.getGameRules().get(WorldGate.ENABLED);
        WorldConfig config = ConfigManager.world(level);
        int previousPercent = config.killerBunnyPercent();
        level.getGameRules().set(WorldGate.ENABLED, true, server);
        config.setKillerBunnyPercent(0);
            Rabbit rabbit = helper.spawn(EntityTypes.RABBIT, new BlockPos(1, 2, 1), EntitySpawnReason.NATURAL);
            Rabbit.Variant before = rabbit.getVariant();
            SpawnReplaceService.replaceIfNeeded(rabbit, level, EntitySpawnReason.NATURAL);
            helper.assertTrue(rabbit.getVariant() == before, "percent 0 keeps rabbit variant");
            helper.assertTrue(rabbit.getType() == EntityTypes.RABBIT && !rabbit.isRemoved(), "rabbit kept");
            config.setKillerBunnyPercent(previousPercent);
            level.getGameRules().set(WorldGate.ENABLED, previousRule, server);
    @GameTest(maxTicks = 40, padding = 16)
    public void oakTreeFalls(GameTestHelper helper) {
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(base.above(y - 1), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(2, 5, 1), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(2, 5, 3), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(1, 5, 2), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(3, 5, 2), Blocks.OAK_LEAVES);
        BlockPos[] remaining = remainingColumn(base, 5);
        breakAndFell(helper, mock, base, Blocks.OAK_LOG.defaultBlockState());
        assertNoPlacedLogs(helper, Blocks.OAK_LOG, remaining);
        helper.runAfterDelay(2, () -> {
            assertNoPlacedLogs(helper, Blocks.OAK_LOG, base.above(4));
        });
    public void jungleTwoByTwoFalls(GameTestHelper helper) {
        BlockPos[] columns = {
            new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(2, 1, 3), new BlockPos(3, 1, 3)
        };
        for (BlockPos column : columns) {
            for (int y = 0; y < 6; y++) {
                helper.setBlock(column.above(y), Blocks.JUNGLE_LOG);
            }
        helper.setBlock(new BlockPos(2, 6, 1), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(3, 6, 1), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(1, 6, 2), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(1, 6, 3), Blocks.JUNGLE_LEAVES);
        BlockPos[] remaining = remainingTwoByTwo(columns[0], columns);
        BlockPos[] crown = {columns[0].above(5), columns[1].above(5), columns[2].above(5), columns[3].above(5)};
        breakAndFell(helper, mock, columns[0], Blocks.JUNGLE_LOG.defaultBlockState());
        assertNoPlacedLogs(helper, Blocks.JUNGLE_LOG, remaining);
            assertNoPlacedLogs(helper, Blocks.JUNGLE_LOG, crown);
    public void acaciaBendFalls(GameTestHelper helper) {
        helper.setBlock(base, Blocks.ACACIA_LOG);
        helper.setBlock(base.above(), Blocks.ACACIA_LOG);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.ACACIA_LOG);
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.ACACIA_LOG);
        helper.setBlock(new BlockPos(4, 2, 1), Blocks.ACACIA_LEAVES);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.ACACIA_LEAVES);
        helper.setBlock(new BlockPos(4, 3, 2), Blocks.ACACIA_LEAVES);
        helper.setBlock(new BlockPos(5, 2, 2), Blocks.ACACIA_LEAVES);
        BlockPos[] remaining = {base.above(), new BlockPos(3, 2, 2), new BlockPos(4, 2, 2)};
        breakAndFell(helper, mock, base, Blocks.ACACIA_LOG.defaultBlockState());
        assertNoPlacedLogs(helper, Blocks.ACACIA_LOG, remaining);
            assertNoPlacedLogs(helper, Blocks.ACACIA_LOG, remaining);
    public void logPillarWithNearbyLeafDoesNotFell(GameTestHelper helper) {
        for (int y = 1; y <= 8; y++) {
        helper.setBlock(new BlockPos(4, 4, 2), Blocks.OAK_LEAVES);
        helper.runAfterDelay(8, () -> {
            for (int y = 2; y <= 8; y++) {
                helper.assertBlockPresent(Blocks.OAK_LOG, base.above(y - 1));
    public void threeAdjacentLeavesDoNotFell(GameTestHelper helper) {
            for (int y = 2; y <= 5; y++) {
    public void worldGateOffDoesNotFell(GameTestHelper helper) {
        level.getGameRules().set(WorldGate.ENABLED, false, level.getServer());
    public void fallingLogDealsGatedDamage(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 4, 2);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);
        FallingBlockEntity log = FallingBlockEntity.fall(level, abs, Blocks.OAK_LOG.defaultBlockState());
        log.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        helper.assertTrue(FallingBlocks.appliesFallDamage(log), "EHM oak log is gated in");
    @GameTest(maxTicks = 40, padding = 8)
    public void netherStemDoesNotFell(GameTestHelper helper) {
                Blocks.CRIMSON_STEM.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "crimson stem is not fellable");
                Blocks.WARPED_STEM.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "warped stem is not fellable");
                Blocks.CRIMSON_HYPHAE.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "crimson hyphae is not fellable");
                Blocks.WARPED_HYPHAE.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "warped hyphae is not fellable");
        helper.assertTrue(Blocks.OAK_LOG.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "oak log is fellable");
        helper.assertTrue(Blocks.JUNGLE_LOG.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "jungle log is fellable");
            helper.setBlock(base.above(y - 1), Blocks.CRIMSON_STEM);
        helper.setBlock(new BlockPos(2, 5, 1), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(2, 5, 3), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(1, 5, 2), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(3, 5, 2), Blocks.JUNGLE_LEAVES);
        breakAndFell(helper, mock, base, Blocks.CRIMSON_STEM.defaultBlockState());
                helper.assertBlockPresent(Blocks.CRIMSON_STEM, base.above(y - 1));
    private static void breakAndFell(GameTestHelper helper, Player player, BlockPos rel, BlockState broken) {
        helper.setBlock(rel, Blocks.AIR);
        RealisticChopping.tryFell(level, player, abs, broken);
        PhysicsQueue.tick(level);
    private static BlockPos[] remainingColumn(BlockPos base, int height) {
        BlockPos[] column = new BlockPos[height - 1];
        for (int i = 1; i < height; i++) {
            column[i - 1] = base.above(i);
        return column;
    private static BlockPos[] remainingTwoByTwo(BlockPos broken, BlockPos[] bases) {
        int height = 6;
        BlockPos[] remaining = new BlockPos[bases.length * height - 1];
        int i = 0;
        for (BlockPos column : bases) {
            for (int y = 0; y < height; y++) {
                BlockPos cell = column.above(y);
                if (cell.equals(broken)) {
                    continue;
                }
                remaining[i++] = cell;
        return remaining;
    private static void assertNoPlacedLogs(GameTestHelper helper, Block log, BlockPos... cells) {
        for (BlockPos rel : cells) {
            helper.assertFalse(helper.getBlockState(rel).is(log), "placed log remaining at " + rel);
            BlockPos abs = helper.absolutePos(rel);
            boolean fallingHere = false;
            AABB box = new AABB(abs).inflate(0.1);
            for (FallingBlockEntity falling : level.getEntities(EntityTypes.FALLING_BLOCK, box, entity -> true)) {
                if (falling.getBlockState().is(log)) {
                    fallingHere = true;
                    break;
                    helper.getBlockState(rel).isAir() || fallingHere,
                    "converted log must be air or a falling entity at " + rel);
    public void tntRecipeMakesThree(GameTestHelper helper) {
        var key = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("tnt"));
        var holder = level.getServer().getRecipeManager().byKey(key);
        helper.assertTrue(holder.isPresent(), "minecraft:tnt recipe loaded");
        RecipeHolder<?> recipeHolder = holder.get();
        helper.assertTrue(recipeHolder.value() instanceof ShapedRecipe, "tnt is shaped");
        ShapedRecipe recipe = (ShapedRecipe) recipeHolder.value();
        CraftingInput input = CraftingInput.of(
                3,
                List.of(
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GUNPOWDER)));
        ItemStack result = recipe.assemble(input);
        helper.assertValueEqual(3, result.getCount(), "tnt recipe yields 3");
        helper.assertTrue(result.is(Items.TNT), "tnt recipe result is tnt");
        helper.assertValueEqual(3, MoreTnt.adjustResult(level, new ItemStack(Items.TNT, 3)).getCount(), "module on keeps 3");
        helper.assertValueEqual(1, MoreTnt.adjustResult(level, new ItemStack(Items.TNT, 3)).getCount(), "gamerule off yields 1");
    @GameTest(padding = 8)
    public void explosionTurnsStoneToCobble(GameTestHelper helper) {
        level.getGameRules().set(GameRules.TNT_EXPLODES, true, level.getServer());
        BlockPos center = new BlockPos(4, 4, 4);
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 2; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        BlockPos abs = helper.absolutePos(center);
        Explosions.create(level, Vec3.atCenterOf(abs), ExplosionType.TNT, null);
        helper.succeedWhen(() -> {
            helper.assertTrue(explosionProducedCobble(helper, center), "TNT explosion softened stone to cobble");
    private static boolean explosionProducedCobble(GameTestHelper helper, BlockPos center) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBBLESTONE)) {
                        return true;
                    }
        AABB box = new AABB(helper.absolutePos(center)).inflate(8.0);
        for (FallingBlockEntity falling : level.getEntities(EntityTypes.FALLING_BLOCK, box, entity -> true)) {
            if (falling.getBlockState().is(Blocks.COBBLESTONE)) {
                return true;
        return false;
    public void explosionInterceptTurnsStoneToCobble(GameTestHelper helper) {
        fillStoneCube(helper, center);
        PrimedTnt tnt = new PrimedTnt(level, abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, null);
        level.addFreshEntity(tnt);
        level.explode(tnt, tnt.getX(), tnt.getY(), tnt.getZ(), 4.0F, false, Level.ExplosionInteraction.TNT);
            helper.assertTrue(explosionProducedCobble(helper, center), "vanilla TNT intercept softened stone");
    public void cancelledExplosionDoesNotBreakOrCrater(GameTestHelper helper) {
        CANCEL_NEXT.set(Boolean.TRUE);
            PrimedTnt tnt = new PrimedTnt(level, abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, null);
            level.addFreshEntity(tnt);
            level.explode(tnt, tnt.getX(), tnt.getY(), tnt.getZ(), 4.0F, false, Level.ExplosionInteraction.TNT);
            CANCEL_NEXT.set(Boolean.FALSE);
        helper.runAfterDelay(25, () -> {
            for (int x = 2; x <= 6; x++) {
                for (int y = 2; y <= 6; y++) {
                    for (int z = 2; z <= 6; z++) {
                        helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, z));
    public void mobGriefingFalseSkipsCreeperWorldDamage(GameTestHelper helper) {
        level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
        Creeper creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        if (creeper == null) {
            helper.fail("creeper create");
            return;
        creeper.snapTo(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5);
        level.addFreshEntity(creeper);
        level.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), 3.0F, false, Level.ExplosionInteraction.MOB);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(Blocks.STONE, center);
            helper.assertBlockPresent(Blocks.STONE, center.above());
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
    @GameTest(maxTicks = 80)
    public void flyingDebrisAutoremovePastRadius(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 2, 2);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        BlockPos spawnRel = new BlockPos(2, 7, 2);
        helper.setBlock(spawnRel, Blocks.AIR);
        BlockPos spawn = helper.absolutePos(spawnRel);
        Vec3 origin = Vec3.atCenterOf(spawn).add(20.0, 0.0, 0.0);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, spawn, Blocks.COBBLESTONE.defaultBlockState());
        falling.setDeltaMovement(0.0, 0.0, 0.0);
        falling.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        falling.setAttached(EhmAttachments.EHM_FLY_ORIGIN, origin);
            helper.assertTrue(falling.isRemoved(), "far flying debris discarded on land");
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, spawnRel);
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, floor.above());
            helper.assertBlockPresent(Blocks.STONE, floor);
    private static void fillStoneCube(GameTestHelper helper, BlockPos center) {
        });
    }

    @GameTest
    public void spawnProcessedSurvivesChunkReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 2, 1);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, pos, EntitySpawnReason.COMMAND);
        zombie.setPersistenceRequired();
        zombie.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        UUID id = zombie.getUUID();
        ChunkPos chunkPos = ChunkPos.containing(zombie.blockPosition());
        level.getChunkSource().save(true);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        if (!zombie.save(output)) {
            helper.fail("processed zombie did not save");
            return;
        }
        CompoundTag tag = output.buildResult();
        zombie.discard();
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        Entity loaded = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.LOAD, entity -> {
            if (!level.addFreshEntity(entity)) {
                return null;
            }
            return entity;
        if (!(loaded instanceof Zombie reloaded)) {
            helper.fail("reloaded entity was not a zombie");
        helper.assertValueEqual(id, reloaded.getUUID(), "uuid");
        helper.assertTrue(
                Boolean.TRUE.equals(reloaded.getAttached(EhmAttachments.EHM_SPAWN_PROCESSED)),
                "spawn_processed persisted across chunk reload");
                level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) != null, "chunk reloaded");
        SpawnReplaceService.replaceIfNeeded(reloaded, level, EntitySpawnReason.NATURAL);
        helper.assertTrue(reloaded.getType() == EntityTypes.ZOMBIE && !reloaded.isRemoved(), "not replaced again");
        helper.assertEntityNotPresent(EntityTypes.WITCH);
        helper.succeed();
    public void spawnReplaceSkippedWhenWorldGateInactive(GameTestHelper helper) {
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, false, server);
        try {
            Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.NATURAL);
            SpawnReplaceService.replaceIfNeeded(zombie, level, EntitySpawnReason.NATURAL);
            helper.assertFalse(
                    Boolean.TRUE.equals(
                            zombie.getAttachedOrElse(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.FALSE)),
                    "spawn_processed not stamped when inactive");
            helper.assertTrue(zombie.getType() == EntityTypes.ZOMBIE, "zombie not replaced when inactive");
            helper.succeed();
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
    @GameTest(maxTicks = 180)
    public void zombieVillagerDoesNotReanimate(GameTestHelper helper) {
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.AIR);
        ZombieVillager villager = helper.spawn(EntityTypes.ZOMBIE_VILLAGER, pos, EntitySpawnReason.COMMAND);
        helper.assertFalse(Zombies.isOrdinaryZombie(villager), "zombie villager is not ordinary");
        villager.kill(level);
        helper.runAfterDelay(165, () -> {
            helper.assertEntityNotPresent(EntityTypes.ZOMBIE);
            helper.assertBlockNotPresent(Blocks.ZOMBIE_HEAD, pos);
            helper.assertBlockNotPresent(Blocks.ZOMBIE_HEAD, pos.above());
    public void reinforcementZombieIsIgnored(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.REINFORCEMENT);
        helper.assertTrue(EntityHelper.ignored(zombie), "finalizeSpawn mixin stamped EHM_IGNORE");
        helper.assertTrue(Zombies.isOrdinaryZombie(zombie), "still an ordinary zombie type");
    public void burningZombieDoesNotPlaceSkull(GameTestHelper helper) {
        zombie.igniteForTicks(8 * 20);
        helper.assertTrue(zombie.getRemainingFireTicks() >= 1 || zombie.isOnFire(), "zombie is on fire");
        zombie.kill(level);
    public void spiderDeathPlacesCobweb(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        var spider = helper.spawn(EntityTypes.SPIDER, floor.above(), EntitySpawnReason.COMMAND);
        spider.kill(level);
        helper.runAfterDelay(2, () -> {
            boolean found = false;
            for (int x = 0; x <= 10; x++) {
                for (int z = 0; z <= 10; z++) {
                    if (helper.getBlockState(new BlockPos(x, 2, z)).is(Blocks.COBWEB)
                            || helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.COBWEB)) {
                        found = true;
                    }
                }
            helper.assertTrue(found, "spider death placed cobweb");
        }
    }

    @GameTest(padding = 8)
    public void magmaCubeGrowsIntoBlazeOnDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MagmaCube cube = helper.spawn(EntityTypes.MAGMA_CUBE, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
        MagmaCube neighbor = helper.spawn(EntityTypes.MAGMA_CUBE, new BlockPos(3, 2, 2), EntitySpawnReason.COMMAND);
        cube.setSize(1, true);
        neighbor.setSize(1, true);
        helper.hurt(cube, level.damageSources().generic(), 1.0F);
        helper.assertTrue(cube.isRemoved(), "damaged magma cube discarded via ALLOW_DAMAGE");
        helper.assertEntityPresent(EntityTypes.BLAZE);
        AABB box = new AABB(helper.absolutePos(new BlockPos(2, 2, 2))).inflate(8.0);
        int blazes = level.getEntities(EntityTypes.BLAZE, box, entity -> true).size();
        helper.assertTrue(blazes == 1, "MAGMACUBE_FIRE must not convert neighbor cubes, blazes=" + blazes);
        helper.succeed();
    @GameTest
    public void zombifiedPiglinStaysAngryAtPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ZombifiedPiglin piglin =
                helper.spawn(EntityTypes.ZOMBIFIED_PIGLIN, new BlockPos(2, 2, 3), EntitySpawnReason.COMMAND);
        piglin.setPersistenceRequired();
        PigMen.keepAngry(piglin, level);
        helper.assertTrue(piglin.isAngry(), "unhit piglin is angry");
        helper.assertTrue(piglin.getTarget() == player || piglin.isAngryAt(player, level), "aggroes nearby player");
        piglin.stopBeingAngry();
        helper.assertTrue(piglin.isAngry(), "stopBeingAngry does not clear always-angry piglin");
        helper.assertTrue(
                piglin.getTarget() == player || piglin.isAngryAt(player, level), "still angry at the player after calm");
    public void nearBedrockBlazeYGate(GameTestHelper helper) {
        helper.assertTrue(Blazes.shouldReplaceNearBedrock(-56, true, -56), "Y=-56 is near bedrock");
        helper.assertFalse(Blazes.shouldReplaceNearBedrock(-55, true, -56), "Y=-55 is above blaze band");
        helper.assertFalse(Blazes.shouldReplaceNearBedrock(-56, false, -56), "boolean disables Y gate");
    public void deepDarkSkipsBlazeReplace(GameTestHelper helper) {
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
                biomes.getOrThrow(Biomes.DEEP_DARK).is(SpawnReplaceService.NO_SPAWN_REPLACEMENTS),
                "deep_dark is in #no_spawn_replacements; SpawnReplaceService skips before Blazes.roll");
        helper.setBiome(Biomes.DEEP_DARK);
        BlockPos here = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(SpawnReplaceService.locationExcluded(level, here), "Deep Dark location is excluded");
        Skeleton deep = helper.spawn(EntityTypes.SKELETON, new BlockPos(1, 2, 1), EntitySpawnReason.NATURAL);
        SpawnReplaceService.replaceIfNeeded(deep, level, EntitySpawnReason.NATURAL);
        helper.assertTrue(!deep.isRemoved() && deep.getType() == EntityTypes.SKELETON, "Deep Dark skeleton not replaced");
        helper.setBiome(Biomes.PLAINS);
        helper.assertFalse(
                SpawnReplaceService.locationExcluded(level, here), "plains is not excluded so Y<=-56 can roll");
        Skeleton cave = helper.spawn(EntityTypes.SKELETON, new BlockPos(2, 2, 1), EntitySpawnReason.COMMAND);
        cave.snapTo(cave.getX(), -56.0, cave.getZ());
                Blazes.rollOverworldSkeleton(cave, level, 100) == EntityTypes.BLAZE,
                "Y=-56 outside Deep Dark rolls blaze at 100%");
    private static void fillStoneCube(GameTestHelper helper, BlockPos center) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 2; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
}
