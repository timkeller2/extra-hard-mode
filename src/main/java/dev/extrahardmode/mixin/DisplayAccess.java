package dev.extrahardmode.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccess {
    @Invoker("setBillboardConstraints")
    void extrahardmode$setBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setTransformation")
    void extrahardmode$setTransformation(Transformation transformation);

    @Invoker("setBrightnessOverride")
    void extrahardmode$setBrightnessOverride(Brightness brightness);
}
