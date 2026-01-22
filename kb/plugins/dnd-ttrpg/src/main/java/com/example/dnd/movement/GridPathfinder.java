package com.example.dnd.movement;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.*;

/**
 * A* pathfinding on the Hytale block grid.
 * Finds paths around obstacles while respecting movement distance limits.
 */
public class GridPathfinder {
    private final MovementConfig config;

    public GridPathfinder(MovementConfig config) {
        this.config = config;
    }

    /**
     * Find a path from start to end position within the given movement budget.
     *
     * @param world The world to pathfind in
     * @param start Starting position
     * @param end Target position
     * @param maxDistance Maximum movement distance in blocks
     * @return List of positions forming the path, or null if no path found
     */
    public List<Vector3i> findPath(World world, Vector3i start, Vector3i end, int maxDistance) {
        // Quick validation
        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // If straight-line distance is already too far, fail fast
        double directDistance = config.getHeuristic(start, end);
        if (directDistance > maxDistance) {
            return null;
        }

        // A* implementation
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Vector3i, Node> allNodes = new HashMap<>();
        Set<Vector3i> closedSet = new HashSet<>();

        Node startNode = new Node(start, 0, config.getHeuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;
        int maxIterations = config.getMaxPathLength() * 100; // Prevent infinite loops

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;

            Node current = openSet.poll();

            // Check if we reached the goal
            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            // Explore neighbors
            for (Vector3i neighbor : getNeighbors(current.pos, world)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double moveCost = config.getMoveCost(current.pos, neighbor);
                double tentativeG = current.gScore + moveCost;

                // Check max distance constraint
                if (tentativeG > maxDistance) {
                    continue;
                }

                // Check if path length exceeds maximum
                if (getPathLength(current) >= config.getMaxPathLength()) {
                    continue;
                }

                Node neighborNode = allNodes.get(neighbor);
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor, Double.MAX_VALUE, config.getHeuristic(neighbor, end));
                    allNodes.put(neighbor, neighborNode);
                }

                if (tentativeG < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG + neighborNode.hScore;

                    // Add to open set if not already there
                    openSet.remove(neighborNode);
                    openSet.add(neighborNode);
                }
            }
        }

        // No path found within constraints
        return null;
    }

    /**
     * Get valid neighboring positions from the current position.
     * Supports 8-directional movement (orthogonal + diagonal) plus vertical.
     */
    private List<Vector3i> getNeighbors(Vector3i pos, World world) {
        List<Vector3i> neighbors = new ArrayList<>();

        // 8-directional movement on the XZ plane
        int[] dxz = {-1, 0, 1};

        for (int dx : dxz) {
            for (int dz : dxz) {
                if (dx == 0 && dz == 0) continue;

                // Check same level first
                Vector3i neighbor = new Vector3i(pos.x + dx, pos.y, pos.z + dz);
                if (isWalkable(world, neighbor, pos)) {
                    neighbors.add(neighbor);
                    continue;
                }

                // Check stepping up or down if enabled
                if (config.isAllowVerticalMovement()) {
                    for (int dy = -config.getMaxStepHeight(); dy <= config.getMaxStepHeight(); dy++) {
                        if (dy == 0) continue;
                        neighbor = new Vector3i(pos.x + dx, pos.y + dy, pos.z + dz);
                        if (isWalkable(world, neighbor, pos)) {
                            neighbors.add(neighbor);
                            break; // Only add first valid vertical option
                        }
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * Check if a position is walkable (can stand there).
     *
     * @param world The world
     * @param pos The position to check
     * @param from The position we're coming from
     * @return true if the position is walkable
     */
    private boolean isWalkable(World world, Vector3i pos, Vector3i from) {
        // Check that we can stand at this position:
        // - The block at pos should be air (or passable)
        // - The block at pos+1 (head height) should be air (for player height)
        // - The block below pos should be solid (ground)

        int blockAtFeet = world.getBlock(pos.x, pos.y, pos.z);
        int blockAtHead = world.getBlock(pos.x, pos.y + 1, pos.z);
        int blockBelow = world.getBlock(pos.x, pos.y - 1, pos.z);

        // Block ID 0 is typically air
        boolean feetClear = blockAtFeet == 0;
        boolean headClear = blockAtHead == 0;
        boolean groundSolid = blockBelow != 0;

        if (!feetClear || !headClear || !groundSolid) {
            return false;
        }

        // For diagonal movement, check that we can actually pass through
        // (no walls blocking the diagonal)
        int dx = pos.x - from.x;
        int dz = pos.z - from.z;

        if (dx != 0 && dz != 0) {
            // Diagonal movement - check both intermediate positions
            int block1 = world.getBlock(from.x + dx, from.y, from.z);
            int block2 = world.getBlock(from.x, from.y, from.z + dz);
            int block1Head = world.getBlock(from.x + dx, from.y + 1, from.z);
            int block2Head = world.getBlock(from.x, from.y + 1, from.z + dz);

            // At least one path must be clear for diagonal movement
            boolean path1Clear = (block1 == 0) && (block1Head == 0);
            boolean path2Clear = (block2 == 0) && (block2Head == 0);

            return path1Clear || path2Clear;
        }

        return true;
    }

    /**
     * Reconstruct the path from start to the given node.
     */
    private List<Vector3i> reconstructPath(Node node) {
        List<Vector3i> path = new ArrayList<>();
        Node current = node;

        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Get the length of the path to a node.
     */
    private int getPathLength(Node node) {
        int length = 0;
        Node current = node;
        while (current.parent != null) {
            length++;
            current = current.parent;
        }
        return length;
    }

    /**
     * Internal node class for A* algorithm.
     */
    private static class Node {
        final Vector3i pos;
        Node parent;
        double gScore;  // Cost from start to this node
        double hScore;  // Heuristic cost from this node to goal
        double fScore;  // gScore + hScore

        Node(Vector3i pos, double gScore, double hScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return pos.equals(node.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }
}
