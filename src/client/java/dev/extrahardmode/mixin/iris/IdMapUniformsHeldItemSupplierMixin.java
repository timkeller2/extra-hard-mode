package dev.extrahardmode.mixin.iris;

import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris handheld lighting reads {@code heldBlockLightValue} / {@code heldItemId} from this
 * supplier. Zero both for depth-limited lights (torches) and campfires below the cutoff.
 */
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier", remap = false)
public abstract class IdMapUniformsHeldItemSupplierMixin {
    @Shadow(remap = false)
    @Final
    private InteractionHand hand;

    @Shadow(remap = false)
    private int intID;

    @Shadow(remap = false)
    private int lightValue;

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void ehm$zeroHeldFlameBelowCutoff(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (ExtraHardModeClient.suppressIrisHeldFlameLight(stack)) {
            intID = -1;
            lightValue = 0;
            return;
        }
        // oldHandLight copies the brighter off-hand onto the main-hand uniforms.
        if (hand == InteractionHand.MAIN_HAND
                && ExtraHardModeClient.suppressIrisHeldFlameLight(player.getOffhandItem())) {
            int own = ExtraHardModeClient.vanillaHeldBlockLight(stack);
            lightValue = own;
            if (own <= 0) {
                intID = -1;
            }
        }
    }
}
