package com.lucasmuhr.minigames.gather;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mass-teleports every online player to a world and back again, remembering
 * each player's exact position and facing direction from before they left.
 */
public class GatherManager {

    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public int sendAll(World destination) {
        int moved = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (savedLocations.containsKey(uuid)) {
                continue;
            }
            savedLocations.put(uuid, player.getLocation().clone());
            player.teleport(destination.getSpawnLocation());
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

    public void shutdown() {
        returnAll();
    }
}
