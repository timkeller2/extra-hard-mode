package dev.extrahardmode.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface CompoundContainerAccess {
    @Accessor("container1")
    Container tougher$container1();

    @Accessor("container2")
    Container tougher$container2();
}
