package com.lucasmuhr.minigames.gather;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GatherCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("sendall", "returnall", "setspawn");

    private final GatherManager manager;

    public GatherCommand(GatherManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minigames.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("sendall")) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "No world named '" + args[1] + "' is loaded.");
                return true;
            }
            int moved = manager.sendAll(world);
            sender.sendMessage(ChatColor.GREEN + "Sent " + moved + " player(s) to " + world.getName() + ".");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("returnall")) {
            int moved = manager.returnAll();
            sender.sendMessage(ChatColor.GREEN + "Returned " + moved + " player(s) to where they were.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setspawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can run this command.");
                return true;
            }
            manager.setSpawn(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Gather spawn for " + player.getWorld().getName()
                    + " set to your current location.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Gather ---");
        sender.sendMessage(ChatColor.YELLOW + "/mg sendall <world> " + ChatColor.GRAY
                + "- teleport every online player to <world>, remembering where they were");
        sender.sendMessage(ChatColor.YELLOW + "/mg returnall " + ChatColor.GRAY
                + "- teleport everyone sent away back to their remembered spot");
        sender.sendMessage(ChatColor.YELLOW + "/mg setspawn " + ChatColor.GRAY
                + "- set the gather point for the world you're standing in (used by /mg sendall)");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    matches.add(sub);
                }
            }
            return matches;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sendall")) {
            List<String> matches = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    matches.add(world.getName());
                }
            }
            return matches;
        }
        return new ArrayList<>();
    }
}
