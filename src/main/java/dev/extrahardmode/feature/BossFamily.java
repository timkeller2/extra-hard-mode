package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.Biome;

/**
 * One persistent biome boss family. Cooldown and living UUID are per family, not global.
 */
public enum BossFamily {
    LUSH_CAVES("lush_caves", "Lush Cave Broodmother", EntityTypes.SPIDER, false),
    SULFUR_CAVES("sulfur_caves", "Sulfur Magma Titan", EntityTypes.MAGMA_CUBE, false),
    DRIPSTONE("dripstone", "Dripstone Drowned", EntityTypes.DROWNED, true),
    DARK_FOREST("dark_forest", "Dark Forest Horde King", EntityTypes.ZOMBIE, false),
    JUNGLE("jungle", "Jungle Brood Spider", EntityTypes.CAVE_SPIDER, false),
    DESERT("desert", "Desert Husk Champion", EntityTypes.HUSK, false),
    SWAMP("swamp", "Swamp Hex Witch", EntityTypes.WITCH, false),
    MUSHROOM("mushroom", "Mushroom Horde King", EntityTypes.ZOMBIE, false),
    OCEAN("ocean", "Ocean Guardian", EntityTypes.GUARDIAN, true),
    SAVANNA("savanna", "Savanna Ravager", EntityTypes.RAVAGER, false),
    PLAINS("plains", "Plains Ravager", EntityTypes.RAVAGER, false),
    COLD("cold", "Cold Stray Sniper", EntityTypes.STRAY, false),
    DROWNED("drowned", "River Drowned", EntityTypes.DROWNED, true),
    FOREST("forest", "Forest Broodmother", EntityTypes.SPIDER, false),
    NETHER_WASTES("nether_wastes", "Nether Blaze Lord", EntityTypes.BLAZE, false),
    CRIMSON("crimson", "Crimson Hoglin", EntityTypes.HOGLIN, false),
    SOUL_SAND("soul_sand", "Soul Sand Wither Knight", EntityTypes.WITHER_SKELETON, false);

    private final String id;
    private final String fallbackName;
    private final EntityType<?> entityType;
    private final boolean aquatic;
    private final TagKey<Biome> biomes;

    BossFamily(String id, String fallbackName, EntityType<?> entityType, boolean aquatic) {
        this.id = id;
        this.fallbackName = fallbackName;
        this.entityType = entityType;
        this.aquatic = aquatic;
        this.biomes = TagKey.create(Registries.BIOME, ExtraHardModeMod.id("boss/" + id));
    }

    public String id() {
        return id;
    }

    public String fallbackName() {
        return fallbackName;
    }

    public String translationKey() {
        return "tougher.boss." + id;
    }

    public EntityType<?> entityType() {
        return entityType;
    }

    public boolean aquatic() {
        return aquatic;
    }

    /** Lush, forest, and jungle spider bosses. They spit and do not flee light once hit. */
    public boolean brood() {
        return this == LUSH_CAVES || this == FOREST || this == JUNGLE;
    }

    public TagKey<Biome> biomes() {
        return biomes;
    }

    public Identifier tagId() {
        return ExtraHardModeMod.id("boss/" + id);
    }

    public static BossFamily byId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (BossFamily family : values()) {
            if (family.id.equals(id)) {
                return family;
            }
        }
        return null;
    }
}
