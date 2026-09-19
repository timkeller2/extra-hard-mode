package dev.extrahardmode.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** Marker for the composter's hopper output so bone meal can carry cooking XP. */
@Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
public interface ComposterOutputAccess {}
