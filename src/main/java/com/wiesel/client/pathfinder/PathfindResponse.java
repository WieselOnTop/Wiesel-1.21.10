package com.wiesel.client.pathfinder;

import java.util.List;

public class PathfindResponse {
    public List<PathNode> path;
    public List<PathNode> keynodes;

    public PathfindResponse(List<PathNode> path, List<PathNode> keynodes) {
        this.path = path;
        this.keynodes = keynodes;
    }
}
