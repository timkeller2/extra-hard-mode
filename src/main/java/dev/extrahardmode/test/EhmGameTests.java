package dev.extrahardmode.test;

import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.api.event.EhmExplosionEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.MoreTnt;
import dev.extrahardmode.feature.monster.Zombies;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
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
import net.minecraft.gametest.framework.GameTestHelper;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
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
import net.minecraft.world.level.block.Blocks;
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
        helper.succeed();
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
    public void tntRecipeMakesThree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var key = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("tnt"));
        var holder = level.getServer().getRecipeManager().byKey(key);
        helper.assertTrue(holder.isPresent(), "minecraft:tnt recipe loaded");
        RecipeHolder<?> recipeHolder = holder.get();
        helper.assertTrue(recipeHolder.value() instanceof ShapedRecipe, "tnt is shaped");
        ShapedRecipe recipe = (ShapedRecipe) recipeHolder.value();
        CraftingInput input = CraftingInput.of(
                3,
                3,
                List.of(
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GUNPOWDER),
                        new ItemStack(Items.SAND),
                        new ItemStack(Items.GUNPOWDER)));
        ItemStack result = recipe.assemble(input);
        helper.assertValueEqual(3, result.getCount(), "tnt recipe yields 3");
        helper.assertTrue(result.is(Items.TNT), "tnt recipe result is tnt");
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        helper.assertValueEqual(3, MoreTnt.adjustResult(level, new ItemStack(Items.TNT, 3)).getCount(), "module on keeps 3");
        level.getGameRules().set(WorldGate.ENABLED, false, level.getServer());
        helper.assertValueEqual(1, MoreTnt.adjustResult(level, new ItemStack(Items.TNT, 3)).getCount(), "gamerule off yields 1");
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        helper.succeed();
    }

    @GameTest(padding = 8)
    public void explosionTurnsStoneToCobble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        level.getGameRules().set(GameRules.TNT_EXPLODES, true, level.getServer());
        BlockPos center = new BlockPos(4, 4, 4);
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 2; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos abs = helper.absolutePos(center);
        Explosions.create(level, Vec3.atCenterOf(abs), ExplosionType.TNT, null);
        helper.succeedWhen(() -> {
            helper.assertTrue(explosionProducedCobble(helper, center), "TNT explosion softened stone to cobble");
        });
    }

    private static boolean explosionProducedCobble(GameTestHelper helper, BlockPos center) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 2; z <= 6; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBBLESTONE)) {
                        return true;
                    }
                }
            }
        }
        ServerLevel level = helper.getLevel();
        AABB box = new AABB(helper.absolutePos(center)).inflate(8.0);
        for (FallingBlockEntity falling : level.getEntities(EntityTypes.FALLING_BLOCK, box, entity -> true)) {
            if (falling.getBlockState().is(Blocks.COBBLESTONE)) {
                return true;
            }
        }
        return false;
    }

    @GameTest(padding = 8)
    public void explosionInterceptTurnsStoneToCobble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        level.getGameRules().set(GameRules.TNT_EXPLODES, true, level.getServer());
        BlockPos center = new BlockPos(4, 4, 4);
        fillStoneCube(helper, center);
        BlockPos abs = helper.absolutePos(center);
        PrimedTnt tnt = new PrimedTnt(level, abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, null);
        level.addFreshEntity(tnt);
        level.explode(tnt, tnt.getX(), tnt.getY(), tnt.getZ(), 4.0F, false, Level.ExplosionInteraction.TNT);
        helper.succeedWhen(() -> {
            helper.assertTrue(explosionProducedCobble(helper, center), "vanilla TNT intercept softened stone");
        });
    }

    @GameTest(maxTicks = 40, padding = 8)
    public void cancelledExplosionDoesNotBreakOrCrater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        level.getGameRules().set(GameRules.TNT_EXPLODES, true, level.getServer());
        BlockPos center = new BlockPos(4, 4, 4);
        fillStoneCube(helper, center);
        BlockPos abs = helper.absolutePos(center);
        CANCEL_NEXT.set(Boolean.TRUE);
        try {
            PrimedTnt tnt = new PrimedTnt(level, abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, null);
            level.addFreshEntity(tnt);
            level.explode(tnt, tnt.getX(), tnt.getY(), tnt.getZ(), 4.0F, false, Level.ExplosionInteraction.TNT);
        } finally {
            CANCEL_NEXT.set(Boolean.FALSE);
        }
        helper.runAfterDelay(25, () -> {
            for (int x = 2; x <= 6; x++) {
                for (int y = 2; y <= 6; y++) {
                    for (int z = 2; z <= 6; z++) {
                        helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, y, z));
                    }
                }
            }
            helper.succeed();
        });
    }

    @GameTest(padding = 8)
    public void mobGriefingFalseSkipsCreeperWorldDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
        BlockPos center = new BlockPos(4, 4, 4);
        fillStoneCube(helper, center);
        BlockPos abs = helper.absolutePos(center);
        Creeper creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        if (creeper == null) {
            helper.fail("creeper create");
            return;
        }
        creeper.snapTo(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5);
        level.addFreshEntity(creeper);
        level.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(), 3.0F, false, Level.ExplosionInteraction.MOB);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(Blocks.STONE, center);
            helper.assertBlockPresent(Blocks.STONE, center.above());
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void flyingDebrisAutoremovePastRadius(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
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
        helper.succeedWhen(() -> {
            helper.assertTrue(falling.isRemoved(), "far flying debris discarded on land");
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, spawnRel);
            helper.assertBlockNotPresent(Blocks.COBBLESTONE, floor.above());
            helper.assertBlockPresent(Blocks.STONE, floor);
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
        });
        if (!(loaded instanceof Zombie reloaded)) {
            helper.fail("reloaded entity was not a zombie");
            return;
        }
        helper.assertValueEqual(id, reloaded.getUUID(), "uuid");
        helper.assertTrue(
                Boolean.TRUE.equals(reloaded.getAttached(EhmAttachments.EHM_SPAWN_PROCESSED)),
                "spawn_processed persisted across chunk reload");
        helper.assertTrue(
                level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) != null, "chunk reloaded");
        SpawnReplaceService.replaceIfNeeded(reloaded, level, EntitySpawnReason.NATURAL);
        helper.assertTrue(reloaded.getType() == EntityTypes.ZOMBIE && !reloaded.isRemoved(), "not replaced again");
        helper.assertEntityNotPresent(EntityTypes.WITCH);
        helper.succeed();
    }

    @GameTest
    public void spawnReplaceSkippedWhenWorldGateInactive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
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
        }
    }

    @GameTest(maxTicks = 180)
    public void zombieVillagerDoesNotReanimate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
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
            helper.succeed();
        });
    }

    @GameTest
    public void reinforcementZombieIsIgnored(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.REINFORCEMENT);
        helper.assertTrue(EntityHelper.ignored(zombie), "finalizeSpawn mixin stamped EHM_IGNORE");
        helper.assertTrue(Zombies.isOrdinaryZombie(zombie), "still an ordinary zombie type");
        helper.succeed();
    }

    @GameTest(maxTicks = 180)
    public void burningZombieDoesNotPlaceSkull(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.AIR);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, pos, EntitySpawnReason.COMMAND);
        zombie.igniteForTicks(8 * 20);
        helper.assertTrue(zombie.getRemainingFireTicks() >= 1 || zombie.isOnFire(), "zombie is on fire");
        zombie.kill(level);
        helper.runAfterDelay(165, () -> {
            helper.assertEntityNotPresent(EntityTypes.ZOMBIE);
            helper.assertBlockNotPresent(Blocks.ZOMBIE_HEAD, pos);
            helper.assertBlockNotPresent(Blocks.ZOMBIE_HEAD, pos.above());
            helper.succeed();
        });
    }

    @GameTest
    public void spiderDeathPlacesCobweb(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
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
            }
            helper.assertTrue(found, "spider death placed cobweb");
            helper.succeed();
        });
    }

    private static void fillStoneCube(GameTestHelper helper, BlockPos center) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 2; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }
}
