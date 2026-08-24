package com.lucasmuhr.minigames.util;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * A simple axis-aligned box between two corners, used to define the legal
 * fighting area (the ring) inside the arena world.
 */
public class Cuboid {

    private final World world;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;

    public Cuboid(Location corner1, Location corner2) {
        this.world = corner1.getWorld();
        this.minX = Math.min(corner1.getX(), corner2.getX());
        this.minY = Math.min(corner1.getY(), corner2.getY());
        this.minZ = Math.min(corner1.getZ(), corner2.getZ());
        this.maxX = Math.max(corner1.getX(), corner2.getX());
        this.maxY = Math.max(corner1.getY(), corner2.getY());
        this.maxZ = Math.max(corner1.getZ(), corner2.getZ());
    }

    public boolean contains(Location loc) {
        World locWorld = loc.getWorld();
        if (locWorld == null || !locWorld.equals(world)) {
            return false;
        }
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public World getWorld() {
        return world;
    }
}
