package dev.extrahardmode.feature;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cave-in from→to pairs. Minecraft-free so JUnit can cover stone→cobble without a server.
 */
public final class SoftenMap {
    public static final List<String> DEFAULT_ENTRIES = List.of(
            "minecraft:stone>minecraft:cobblestone",
            "minecraft:deepslate>minecraft:cobbled_deepslate",
            "minecraft:infested_stone>minecraft:cobblestone",
            "minecraft:infested_deepslate>minecraft:cobbled_deepslate");

    private SoftenMap() {}

    public static Map<String, String> parseAll(List<String> entries) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String entry : entries) {
            int split = entry.indexOf('>');
            if (split <= 0 || split == entry.length() - 1) {
                throw new IllegalArgumentException("Soften map entry must be from>to: " + entry);
            }
            String from = qualify(entry.substring(0, split).trim());
            String to = qualify(entry.substring(split + 1).trim());
            if (from.isEmpty() || to.isEmpty()) {
                throw new IllegalArgumentException("Soften map entry must be from>to: " + entry);
            }
            map.put(from, to);
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Soften map is empty");
        }
        return map;
    }

    public static String resultId(String fromId) {
        return parseAll(DEFAULT_ENTRIES).get(qualify(fromId));
    }

    public static String qualify(String id) {
        return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
    }
}
