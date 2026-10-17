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

/** Bottom-right ability icons, highest level first, with each level to the right of its icon. */
public final class AbilityReadyHud {
    public static final net.minecraft.resources.Identifier ELEMENT_ID = ExtraHardModeMod.id("ability_ready");
    /** Cooldown seconds on the icon, half the normal HUD font. */
    static final float COOLDOWN_TEXT_SCALE = 0.5F;

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
            ClientboundAbilityReadyPayload.Entry entry = shown.get(i);
            drawIcon(graphics, entry.ability(), placement);
            if (entry.level() > 0) {
                int color = entry.learned()
                        ? AbilityReadyRules.LEARNED_LEVEL_COLOR
                        : AbilityReadyRules.UNLEARNED_LEVEL_COLOR;
                drawLevel(graphics, minecraft.font, Integer.toString(entry.level()), placement, color);
            }
            if (entry.cooldownSeconds() > 0) {
                drawCooldown(graphics, minecraft.font, Integer.toString(entry.cooldownSeconds()), placement);
            }
        }
    }

    static void drawLevel(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.gui.Font font,
            String text,
            AbilityReadyRules.Placement placement,
            int color) {
        float scale = placement.size() / (float) AbilityReadyRules.ICON_SIZE;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(placement.labelX(), placement.labelY());
            pose.scale(scale, scale);
            graphics.text(font, text, 0, 0, color, true);
        } finally {
            pose.popMatrix();
        }
    }

    /** Yellow seconds centered on the icon, at half the normal HUD font size. */
    static void drawCooldown(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.gui.Font font,
            String text,
            AbilityReadyRules.Placement placement) {
        float textWidth = font.width(text) * COOLDOWN_TEXT_SCALE;
        float textHeight = font.lineHeight * COOLDOWN_TEXT_SCALE;
        float x = placement.x() + (placement.size() - textWidth) / 2.0F;
        float y = placement.y() + (placement.size() - textHeight) / 2.0F;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(x, y);
            pose.scale(COOLDOWN_TEXT_SCALE, COOLDOWN_TEXT_SCALE);
            graphics.text(font, text, 0, 0, AbilityReadyRules.COOLDOWN_COLOR, true);
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
        if (AbilityRules.EMPOWER.equals(ability)) {
            return new ItemStack(Items.REDSTONE);
        }
        if (AbilityRules.BATTERY.equals(ability)) {
            return new ItemStack(Items.QUARTZ_BLOCK);
        }
        if (AbilityRules.LAPIS_SHIELD.equals(ability)) {
            return new ItemStack(Items.LAPIS_LAZULI);
        }
        if (AbilityRules.WRITE_TOME.equals(ability)) {
            return new ItemStack(Items.BOOK);
        }
        if (AbilityRules.READ_TOME.equals(ability)) {
            return new ItemStack(Items.WRITTEN_BOOK);
        }
        return new ItemStack(Items.FEATHER);
    }
}
