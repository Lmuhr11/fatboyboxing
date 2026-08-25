package com.lucasmuhr.minigames.gather;

import com.lucasmuhr.minigames.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mass-teleports every online player to a world and back again. Players are
 * stripped of their inventory, armor and offhand item before being sent -
 * exactly like stepping into the boxing ring - and get it all back exactly
 * as it was the moment they're returned, regardless of which world or
 * dimension they started in.
 *
 * Each world can have its own gather point (/mg setspawn) and its own gather
 * area (/mg setarea1 and /mg setarea2, two corners marking a safe rectangle -
 * same idea as the boxing ring). When an area is configured, everyone is
 * placed on a grid strictly inside those two corners, so nobody can ever land
 * outside the arena no matter how many players show up - extra players just
 * double up on the same spots instead of spilling past the edge. Without an
 * area configured, players are spread out in a spiral around the gather
 * point instead, which has no such boundary.
 *
 * Who's away and what they had is only kept in memory - it does not survive
 * a server restart.
 */
public class GatherManager {

    private static final int SPACING = 3;

    private final JavaPlugin plugin;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();

    public GatherManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int sendAll(World destination) {
        int moved = 0;
        int index = 0;
        Cuboid area = getArea(destination);
        Location spawn = getSpawn(destination);
        if (spawn == null) {
            spawn = destination.getSpawnLocation();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!snapshots.containsKey(uuid)) {
                snapshots.put(uuid, new PlayerSnapshot(player));
                clearInventory(player);
            }
            Location target = area != null
                    ? boundedLocation(area, spawn.getY(), index)
                    : spreadLocation(spawn, index);
            player.teleport(target);
            index++;
            moved++;
        }
        return moved;
    }

    public int returnAll() {
        int moved = 0;
        for (Map.Entry<UUID, PlayerSnapshot> entry : new HashMap<>(snapshots).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                entry.getValue().restore(player);
                moved++;
            }
            snapshots.remove(entry.getKey());
        }
        return moved;
    }

    public void setSpawn(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "gather." + loc.getWorld().getName();
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", (double) loc.getYaw());
        cfg.set(path + ".pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    public void setArea1(Location loc) {
        saveAreaCorner(loc, "area1");
    }

    public void setArea2(Location loc) {
        saveAreaCorner(loc, "area2");
    }

    public void shutdown() {
        returnAll();
    }

    private void clearInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
    }

    private Location getSpawn(World world) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "gather." + world.getName();
        String xStr = cfg.getString(path + ".x");
        if (xStr == null) {
            return null;
        }
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveAreaCorner(Location loc, String corner) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "gather." + loc.getWorld().getName() + "." + corner;
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        plugin.saveConfig();
    }

    private Location loadAreaCorner(World world, String corner) {
        FileConfiguration cfg = plugin.getConfig();
        String path = "gather." + world.getName() + "." + corner;
        String xStr = cfg.getString(path + ".x");
        if (xStr == null) {
            return null;
        }
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        return new Location(world, x, y, z);
    }

    private Cuboid getArea(World world) {
        Location corner1 = loadAreaCorner(world, "area1");
        Location corner2 = loadAreaCorner(world, "area2");
        if (corner1 == null || corner2 == null) {
            return null;
        }
        return new Cuboid(corner1, corner2);
    }

    private Location boundedLocation(Cuboid area, double y, int index) {
        double minX = area.getMinX();
        double maxX = area.getMaxX();
        double minZ = area.getMinZ();
        double maxZ = area.getMaxZ();
        int columns = Math.max(1, (int) ((maxX - minX) / SPACING) + 1);
        int rows = Math.max(1, (int) ((maxZ - minZ) / SPACING) + 1);
        int col = index % columns;
        int row = (index / columns) % rows;
        double x = Math.min(maxX, minX + col * SPACING);
        double z = Math.min(maxZ, minZ + row * SPACING);
        return new Location(area.getWorld(), x, y, z);
    }

    private Location spreadLocation(Location spawn, int index) {
        int[] offset = spiralOffset(index);
        double x = spawn.getX() + offset[0] * SPACING;
        double z = spawn.getZ() + offset[1] * SPACING;
        return new Location(spawn.getWorld(), x, spawn.getY(), z, spawn.getYaw(), spawn.getPitch());
    }

    private int[] spiralOffset(int index) {
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int legLength = 1;
        int stepsInLeg = 0;
        int legsCompleted = 0;
        for (int i = 0; i < index; i++) {
            x += dx;
            z += dz;
            stepsInLeg++;
            if (stepsInLeg == legLength) {
                stepsInLeg = 0;
                int temp = dx;
                dx = -dz;
                dz = temp;
                legsCompleted++;
                if (legsCompleted % 2 == 0) {
                    legLength++;
                }
            }
        }
        return new int[]{x, z};
    }

    private static class PlayerSnapshot {
        final Location returnLocation;
        final ItemStack[] inventoryContents;
        final ItemStack[] armorContents;
        final ItemStack offHand;

        PlayerSnapshot(Player player) {
            this.returnLocation = player.getLocation().clone();
            this.inventoryContents = cloneArray(player.getInventory().getContents());
            this.armorContents = cloneArray(player.getInventory().getArmorContents());
            this.offHand = player.getInventory().getItemInOffHand().clone();
        }

        void restore(Player player) {
            PlayerInventory inv = player.getInventory();
            inv.setContents(inventoryContents);
            inv.setArmorContents(armorContents);
            inv.setItemInOffHand(offHand);
            player.teleport(returnLocation);
        }

        private static ItemStack[] cloneArray(ItemStack[] source) {
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                copy[i] = source[i] == null ? null : source[i].clone();
            }
            return copy;
        }
    }
}
