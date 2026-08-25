package com.lucasmuhr.minigames.gather;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mass-teleports every online player to a world and back again, remembering
 * each player's exact position and facing direction from before they left.
 * Players are spread out around the destination gather point in a spiral
 * pattern so they don't all land stacked on the same block.
 *
 * Each world can have its own configured gather point (set with /mg setspawn),
 * saved to config.yml so it doesn't depend on Multiverse's or vanilla's own
 * world spawn value, which can silently drift out of sync with each other.
 * If a world has no gather point configured, its normal world spawn is used.
 */
public class GatherManager {

    private static final int SPACING = 3;

    private final JavaPlugin plugin;
    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public GatherManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int sendAll(World destination) {
        int moved = 0;
        int index = 0;
        Location spawn = getSpawn(destination);
        if (spawn == null) {
            spawn = destination.getSpawnLocation();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!savedLocations.containsKey(uuid)) {
                savedLocations.put(uuid, player.getLocation().clone());
            }
            player.teleport(spreadLocation(spawn, index));
            index++;
            moved++;
        }
        return moved;
    }

    public int returnAll() {
        int moved = 0;
        for (Map.Entry<UUID, Location> entry : new HashMap<>(savedLocations).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.teleport(entry.getValue());
                moved++;
            }
            savedLocations.remove(entry.getKey());
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

    public void shutdown() {
        returnAll();
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

    private Location spreadLocation(Location spawn, int index) {
        int[] offset = spiralOffset(index);
        int x = spawn.getBlockX() + offset[0] * SPACING;
        int z = spawn.getBlockZ() + offset[1] * SPACING;
        int y = spawn.getWorld().getHighestBlockYAt(x, z) + 1;
        return new Location(spawn.getWorld(), x + 0.5, y, z + 0.5, spawn.getYaw(), spawn.getPitch());
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
}
