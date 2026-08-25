package com.lucasmuhr.minigames.boxing;

import com.lucasmuhr.minigames.util.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the boxing queue, match state, arena protection and cleanup.
 *
 * Rules enforced here:
 *  - staff queue players by name with /box join <player>, matches auto-start once 2 are queued
 *  - fighters start with an empty inventory, full health and full hunger
 *  - only fists work - items can't be picked up or dropped during a match
 *  - the loser respawns at the configured exit point, never at their bed
 *  - players are unhittable in the arena world unless both are standing inside the ring, except ops
 *  - the arena world itself is protected from block break/place by non-ops
 */
public class BoxingManager implements Listener {

    private final JavaPlugin plugin;

    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Set<UUID> awaitingCustomRespawn = new HashSet<>();
    private Match currentMatch;

    public BoxingManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ----------------------------------------------------------------
    // Queue / match lifecycle
    // ----------------------------------------------------------------

    /**
     * Queues the given player for boxing. Returns an error message to show the
     * command sender, or null on success.
     */
    public String joinQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (currentMatch != null && currentMatch.has(uuid)) {
            return player.getName() + " is already in a boxing match!";
        }
        if (queue.contains(uuid)) {
            return player.getName() + " is already in the boxing queue.";
        }
        if (!isArenaConfigured()) {
            return "The boxing arena isn't set up yet - run /box setring1, setring2, setspawn1, setspawn2 and setexit first.";
        }

