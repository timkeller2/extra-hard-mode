package dev.extrahardmode.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.extrahardmode.feature.AutoStackRules;
import java.util.List;
import dev.extrahardmode.network.ClientboundAutoStackChestPayload;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ServerboundAutoStackChestQueryPayload;
import dev.extrahardmode.network.ServerboundAutoStackExcludePayload;
import dev.extrahardmode.network.ServerboundAutoStackPayload;
import dev.extrahardmode.player.EhmAttachments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Auto-stack key, the excluded-slot outline, and the clicks that toggle a slot. */
public final class AutoStackClient {
    /** Yellow slot border. Same ink as a food-craving mark. */
    private static final int OUTLINE = 0xFFFFE14A;

    public static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.tougher.auto_stack",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G,
            KeyMapping.Category.GAMEPLAY));

    private static Screen trackedScreen;
    private static boolean chestKnown;
    private static long chestMask;

    private AutoStackClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundAutoStackChestPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().execute(() -> {
                    chestKnown = payload.active();
                    chestMask = payload.active() ? payload.mask() : 0L;
                }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            watchChest(client);
            while (KEY.consumeClick()) {
                tryDeposit(client);
            }
        });
    }

    /**
     * Alt-click toggles the hovered main-inventory or chest slot. {@code whole} is the
     * second click of a double-click and flips every other slot in that same inventory.
     * Returning true records the click so the next one can arrive as a double-click.
     */
    public static boolean tryToggle(Slot slot, boolean whole) {
        Minecraft minecraft = Minecraft.getInstance();
        if (slot == null || minecraft.player == null || minecraft.player.isSpectator()) {
            return false;
        }
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync != null && !sync.worldActive()) {
            return false;
        }
        if (slot.container instanceof Inventory inventory && inventory.player == minecraft.player) {
            int index = slot.getContainerSlot();
            if (!AutoStackRules.mainSlot(index)) {
                return false;
            }
            int mask = minecraft.player.getAttachedOrElse(EhmAttachments.EHM_AUTO_STACK_EXCLUDED, 0);
            int next = whole ? AutoStackRules.toggleMainRest(mask, index) : AutoStackRules.toggle(mask, index);
            minecraft.player.setAttached(EhmAttachments.EHM_AUTO_STACK_EXCLUDED, next);
            if (ClientPlayNetworking.canSend(ServerboundAutoStackExcludePayload.TYPE)) {
                ClientPlayNetworking.send(new ServerboundAutoStackExcludePayload(index, false, whole));
            }
            return true;
        }
        if (!(minecraft.gui.screen() instanceof ContainerScreen screen)) {
            return false;
        }
        if (slot.container != screen.getMenu().getContainer()) {
            return false;
        }
        int index = slot.getContainerSlot();
        int size = screen.getMenu().getContainer().getContainerSize();
        if (!AutoStackRules.chestSlot(index, size)) {
            return false;
        }
        if (chestKnown) {
            long next = whole
                    ? AutoStackRules.toggleChestRest(chestMask, size, index)
                    : AutoStackRules.toggleChest(chestMask, index, size);
            chestMask = AutoStackRules.clampChest(next, size);
        }
        if (ClientPlayNetworking.canSend(ServerboundAutoStackExcludePayload.TYPE)) {
            ClientPlayNetworking.send(new ServerboundAutoStackExcludePayload(index, true, whole));
        }
        return true;
    }

    /**
     * Paints every excluded slot. A new GUI stratum puts the ink above the white hover
     * frame, which otherwise covers the ring around the slot under the cursor.
     */
    public static void drawExcluded(GuiGraphicsExtractor graphics, List<Slot> slots) {
        if (graphics == null || slots == null || slots.isEmpty()) {
            return;
        }
        boolean raised = false;
        for (Slot slot : slots) {
            if (!excludedSlot(slot)) {
                continue;
            }
            if (!raised) {
                graphics.nextStratum();
                raised = true;
            }
            outline(graphics, slot);
        }
    }

    private static boolean excludedSlot(Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (slot == null || minecraft.player == null) {
            return false;
        }
        if (slot.container instanceof Inventory inventory && inventory.player == minecraft.player) {
            int mask = minecraft.player.getAttachedOrElse(EhmAttachments.EHM_AUTO_STACK_EXCLUDED, 0);
            return AutoStackRules.excluded(mask, slot.getContainerSlot());
        }
        return chestKnown && chestSection(slot) && AutoStackRules.chestExcluded(chestMask, slot.getContainerSlot());
    }

    private static boolean chestSection(Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof ContainerScreen screen)) {
            return false;
        }
        return slot.container == screen.getMenu().getContainer();
    }

    /**
     * One-pixel ring on the slot edge. That pixel sits inside the hover sprite, so the
     * white frame does not cover it. One pixel is the thinnest solid line the GUI draws.
     */
    private static void outline(GuiGraphicsExtractor graphics, Slot slot) {
        graphics.outline(slot.x, slot.y, 16, 16, OUTLINE);
    }

    private static void watchChest(Minecraft client) {
        Screen screen = client.gui.screen();
        if (screen == trackedScreen) {
            return;
        }
        trackedScreen = screen;
        chestKnown = false;
        chestMask = 0L;
        if (!(screen instanceof ContainerScreen)) {
            return;
        }
        if (client.player == null || client.player.isSpectator()) {
            return;
        }
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync != null && !sync.worldActive()) {
            return;
        }
        if (ClientPlayNetworking.canSend(ServerboundAutoStackChestQueryPayload.TYPE)) {
            ClientPlayNetworking.send(ServerboundAutoStackChestQueryPayload.INSTANCE);
        }
    }

    private static void tryDeposit(Minecraft client) {
        if (client.player == null || client.player.isSpectator()) {
            return;
        }
        if (client.gui.screen() != null && !(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            return;
        }
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync != null && !sync.worldActive()) {
            return;
        }
        if (!ClientPlayNetworking.canSend(ServerboundAutoStackPayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(ServerboundAutoStackPayload.INSTANCE);
    }
}
