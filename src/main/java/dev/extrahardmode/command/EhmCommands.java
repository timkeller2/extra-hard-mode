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
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
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
        "limited_building",
        "realistic_chopping",
        "players",
        "anti_farming",
        "water_sources",
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
        "zombies",
        "endermen",
        "blazes",
        "dragon",
        "killer_bunny",
        "vindicator",
        "cave_spider",
        "guardians",
        "vex"
    };

    private static final SuggestionProvider<CommandSourceStack> MODULE_SUGGESTOR =
            (context, builder) -> SharedSuggestionProvider.suggest(MODULE_SUGGESTIONS, builder);

    private EhmCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(EhmCommands::register);
    }

    private static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("ehm")
                .executes(EhmCommands::help)
                .then(Commands.literal("help").executes(EhmCommands::help))
                .then(Commands.literal("version").executes(EhmCommands::version))
                .then(Commands.literal("enabled")
                        .executes(EhmCommands::enabledHere)
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(EhmCommands::enabledWorld)))
                .then(Commands.literal("reload")
                        .requires(PermissionPredicates.require(EhmPermissions.ADMIN, PermissionLevel.ADMINS))
                        .executes(EhmCommands::reload))
                .then(Commands.literal("debug")
                        .requires(PermissionPredicates.require(EhmPermissions.ADMIN, PermissionLevel.ADMINS))
                        .executes(EhmCommands::debug))
                .then(Commands.literal("bypass")
                        .requires(EhmCommands::canBypassCommand)
                        .executes(EhmCommands::bypass))
                .then(Commands.literal("set")
                        .requires(PermissionPredicates.require(EhmPermissions.ADMIN, PermissionLevel.ADMINS))
                        .then(Commands.argument("module", StringArgumentType.word())
                                .suggests(MODULE_SUGGESTOR)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(EhmCommands::setModule))))
                .then(Commands.literal("set-world")
                        .requires(PermissionPredicates.require(EhmPermissions.ADMIN, PermissionLevel.ADMINS))
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(EhmCommands::setWorld)))
                .then(Commands.literal("enable")
                        .requires(PermissionPredicates.require(EhmPermissions.ADMIN, PermissionLevel.ADMINS))
                        .executes(context -> setWorldEnabled(context, true)))
                .then(Commands.literal("disable")
                        .requires(PermissionPredicates.require(EhmPermissions.ADMIN, PermissionLevel.ADMINS))
                        .executes(context -> setWorldEnabled(context, false))));
    }

    private static boolean canBypassCommand(CommandSourceStack source) {
        return source.checkPermission(EhmPermissions.BYPASS, false)
                || Commands.LEVEL_GAMEMASTERS.check(source.permissions());
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "extrahardmode.command.help",
                                "/ehm help|version|enabled [world]|reload|debug|bypass|set <module> <bool>|set-world <bool>|enable|disable"),
                        false);
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
                                "extrahardmode.command.version",
                                "Extra Hard Mode %s (Minecraft %s, Fabric Loader %s)",
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
                        enabled ? "extrahardmode.command.enabled.true" : "extrahardmode.command.enabled.false",
                        "Extra Hard Mode is %s in %s",
                        enabled ? "on" : "off",
                        level.dimension().identifier().toString()),
                false);
        return enabled ? Command.SINGLE_SUCCESS : 0;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        ConfigManager.reload(server);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback("extrahardmode.command.reload", "Reloaded Extra Hard Mode config"),
                        true);
        return Command.SINGLE_SUCCESS;
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        boolean next = !ConfigManager.global().debug();
        ConfigManager.setDebug(next);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "extrahardmode.command.debug", "EHM debug %s", next ? "on" : "off"),
                        true);
        return Command.SINGLE_SUCCESS;
    }

    private static int bypass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean next = !Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_BYPASS, Boolean.FALSE));
        player.setAttached(EhmAttachments.EHM_BYPASS, next);
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "extrahardmode.command.bypass", "Personal EHM bypass %s", next ? "on" : "off"),
                        false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setModule(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        Identifier moduleId = parseModule(StringArgumentType.getString(context, "module"));
        if (moduleId == null) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            "extrahardmode.command.module.invalid",
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
                            "extrahardmode.command.set.failed", "Failed to save Extra Hard Mode config for this dimension"));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "extrahardmode.command.set",
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
        ServerLevel level = context.getSource().getLevel();
        WorldConfig config = ConfigManager.world(level);
        config.setEnabled(value);
        if (!saveAndSync(level)) {
            context.getSource()
                    .sendFailure(Component.translatableWithFallback(
                            "extrahardmode.command.set.failed", "Failed to save Extra Hard Mode config for this dimension"));
            return 0;
        }
        context.getSource()
                .sendSuccess(
                        () -> Component.translatableWithFallback(
                                "extrahardmode.command.set-world",
                                "Set Extra Hard Mode for %s to %s",
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
