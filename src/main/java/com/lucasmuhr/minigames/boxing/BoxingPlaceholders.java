package com.lucasmuhr.minigames.boxing;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * Exposes %boxing_wins% to PlaceholderAPI - the number of boxing matches a
 * player has won by knocking their opponent out inside the ring. Only ever
 * incremented by BoxingManager on an actual in-ring kill, never a forfeit.
 * Feed this placeholder into a leaderboard plugin like ajLeaderboards and a
 * hologram plugin like DecentHolograms to build a "top boxers" display.
 */
public class BoxingPlaceholders extends PlaceholderExpansion {

    private final BoxingManager manager;

    public BoxingPlaceholders(BoxingManager manager) {
        this.manager = manager;
    }

    @Override
    public String getIdentifier() {
        return "boxing";
    }

    @Override
    public String getAuthor() {
        return "lucasmuhr";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !params.equalsIgnoreCase("wins")) {
            return null;
        }
        return String.valueOf(manager.getWins(player.getUniqueId()));
    }
}
