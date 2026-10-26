package dev.extrahardmode.mixin;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightEngine.class)
public interface LightEngineAccess {
    @Accessor("chunkSource")
    LightChunkGetter tougher$chunkSource();

    @Invoker("enqueueIncrease")
    void tougher$enqueueIncrease(long packedPos, long queueEntry);
}
