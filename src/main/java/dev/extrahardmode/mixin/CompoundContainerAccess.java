package dev.extrahardmode.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface CompoundContainerAccess {
    @Accessor("container1")
    Container extrahardmode$container1();

    @Accessor("container2")
    Container extrahardmode$container2();
}
