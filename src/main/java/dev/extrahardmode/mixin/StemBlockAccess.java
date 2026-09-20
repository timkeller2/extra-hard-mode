package dev.extrahardmode.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StemBlock.class)
public interface StemBlockAccess {
    @Accessor("fruit")
    ResourceKey<Block> tougher$fruit();

    @Accessor("attachedStem")
    ResourceKey<Block> tougher$attachedStem();

    @Accessor("fruitSupportBlocks")
    TagKey<Block> tougher$fruitSupportBlocks();
}
