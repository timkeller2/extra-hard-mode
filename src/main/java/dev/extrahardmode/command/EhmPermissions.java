package dev.extrahardmode.command;

import dev.extrahardmode.module.MessageId;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;

public final class EhmPermissions {
    public static final PermissionNode<Boolean> ADMIN = PermissionNode.of("tougher", "admin");
    public static final PermissionNode<Boolean> BYPASS = PermissionNode.of("tougher", "bypass");
    public static final PermissionNode<Boolean> BYPASS_CREEPERS = PermissionNode.of("tougher", "bypass.creepers");
    public static final PermissionNode<Boolean> BYPASS_INVENTORY = PermissionNode.of("tougher", "bypass.inventory");
    public static final PermissionNode<Boolean> SILENT_STONE_MINING_HELP =
            PermissionNode.of("tougher", "silent.stone_mining_help");
    public static final PermissionNode<Boolean> SILENT_NO_PLACING_ORE_AGAINST_STONE =
            PermissionNode.of("tougher", "silent.no_placing_ore_against_stone");
    public static final PermissionNode<Boolean> SILENT_REALISTIC_BUILDING =
            PermissionNode.of("tougher", "silent.realistic_building");
    public static final PermissionNode<Boolean> SILENT_LIMITED_TORCH_PLACEMENT =
            PermissionNode.of("tougher", "silent.limited_torch_placement");
    public static final PermissionNode<Boolean> SILENT_NO_TORCHES_HERE =
            PermissionNode.of("tougher", "silent.no_torches_here");

    private EhmPermissions() {}

    public static void register() {
        // PermissionNode.of registers the identifier.
    }

    /** Only the five plugin.yml silent nodes. Pillar-beneath shares realistic_building. */
    public static PermissionNode<Boolean> silentNode(MessageId id) {
        if (id == null) {
            return null;
        }
        return switch (id) {
            case STONE_MINING_HELP -> SILENT_STONE_MINING_HELP;
            case NO_PLACING_ORE_AGAINST_STONE -> SILENT_NO_PLACING_ORE_AGAINST_STONE;
            case REALISTIC_BUILDING, REALISTIC_BUILDING_BENEATH -> SILENT_REALISTIC_BUILDING;
            case LIMITED_TORCH_PLACEMENT -> SILENT_LIMITED_TORCH_PLACEMENT;
            case NO_TORCHES_HERE -> SILENT_NO_TORCHES_HERE;
            default -> null;
        };
    }
}
