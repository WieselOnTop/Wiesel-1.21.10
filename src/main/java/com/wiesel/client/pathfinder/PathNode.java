package com.wiesel.client.pathfinder;

public class PathNode {
    public final int x;
    public final int y;
    public final int z;
    public final float topBound;
    public final float pathWeight;
    public final boolean isLiquid;

    public PathNode(int x, int y, int z, float topBound, float pathWeight, boolean isLiquid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.topBound = topBound;
        this.pathWeight = pathWeight;
        this.isLiquid = isLiquid;
    }

    @Override
    public String toString() {
        return String.format("Node(%d, %d, %d)", x, y, z);
    }
}
