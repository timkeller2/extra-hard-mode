package dev.extrahardmode.client;

import dev.extrahardmode.feature.AbilityRules;
import dev.extrahardmode.feature.ManaAbilities;
import dev.extrahardmode.feature.SmiteCrosshairRules;
import dev.extrahardmode.network.ClientboundAbilityReadyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/** Gold crosshair while a golden sword is on a creature Smite Evil can reach. */
public final class SmiteCrosshair {
    private SmiteCrosshair() {}

    public static int color(Minecraft minecraft) {
        return SmiteCrosshairRules.color(show(minecraft));
    }

    static boolean show(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (!minecraft.player.getMainHandItem().is(Items.GOLDEN_SWORD)) {
            return false;
        }
        var mana = ExtraHardModeClient.lastMana();
        double current = mana == null ? 0.0 : mana.currentMana();
        double range = smiteRange(ExtraHardModeClient.lastAbilityReady());
        if (range <= 0.0 || current < SmiteCrosshairRules.MANA_REQUIRED) {
            return false;
        }
        EntityHitResult hit = ManaAbilities.raycast(minecraft.player, range);
        boolean creature = hit != null && hit.getEntity() instanceof LivingEntity;
        return SmiteCrosshairRules.highlight(true, current, range, creature);
    }

    static double smiteRange(ClientboundAbilityReadyPayload payload) {
        if (payload == null) {
            return 0.0;
        }
        for (ClientboundAbilityReadyPayload.Entry entry : payload.abilities()) {
            if (AbilityRules.SMITE_EVIL.equals(entry.ability())) {
                return Math.max(0.0, entry.power());
            }
        }
        return 0.0;
    }
}
