package dev.extrahardmode.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccess {
    @Invoker("setText")
    void extrahardmode$setText(Component text);

    @Invoker("setBackgroundColor")
    void extrahardmode$setBackgroundColor(int color);

    @Invoker("setFlags")
    void extrahardmode$setFlags(byte flags);
}
