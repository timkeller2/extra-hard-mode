package dev.extrahardmode.feature.monster;

/**
 * Playback volume for guardian and elder guardian sounds. Minecraft-free for JUnit.
 *
 * <p>The laser charge is a looping client sound with no distance falloff, so a
 * few guardians in the ocean stay at full volume. Idle, hurt, death, flop, and
 * the elder curse use the same scale.
 */
public final class GuardianSounds {
    public static final float VOLUME_SCALE = 1.0f / 3.0f;

    private GuardianSounds() {}

    public static boolean isGuardianSound(String namespace, String path) {
        if (!"minecraft".equals(namespace) || path == null) {
            return false;
        }
        return path.startsWith("entity.guardian.") || path.startsWith("entity.elder_guardian.");
    }

    /** Leaves other sounds alone. Guardian sounds are one third of the volume passed in. */
    public static float volume(String namespace, String path, float volume) {
        if (!isGuardianSound(namespace, path)) {
            return volume;
        }
        return volume * VOLUME_SCALE;
    }
}