        queue.add(uuid);
        Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW
                + " has entered the boxing queue! (" + queue.size() + " waiting)");
        tryStartMatch();
        return null;
    }

    /**
     * Removes the given player from the queue. Returns an error message to show
     * the command sender, or null on success.
     */
    public String leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();
        if (queue.remove(uuid)) {
            return null;
        }
        return player.getName() + " isn't in the boxing queue.";
    }

    private void tryStartMatch() {
        if (currentMatch != null) {
            return;
        }
        if (queue.size() < 2) {
            return;
        }

        Iterator<UUID> it = queue.iterator();
        UUID uuid1 = it.next();
        it.remove();
        UUID uuid2 = it.next();
        it.remove();

        Player p1 = Bukkit.getPlayer(uuid1);
        Player p2 = Bukkit.getPlayer(uuid2);

        if (p1 == null || !p1.isOnline()) {
            if (p2 != null && p2.isOnline()) {
                queue.add(uuid2);
            }
            tryStartMatch();
            return;
        }
        if (p2 == null || !p2.isOnline()) {
            queue.add(uuid1);
            tryStartMatch();
            return;
        }

        startMatch(p1, p2);
    }

    private void startMatch(Player p1, Player p2) {
        Location spawn1 = getSpawn1();
        Location spawn2 = getSpawn2();
        if (spawn1 == null || spawn2 == null) {
            p1.sendMessage(ChatColor.RED + "The boxing arena isn't fully configured - match cancelled.");
            p2.sendMessage(ChatColor.RED + "The boxing arena isn't fully configured - match cancelled.");
            return;
        }

        currentMatch = new Match(p1.getUniqueId(), p2.getUniqueId());
        snapshots.put(p1.getUniqueId(), new PlayerSnapshot(p1));
        snapshots.put(p2.getUniqueId(), new PlayerSnapshot(p2));

        prepareFighter(p1, spawn1);
        prepareFighter(p2, spawn2);

        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "BOXING: " + ChatColor.RESET
                + ChatColor.GOLD + p1.getName() + ChatColor.YELLOW + " vs " + ChatColor.GOLD + p2.getName()
                + ChatColor.YELLOW + " - fists only, first to go down loses!");
    }

    private void prepareFighter(Player player, Location spawn) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);

        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }

        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(spawn);
    }

    private void finishFighter(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerSnapshot snapshot = snapshots.remove(uuid);
        if (snapshot != null) {
            snapshot.restore(player);
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);

        Location destination = snapshot != null ? snapshot.returnLocation : getExit();
        if (destination != null) {
            player.teleport(destination);
        }
    }

    public void shutdown() {
        for (Map.Entry<UUID, PlayerSnapshot> entry : snapshots.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                PlayerSnapshot snapshot = entry.getValue();
                snapshot.restore(player);
                if (snapshot.returnLocation != null) {
                    player.teleport(snapshot.returnLocation);
                }
            }
        }
        snapshots.clear();
        queue.clear();
        awaitingCustomRespawn.clear();
        currentMatch = null;
    }

    // ----------------------------------------------------------------
    // Event handling
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();

        if (currentMatch == null || !currentMatch.has(victimId)) {
            return;
        }

        UUID winnerId = currentMatch.other(victimId);
        Player winner = Bukkit.getPlayer(winnerId);

        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        String winnerName = winner != null ? winner.getName() : "Someone";
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD
                + winnerName + ChatColor.RESET + ChatColor.YELLOW
                + " has won the boxing match against " + ChatColor.GOLD + victim.getName() + ChatColor.YELLOW
                + "! " + ChatColor.GOLD + winnerName + ChatColor.YELLOW + ", talk to staff to claim your prize.");

        awaitingCustomRespawn.add(victimId);
        currentMatch = null;

        if (winner != null && winner.isOnline()) {
            finishFighter(winner);
        } else {
            snapshots.remove(winnerId);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::tryStartMatch, 60L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingCustomRespawn.remove(uuid)) {
            return;
        }

        PlayerSnapshot snapshot = snapshots.get(uuid);
        Location destination = (snapshot != null && snapshot.returnLocation != null) ? snapshot.returnLocation : getExit();
        if (destination != null) {
            event.setRespawnLocation(destination);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerSnapshot removed = snapshots.remove(uuid);
            if (removed != null) {
                removed.restore(player);
            }
            player.setFoodLevel(20);
            player.setSaturation(20f);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        queue.remove(uuid);
        boolean wasAwaitingRespawn = awaitingCustomRespawn.remove(uuid);

        if (currentMatch != null && currentMatch.has(uuid)) {
            UUID winnerId = currentMatch.other(uuid);
            Player winner = Bukkit.getPlayer(winnerId);
            currentMatch = null;

            // restore the leaving fighter's items now, while they're still online,
            // so their saved player data doesn't get stuck with an empty inventory
            PlayerSnapshot leavingSnapshot = snapshots.remove(uuid);
            if (leavingSnapshot != null) {
                leavingSnapshot.restore(player);
            }

            if (winner != null && winner.isOnline()) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName() + ChatColor.RESET
                        + ChatColor.YELLOW + " wins the boxing match - " + player.getName() + " forfeited by leaving!");
                finishFighter(winner);
            } else {
                snapshots.remove(winnerId);
            }

            Bukkit.getScheduler().runTaskLater(plugin, this::tryStartMatch, 60L);
        } else if (wasAwaitingRespawn) {
            // they died in the ring and disconnected from the death screen before respawning
            PlayerSnapshot snapshot = snapshots.remove(uuid);
            if (snapshot != null) {
                snapshot.restore(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        Cuboid ring = getRing();
        if (ring == null) {
            return;
        }
        if (!attacker.getWorld().equals(ring.getWorld())) {
            return;
        }
        if (attacker.isOp()) {
            return;
        }

        if (ring.contains(attacker.getLocation()) && ring.contains(defender.getLocation())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Cuboid ring = getRing();
        if (ring == null || event.getPlayer().isOp()) {
            return;
        }
        if (event.getBlock().getWorld().equals(ring.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Cuboid ring = getRing();
        if (ring == null || event.getPlayer().isOp()) {
            return;
        }
        if (event.getBlock().getWorld().equals(ring.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (currentMatch != null && currentMatch.has(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (currentMatch != null && currentMatch.has(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (currentMatch != null && currentMatch.has(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ----------------------------------------------------------------
    // Arena configuration
    // ----------------------------------------------------------------

    public boolean isArenaConfigured() {
        return getRing() != null && getSpawn1() != null && getSpawn2() != null && getExit() != null;
    }

    public void setRingCorner1(Location loc) {
        saveLocation("ring.corner1", loc, false);
    }

    public void setRingCorner2(Location loc) {
        saveLocation("ring.corner2", loc, false);
    }

    public void setSpawn1(Location loc) {
        saveLocation("spawn1", loc, true);
    }

    public void setSpawn2(Location loc) {
        saveLocation("spawn2", loc, true);
    }

    public void setExit(Location loc) {
        saveLocation("exit", loc, true);
    }

    public void setHideout(Location loc) {
        saveLocation("hideout", loc, true);
    }

    public Location getHideout() {
        return loadLocation("hideout", true);
    }

    public void reloadArenaConfig() {
        plugin.reloadConfig();
    }

    private Cuboid getRing() {
        Location c1 = loadLocation("ring.corner1", false);
        Location c2 = loadLocation("ring.corner2", false);
        if (c1 == null || c2 == null) {
            return null;
        }
        if (!c1.getWorld().equals(c2.getWorld())) {
            return null;
        }
        return new Cuboid(c1, c2);
    }

    private Location getSpawn1() {
        return loadLocation("spawn1", true);
    }

    private Location getSpawn2() {
        return loadLocation("spawn2", true);
    }

    private Location getExit() {
        return loadLocation("exit", true);
    }

    private Location loadLocation(String path, boolean withDirection) {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = cfg.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = withDirection ? (float) cfg.getDouble(path + ".yaw") : 0f;
        float pitch = withDirection ? (float) cfg.getDouble(path + ".pitch") : 0f;
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocation(String path, Location loc, boolean withDirection) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        if (withDirection) {
            cfg.set(path + ".yaw", (double) loc.getYaw());
            cfg.set(path + ".pitch", (double) loc.getPitch());
        }
        plugin.saveConfig();
    }

    // ----------------------------------------------------------------
    // Internal state holders
    // ----------------------------------------------------------------

    private static class Match {
        final UUID player1;
        final UUID player2;

        Match(UUID player1, UUID player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        boolean has(UUID uuid) {
            return uuid.equals(player1) || uuid.equals(player2);
        }

        UUID other(UUID uuid) {
            return uuid.equals(player1) ? player2 : player1;
        }
    }

    private static class PlayerSnapshot {
        final ItemStack[] inventoryContents;
        final ItemStack[] armorContents;
        final ItemStack offHand;
        final GameMode gameMode;
        final Location returnLocation;

        PlayerSnapshot(Player player) {
            this.inventoryContents = cloneArray(player.getInventory().getContents());
            this.armorContents = cloneArray(player.getInventory().getArmorContents());
            this.offHand = player.getInventory().getItemInOffHand().clone();
            this.gameMode = player.getGameMode();
            this.returnLocation = player.getLocation().clone();
        }

        void restore(Player player) {
            PlayerInventory inv = player.getInventory();
            inv.setContents(inventoryContents);
            inv.setArmorContents(armorContents);
            inv.setItemInOffHand(offHand);
            player.setGameMode(gameMode);
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
