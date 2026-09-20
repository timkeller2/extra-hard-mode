package dev.extrahardmode.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccess {
    @Invoker("setText")
    void tougher$setText(Component text);

    @Invoker("setBackgroundColor")
    void tougher$setBackgroundColor(int color);

    @Invoker("setFlags")
    void tougher$setFlags(byte flags);
}
