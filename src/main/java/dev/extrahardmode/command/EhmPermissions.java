package dev.extrahardmode.command;

import net.fabricmc.fabric.api.permission.v1.PermissionNode;

public final class EhmPermissions {
    public static final PermissionNode<Boolean> ADMIN = PermissionNode.of("extrahardmode", "admin");
    public static final PermissionNode<Boolean> BYPASS = PermissionNode.of("extrahardmode", "bypass");
    public static final PermissionNode<Boolean> BYPASS_CREEPERS = PermissionNode.of("extrahardmode", "bypass.creepers");
    public static final PermissionNode<Boolean> BYPASS_INVENTORY = PermissionNode.of("extrahardmode", "bypass.inventory");
    public static final PermissionNode<Boolean> SILENT_STONE_MINING_HELP =
            PermissionNode.of("extrahardmode", "silent.stone_mining_help");
    public static final PermissionNode<Boolean> SILENT_NO_PLACING_ORE_AGAINST_STONE =
            PermissionNode.of("extrahardmode", "silent.no_placing_ore_against_stone");
    public static final PermissionNode<Boolean> SILENT_REALISTIC_BUILDING =
            PermissionNode.of("extrahardmode", "silent.realistic_building");
    public static final PermissionNode<Boolean> SILENT_LIMITED_TORCH_PLACEMENT =
            PermissionNode.of("extrahardmode", "silent.limited_torch_placement");
    public static final PermissionNode<Boolean> SILENT_NO_TORCHES_HERE =
            PermissionNode.of("extrahardmode", "silent.no_torches_here");

    private EhmPermissions() {}

    public static void register() {
        // PermissionNode.of registers the identifier. Silent/bypass.* nodes are unused until later PRs.
    }
}
