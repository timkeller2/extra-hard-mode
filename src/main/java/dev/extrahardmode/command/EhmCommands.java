package dev.extrahardmode.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.AbilityRules;
import dev.extrahardmode.feature.Achievements;
import dev.extrahardmode.feature.EhmHelp;
import dev.extrahardmode.feature.ManaAbilities;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

public final class EhmCommands {
    private static final String[] MODULE_SUGGESTIONS = {
        "hardened_stone",
        "cave_ins",
        "falling_blocks",
        "torches",
        "netherrack_fire",
        "limited_building",
        "realistic_chopping",
        "players",
        "hunger",
        "fish_stocks",
        "anti_farming",
        "water_sources",
        "animal_crowd_control",
        "explosions",
        "more_tnt",
        "anti_grinder",
        "more_monsters",
        "spawn_in_light",
        "tutorial",
        "villager_nerf",
        "spiders",
        "creepers",
        "skeletons",
        "silverfish",
        "zombies",
        "endermen",
        "witches",
        "horses",
        "blazes",
        "dragon",
        "killer_bunny",
        "vindicator",
        "cave_spider",
        "guardians",
        "vex",
        "pigmen",
        "ghasts",
        "biome_bosses",
        "achievements",
        "exploration",
        "mana_abilities",
        "inhabitants"
    };

    private static final SuggestionProvider<CommandSourceStack> MODULE_SUGGESTOR =
            (context, builder) -> SharedSuggestionProvider.suggest(MODULE_SUGGESTIONS, builder);

    private static final SuggestionProvider<CommandSourceStack> TOPIC_SUGGESTOR =
            (context, builder) -> SharedSuggestionProvider.suggest(EhmHelp.topicSuggestions(), builder);

    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTOR =
            (context, builder) -> SharedSuggestionProvider.suggest(AbilityRules.abilityTopics(), builder);

