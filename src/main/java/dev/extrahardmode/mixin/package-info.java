/**
 * Mixins. Every inject's first statement must be {@code FeatureBus.guard} / a WorldGate check
 * (or a skip if there is no {@code ServerLevel} / sync payload).
 * Mixins. Every inject's first statement must be a WorldGate check
 * (or a skip if there is no {@link net.minecraft.server.level.ServerLevel}).
 */
package dev.extrahardmode.mixin;
