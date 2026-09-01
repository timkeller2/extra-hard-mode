package dev.extrahardmode.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Environmental vs combat damage for anti-grinder loot checks. */
public record DamageTracker(float environmental, float player) {
    public static final DamageTracker EMPTY = new DamageTracker(0.0F, 0.0F);

    public static final Codec<DamageTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.fieldOf("environmental").forGetter(DamageTracker::environmental),
                    Codec.FLOAT.fieldOf("player").forGetter(DamageTracker::player))
            .apply(instance, DamageTracker::new));

    public DamageTracker addEnvironmental(float amount) {
        return new DamageTracker(environmental + amount, player);
    }

    public DamageTracker addPlayer(float amount) {
        return new DamageTracker(environmental, player + amount);
    }

    /** True when more than half of recorded damage is environmental. */
    public boolean mostlyEnvironmental() {
        float total = environmental + player;
        return total > 0.0F && environmental / total > 0.5F;
    }
}
