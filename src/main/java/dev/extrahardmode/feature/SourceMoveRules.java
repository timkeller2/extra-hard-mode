package dev.extrahardmode.feature;

/**
 * When a bucket may place a real fluid source. Minecraft-free for JUnit.
 *
 * <p>Down and the four sides must be closed, or the cell must be a receptacle.
 * A neighbor source blocks the exception either way. Up does not count as a
 * flow direction.
 */
public final class SourceMoveRules {
    private SourceMoveRules() {}

    public static boolean allowsSource(
            boolean downClosed,
            boolean northClosed,
            boolean southClosed,
            boolean eastClosed,
            boolean westClosed,
            boolean adjacentSource,
            boolean receptacle) {
        if (adjacentSource) {
            return false;
        }
        if (receptacle) {
            return true;
        }
        return downClosed && northClosed && southClosed && eastClosed && westClosed;
    }
}
