package dev.extrahardmode.mixin.client;

import dev.extrahardmode.feature.monster.GuardianSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client playback has no server level, so this is not behind {@code WorldGate}.
 * The attack loop updates its volume every tick; scaling happens when the engine
 * reads that volume.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    private float calculateVolume(float volume, SoundSource source) {
        throw new AssertionError();
    }

    @Inject(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("HEAD"),
            cancellable = true)
    private void tougher$quieterGuardians(SoundInstance instance, CallbackInfoReturnable<Float> cir) {
        Identifier id = instance.getIdentifier();
        if (id == null || !GuardianSounds.isGuardianSound(id.getNamespace(), id.getPath())) {
            return;
        }
        cir.setReturnValue(this.calculateVolume(
                GuardianSounds.volume(id.getNamespace(), id.getPath(), instance.getVolume()), instance.getSource()));
    }
}
