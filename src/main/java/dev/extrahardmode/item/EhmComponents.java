package dev.extrahardmode.item;

import com.mojang.serialization.Codec;
import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class EhmComponents {
    public static final DataComponentType<Integer> HARDENED_MINED = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ExtraHardModeMod.id("hardened_mined"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private EhmComponents() {}

    public static void register() {
        // Static fields register on class load.
    }
}
