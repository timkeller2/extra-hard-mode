package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AbilityReadyRules;
import dev.extrahardmode.feature.AbilityRules;
import dev.extrahardmode.network.ClientboundAbilityReadyPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

/** Bottom-right list of abilities that can be cast above level 2, with each level to the right of its icon. */
public final class AbilityReadyHud {
    public static final net.minecraft.resources.Identifier ELEMENT_ID = ExtraHardModeMod.id("ability_ready");

    private AbilityReadyHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, AbilityReadyHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundAbilityReadyPayload payload = ExtraHardModeClient.lastAbilityReady();
        if (payload == null || payload.abilities().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        List<Integer> sizes = new ArrayList<>();
        List<Integer> labelWidths = new ArrayList<>();
        List<ClientboundAbilityReadyPayload.Entry> shown = new ArrayList<>();
        for (ClientboundAbilityReadyPayload.Entry entry : payload.abilities()) {
            if (entry.size() <= 0 || entry.level() <= 0) {
                continue;
            }
            shown.add(entry);
            sizes.add(entry.size());
            labelWidths.add(minecraft.font.width(Integer.toString(entry.level())));
        }
        List<AbilityReadyRules.Placement> placements = AbilityReadyRules.place(
                graphics.guiWidth(), graphics.guiHeight(), sizes, labelWidths, minecraft.font.lineHeight);
        for (int i = 0; i < placements.size(); i++) {
            AbilityReadyRules.Placement placement = placements.get(i);
            drawIcon(graphics, shown.get(i).ability(), placement);
            drawLevel(graphics, minecraft.font, Integer.toString(shown.get(i).level()), placement);
        }
    }

    static void drawLevel(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.gui.Font font,
            String text,
            AbilityReadyRules.Placement placement) {
        float scale = placement.size() / (float) AbilityReadyRules.ICON_SIZE;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(placement.labelX(), placement.labelY());
            pose.scale(scale, scale);
            graphics.text(font, text, 0, 0, 0xFFFFFFFF, true);
        } finally {
            pose.popMatrix();
        }
    }

    static void drawIcon(GuiGraphicsExtractor graphics, String ability, AbilityReadyRules.Placement placement) {
        float scale = placement.size() / (float) AbilityReadyRules.ICON_SIZE;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(placement.x(), placement.y());
            pose.scale(scale, scale);
            graphics.item(icon(ability), 0, 0);
        } finally {
            pose.popMatrix();
        }
    }

    static ItemStack icon(String ability) {
        if (AbilityRules.HEAL.equals(ability)) {
            return new ItemStack(Items.PAPER);
        }
        if (AbilityRules.IRON_HEART.equals(ability)) {
            return new ItemStack(Items.IRON_INGOT);
        }
        if (AbilityRules.FIRE_BOLT.equals(ability)) {
            return new ItemStack(Items.CHARCOAL);
        }
        if (AbilityRules.MAGIC_ARROW.equals(ability)) {
            return new ItemStack(Items.ARROW);
        }
        if (AbilityRules.GROW.equals(ability)) {
            return new ItemStack(Items.IRON_HOE);
        }
        if (AbilityRules.LIGHT.equals(ability)) {
            return new ItemStack(Items.COAL);
        }
        if (AbilityRules.POWER_MINE.equals(ability)) {
            return new ItemStack(Items.IRON_PICKAXE);
        }
        if (AbilityRules.DETECT_ORE.equals(ability)) {
            return new ItemStack(Items.COMPASS);
        }
        if (AbilityRules.SLOW.equals(ability)) {
            return new ItemStack(Items.STRING);
        }
        if (AbilityRules.SENSE_EVIL.equals(ability)) {
            return new ItemStack(Items.SPIDER_EYE);
        }
        if (AbilityRules.SMITE_EVIL.equals(ability)) {
            return new ItemStack(Items.GOLDEN_SWORD);
        }
        if (AbilityRules.MASTER_BUILDER.equals(ability)) {
            return new ItemStack(Items.STONE_BRICKS);
        }
        if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
            return new ItemStack(Items.DIAMOND);
        }
        return new ItemStack(Items.FEATHER);
    }
}
