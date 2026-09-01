package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Configurable potion applied on environmental damage. Empty {@code type} means no effect.
 */
public record PotionEffectHolder(String type, int durationTicks, int amplifier) {
    public static final PotionEffectHolder NONE = new PotionEffectHolder("", 0, 0);

    public boolean isEmpty() {
        return type == null || type.isBlank() || durationTicks <= 0;
    }

    public void apply(LivingEntity entity) {
        if (isEmpty()) {
            return;
        }
        Identifier id = Identifier.tryParse(type);
        if (id == null) {
            return;
        }
        Holder.Reference<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (effect == null) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect, durationTicks, amplifier));
    }

    public static PotionEffectHolder read(CommentedFileConfig file, String path, PotionEffectHolder defaults) {
        Object raw = file.get(path);
        if (raw instanceof Config table) {
            return new PotionEffectHolder(
                    table.getOrElse("type", defaults.type()),
                    table.getOrElse("durationTicks", defaults.durationTicks()),
                    table.getOrElse("amplifier", defaults.amplifier()));
        }
        return new PotionEffectHolder(
                file.getOrElse(path + ".type", defaults.type()),
                file.getOrElse(path + ".durationTicks", defaults.durationTicks()),
                file.getOrElse(path + ".amplifier", defaults.amplifier()));
    }

    public static void writeDefaultIfMissing(CommentedFileConfig file, String path, PotionEffectHolder value) {
        if (file.contains(path) || file.contains(path + ".type")) {
            return;
        }
        file.set(path + ".type", value.type());
        file.set(path + ".durationTicks", value.durationTicks());
        file.set(path + ".amplifier", value.amplifier());
    }

    public void write(CommentedFileConfig file, String path) {
        file.set(path + ".type", type);
        file.set(path + ".durationTicks", durationTicks);
        file.set(path + ".amplifier", amplifier);
    }
}