    private EhmCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(EhmCommands::register);
    }

    private static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection) {
        var root = Commands.literal("tougher")
                .executes(EhmCommands::featureHelp)
                .then(Commands.literal("help")
                        .executes(EhmCommands::featureHelp)
                        .then(Commands.literal("ability").executes(EhmCommands::abilityHelp))
                        .then(Commands.literal("homes").executes(EhmCommands::homesHelp))
                        .then(Commands.argument("topic", StringArgumentType.greedyString())
                                .suggests(TOPIC_SUGGESTOR)
                                .executes(EhmCommands::topicHelp)))
                .then(Commands.literal("ability")
                        .executes(EhmCommands::abilityHelp)
                        .then(Commands.literal("help")
                                .executes(EhmCommands::abilityHelp)
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(ABILITY_SUGGESTOR)
                                        .executes(EhmCommands::abilityTopicHelp))))
                .then(Commands.literal("homes")
                        .executes(EhmCommands::homesHelp)
                        .then(Commands.literal("help").executes(EhmCommands::homesHelp)))
                .then(Commands.literal("commands").executes(EhmCommands::commandHelp))
                .then(Commands.literal("achieve").executes(EhmCommands::achieve))
                .then(Commands.literal("me").executes(EhmCommands::me))
                .then(Commands.literal("version").executes(EhmCommands::version))
                .then(Commands.literal("enabled")
                        .executes(EhmCommands::enabledHere)
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(EhmCommands::enabledWorld)))
                .then(Commands.literal("reload").executes(EhmCommands::reload))
                .then(Commands.literal("debug").executes(EhmCommands::debug))
                .then(Commands.literal("bypass").executes(EhmCommands::bypass))
                .then(Commands.literal("set")
                        .then(Commands.argument("module", StringArgumentType.word())
                                .suggests(MODULE_SUGGESTOR)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(EhmCommands::setModule))))
                .then(Commands.literal("set-world")
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(EhmCommands::setWorld)))
                .then(Commands.literal("enable").executes(context -> setWorldEnabled(context, true)))
                .then(Commands.literal("disable").executes(context -> setWorldEnabled(context, false)));
        var node = dispatcher.register(root);
        dispatcher.register(Commands.literal("ehm").redirect(node));
    }

    private static boolean canStaff(CommandSourceStack source) {
        return source.checkPermission(EhmPermissions.ADMIN, false)
                || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
    }

    private static boolean canBypassCommand(CommandSourceStack source) {
        return source.checkPermission(EhmPermissions.BYPASS, false)
                || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
    }

    private static int denyStaff(CommandSourceStack source) {
        source.sendFailure(Component.translatableWithFallback(
                "tougher.command.staff.denied",
                "You need operator permission for that Tougher command. From the server console, run: op <your name>"));
        return 0;
    }

    private static int featureHelp(CommandContext<CommandSourceStack> context) {
        return sendPage(context.getSource(), EhmHelp.overview());
    }

    private static int topicHelp(CommandContext<CommandSourceStack> context) {
        String topic = StringArgumentType.getString(context, "topic");
        EhmHelp.Page page = EhmHelp.resolve(topic);
        if (page == null) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            EhmHelp.UNKNOWN_TOPIC_KEY, EhmHelp.UNKNOWN_TOPIC_FALLBACK, topic.trim()));
            return 0;
        }
        return sendPage(context.getSource(), page);
    }

    private static int abilityHelp(CommandContext<CommandSourceStack> context) {
        return sendPage(context.getSource(), EhmHelp.abilityIndex());
    }

    private static int abilityTopicHelp(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        String ability = AbilityRules.abilityByTopic(name);
        if (ability == null) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            EhmHelp.UNKNOWN_ABILITY_KEY, EhmHelp.UNKNOWN_ABILITY_FALLBACK, name.trim()));
            return 0;
        }
        return sendPage(context.getSource(), EhmHelp.abilityPage(ability));
    }

    private static int homesHelp(CommandContext<CommandSourceStack> context) {
        return sendPage(context.getSource(), EhmHelp.resolve("homes"));
    }

    private static int sendPage(CommandSourceStack source, EhmHelp.Page page) {
        return sendLines(source, page.keys(), page.lines());
    }

    private static int achieve(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CommandSourceStack source = context.getSource();
        return Achievements.sendClosest(player, message -> source.sendSuccess(() -> message, false));
    }

    private static int me(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CommandSourceStack source = context.getSource();
        return ManaAbilities.sendMeReport(player, message -> source.sendSuccess(() -> message, false));
    }

    private static int commandHelp(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(EhmHelp.COMMANDS_KEY, EhmHelp.COMMANDS_FALLBACK),
                        false);
        return Command.SINGLE_SUCCESS;
    }

    private static int sendLines(CommandSourceStack source, java.util.List<String> keys, java.util.List<String> lines) {
        int n = Math.min(keys.size(), lines.size());
        for (int i = 0; i < n; i++) {
            String key = keys.get(i);
            String line = lines.get(i);
            source.sendSuccess(() -> Component.translatableWithFallback(key, line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int version(CommandContext<CommandSourceStack> context) {
        String mod = FabricLoader.getInstance()
                .getModContainer(ExtraHardModeMod.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        String mc = net.minecraft.SharedConstants.getCurrentVersion().name();
        String loader = FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "tougher.command.version",
                                "Tougher %s (Minecraft %s, Fabric Loader %s)",
                                mod,
                                mc,
                                loader),
                        false);
        return Command.SINGLE_SUCCESS;
    }

    private static int enabledHere(CommandContext<CommandSourceStack> context) {
        return reportEnabled(context.getSource(), context.getSource().getLevel());
    }

    private static int enabledWorld(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = DimensionArgument.getDimension(context, "world");
        return reportEnabled(context.getSource(), level);
    }

    private static int reportEnabled(CommandSourceStack source, ServerLevel level) {
        boolean enabled = WorldGate.isActive(level);
        source.sendSuccess(
                () -> Component.translatableWithFallback(
                        enabled ? "tougher.command.enabled.true" : "tougher.command.enabled.false",
                        "Tougher is %s in %s",
                        enabled ? "on" : "off",
                        level.dimension().identifier().toString()),
                false);
        return enabled ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        if (!canStaff(context.getSource())) {
            return denyStaff(context.getSource());
        }
        MinecraftServer server = context.getSource().getServer();
        ConfigManager.reload(server);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback("tougher.command.reload", "Reloaded Tougher config"),
                        true);
        return Command.SINGLE_SUCCESS;
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        if (!canStaff(context.getSource())) {
            return denyStaff(context.getSource());
        }
        boolean next = !ConfigManager.global().debug();
        ConfigManager.setDebug(next);
        PhysicsQueue queue = PhysicsQueue.of(context.getSource().getLevel());
        int depth = queue.queueDepth();
        int live = queue.liveEntities();
        int dropped = queue.dropped();
        int last = queue.conversionsLastTick();
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "tougher.command.debug",
                                "Tougher debug %s (queue=%s live=%s dropped=%s lastTick=%s)",
                                next ? "on" : "off",
                                depth,
                                live,
                                dropped,
                                last),
                        true);
        return Command.SINGLE_SUCCESS;
    }

    private static int bypass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!canBypassCommand(context.getSource())) {
            return denyStaff(context.getSource());
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean next = !Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_BYPASS, Boolean.FALSE));
        player.setAttached(EhmAttachments.EHM_BYPASS, next);
        EhmNetworking.sendSync(player);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "tougher.command.bypass", "Personal Tougher bypass %s", next ? "on" : "off"),
                        false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setModule(CommandContext<CommandSourceStack> context) {
        if (!canStaff(context.getSource())) {
            return denyStaff(context.getSource());
        }
        ServerLevel level = context.getSource().getLevel();
        Identifier moduleId = parseModule(StringArgumentType.getString(context, "module"));
        if (moduleId == null) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            "tougher.command.module.invalid",
                            "Invalid module id '%s'",
                            StringArgumentType.getString(context, "module")));
            return 0;
        }
        boolean value = BoolArgumentType.getBool(context, "value");
        WorldConfig config = ConfigManager.world(level);
        config.setModuleEnabled(moduleId, value);
        if (!saveAndSync(level)) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            "tougher.command.set.failed", "Failed to save Tougher config for this dimension"));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "tougher.command.set",
                                "Set %s to %s in %s",
                                moduleId.toString(),
                                value,
                                level.dimension().identifier().toString()),
                        true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setWorld(CommandContext<CommandSourceStack> context) {
        return setWorldEnabled(context, BoolArgumentType.getBool(context, "value"));
    }

    private static int setWorldEnabled(CommandContext<CommandSourceStack> context, boolean value) {
        if (!canStaff(context.getSource())) {
            return denyStaff(context.getSource());
        }
        ServerLevel level = context.getSource().getLevel();
        WorldConfig config = ConfigManager.world(level);
        config.setEnabled(value);
        if (!saveAndSync(level)) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            "tougher.command.set.failed", "Failed to save Tougher config for this dimension"));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "tougher.command.set-world",
                                "Set Tougher for %s to %s",
                                level.dimension().identifier().toString(),
                                value ? "on" : "off"),
                        true);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean saveAndSync(ServerLevel level) {
        if (!ConfigManager.save(level)) {
            ConfigManager.loadWorld(level);
            return false;
        }
        ConfigManager.loadWorld(level);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level) {
                EhmNetworking.sendSync(player);
            }
        }
        return true;
    }

    static Identifier parseModule(String raw) {
        if (raw.indexOf(':') >= 0) {
            return Identifier.tryParse(raw);
        }
        return Identifier.tryBuild(ExtraHardModeMod.MOD_ID, raw);
    }
}
