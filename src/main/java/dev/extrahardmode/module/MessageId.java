package dev.extrahardmode.module;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tutorial / deny message catalog. Silent nodes exist only for the five
 * {@code plugin.yml} ids; extras persist via {@code ehm:tutorial} counts.
 */
public enum MessageId {
    STONE_MINING_HELP(
            "stone_mining_help",
            Kind.ACTION_BAR,
            true,
            "You'll need an iron or diamond pickaxe to break stone. Try exploring natural formations for exposed ore like coal, which softens stone around it when broken."),
    NO_PLACING_ORE_AGAINST_STONE(
            "no_placing_ore_against_stone",
            Kind.ACTION_BAR,
            true,
            "Sorry, you can't place ore next to stone."),
    REALISTIC_BUILDING("realistic_building", Kind.ACTION_BAR, true, "You can't build while in the air."),
    REALISTIC_BUILDING_BENEATH(
            "realistic_building_beneath",
            Kind.ACTION_BAR,
            "silent.realistic_building",
            "You can't place a block directly beneath you."),
    LIMITED_TORCH_PLACEMENT(
            "limited_torch_placement", Kind.ACTION_BAR, true, "It's too soft there to fasten a torch."),
    NO_TORCHES_HERE(
            "no_torches_here",
            Kind.ACTION_BAR,
            true,
            "There's not enough air flow down here for permanent flames. Use another method to light your way."),
    NO_CRAFTING_MELON_SEEDS("no_crafting_melon_seeds", Kind.ONCE, false, "That appears to be seedless!"),
    HEAVY_INVENTORY("heavy_inventory", Kind.ONCE, false, "You're carrying too much weight to swim!"),
    ZOMBIE_SLOW("zombie_slow", Kind.ONCE, false, "Zombies slow you down when hit!"),
    DRAGON_CHALLENGE("dragon_challenge", Kind.ANNOUNCE, false, "%s is challenging the dragon!"),
    DRAGON_DEFEAT("dragon_defeat", Kind.ANNOUNCE, false, "The dragon has been defeated!"),
    LIMITED_END_BUILDING(
            "limited_end_building",
            Kind.ACTION_BAR,
            false,
            "Sorry, building here is very limited. You may only break blocks to reach ground level."),
    ZOMBIE_RESPAWN("zombie_respawn", Kind.TOAST, false, "Zombies might resurrect if not on fire!"),
    CHARGED_CREEPER("charged_creeper", Kind.TOAST, false, "Charged Creepers explode instantly when hit. Run!"),
    BLAZE_OVERWORLD(
            "blaze_overworld",
            Kind.TOAST,
            false,
            "Blazes spawn near lava and their fiery breath causes a big explosion on death!"),
    BLAZE_NETHER("blaze_nether", Kind.TOAST, false, "Blazes spawn everywhere in the Nether and may split on death!"),
    MAGMACUBE("magmacube", Kind.TOAST, false, "These small buggers actually are just disguised blazes!"),
    GHAST_WARNING(
            "ghast_warning",
            Kind.TOAST,
            false,
            "These fearsome Ghasts wear invisible arrow deflective armor! Ghasts drop a lot more loot as well."),
    PIGZOMBIE("pigzombie", Kind.TOAST, false, "RUN! Pig Zombies are always angry and hungry!"),
    PIGZOMBIE_WART("pigzombie_wart", Kind.TOAST, false, "You can get netherwart from slaying Pig Zombies"),
    SKELETON_DEFLECT_ARROW(
            "skeleton_deflect_arrow",
            Kind.TOAST,
            false,
            "Arrows just pass through Skeletons, you gotta go close combat!"),
    ENDERMAN_TELEPORT("enderman_teleport", Kind.TOAST, false, "Enderman can teleport you too!"),
    CREEPER_DROP_TNT("creeper_drop_tnt", Kind.TOAST, false, "Creepers may drop activated tnt on death!"),
    EXTINGUISH_FIRE("extinguish_fire", Kind.TOAST, false, "Putting out fire with your hand will catch you on fire."),
    BUCKET_FILL(
            "bucket_fill",
            Kind.TOAST,
            false,
            "You can pick up water, but once you place it, it evaporates. Get some ice if you want to farm!"),
    ANTIFARM_UNWATERED("antifarm_unwatered", Kind.TOAST, false, "Your crops need sufficient water, otherwise they'll dry out!"),
    ANTIFARM_NATURAL_LIGHT("antifarm_natural_light", Kind.TOAST, false, "Your crops require natural light to grow!"),
    ANTIFARM_DESERT("antifarm_desert", Kind.TOAST, false, "Deserts are really dry and nothing grows here!"),
    ANIMAL_OVERCROWD(
            "animal_overcrowd",
            Kind.TOAST,
            false,
            "Animals need space! Consider putting them in a bigger area"),
    NETHER_WARN(
            "nether_warn",
            Kind.TOAST,
            false,
            "This is a dangerous place. Make sure you come prepared with arrows and good gear."),
    LOST_ITEMS("lost_items", Kind.TOAST, false, "On death there is a chance you might lose some of your items!"),
    ENABLED_ON("enabled_on", Kind.TOAST, false, "Extra Hard Mode is on. /gamerule extrahardmode:enabled"),
    ENABLED_OFF("enabled_off", Kind.TOAST, false, "Extra Hard Mode is off. /gamerule extrahardmode:enabled");

    private static final Map<String, MessageId> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(MessageId::id, Function.identity()));

    private final String id;
    private final Kind kind;
    private final String silentPath;
    private final String fallback;

    MessageId(String id, Kind kind, boolean hasSilentNode, String fallback) {
        this(id, kind, hasSilentNode ? "silent." + id : null, fallback);
    }

    MessageId(String id, Kind kind, String silentPath, String fallback) {
        this.id = id;
        this.kind = kind;
        this.silentPath = silentPath;
        this.fallback = fallback;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public boolean hasSilentNode() {
        return silentPath != null;
    }

    public String fallback() {
        return fallback;
    }

    public String messageKey() {
        return "extrahardmode.message." + id;
    }

    public String toastKey() {
        return "extrahardmode.toast." + id;
    }

    public String silentPermissionPath() {
        return silentPath;
    }

    public static MessageId byId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    public enum Kind {
        ACTION_BAR,
        TOAST,
        ONCE,
        /** Server-wide chat plus a show-once toast. */
        ANNOUNCE,
        BROADCAST
    }
}
