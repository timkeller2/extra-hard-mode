package dev.extrahardmode.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.PlayerShopRules;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Player SELL and BUY chests. Stored on the overworld so a trader can offer a
 * chest in any dimension.
 */
public final class PlayerShopData extends SavedData {
    public static final Codec<PlayerShopData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("shops", List.of()).forGetter(PlayerShopData::shopList))
            .apply(instance, PlayerShopData::new));

    public static final SavedDataType<PlayerShopData> TYPE = new SavedDataType<>(
            ExtraHardModeMod.id("player_shops"),
            PlayerShopData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX);

    private final LinkedHashSet<String> shops = new LinkedHashSet<>();

    public PlayerShopData() {}

    public PlayerShopData(List<String> shops) {
        if (shops == null) {
            return;
        }
        for (String key : shops) {
            if (PlayerShopRules.parse(key) != null) {
                this.shops.add(key);
            }
        }
    }

    public static PlayerShopData of(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<String> snapshot() {
        return List.copyOf(shops);
    }

    public void remember(String key) {
        PlayerShopRules.Shop shop = PlayerShopRules.parse(key);
        if (shop == null) {
            return;
        }
        if (shops.contains(key)) {
            if (dropOther(key, shop)) {
                setDirty();
            }
            return;
        }
        dropOther(key, shop);
        shops.add(key);
        setDirty();
    }

    public void forget(String key) {
        if (key != null && shops.remove(key)) {
            setDirty();
        }
    }

    public void forgetChest(String dimension, int x, int y, int z) {
        forgetMatching(dimension, x, y, z, true);
    }

    public void forgetSign(String dimension, int x, int y, int z) {
        forgetMatching(dimension, x, y, z, false);
    }

    private void forgetMatching(String dimension, int x, int y, int z, boolean chest) {
        if (dimension == null) {
            return;
        }
        boolean removed = false;
        Iterator<String> it = shops.iterator();
        while (it.hasNext()) {
            PlayerShopRules.Shop shop = PlayerShopRules.parse(it.next());
            if (shop == null) {
                it.remove();
                removed = true;
                continue;
            }
            boolean hit = dimension.equals(shop.dimension())
                    && (chest
                            ? shop.chestX() == x && shop.chestY() == y && shop.chestZ() == z
                            : shop.signX() == x && shop.signY() == y && shop.signZ() == z);
            if (hit) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            setDirty();
        }
    }

    private boolean dropOther(String key, PlayerShopRules.Shop shop) {
        boolean removed = false;
        Iterator<String> it = shops.iterator();
        while (it.hasNext()) {
            String existing = it.next();
            if (existing.equals(key)) {
                continue;
            }
            PlayerShopRules.Shop other = PlayerShopRules.parse(existing);
            if (other != null && (other.sameChest(shop) || other.sameSign(shop))) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    private List<String> shopList() {
        return new ArrayList<>(shops);
    }
}
