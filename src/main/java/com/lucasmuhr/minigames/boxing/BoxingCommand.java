package com.lucasmuhr.minigames.boxing;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoxingCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "join", "leave", "setring1", "setring2", "setspawn1", "setspawn2", "setexit",
            "setboxingadmin", "boxingadmin", "reload"
    );

    private final BoxingManager manager;

    public BoxingCommand(BoxingManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("join")) {
            if (!sender.hasPermission("minigames.box.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /box join <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "No player named '" + args[1] + "' is online.");
                return true;
            }
            String error = manager.joinQueue(target);
            if (error != null) {
                sender.sendMessage(ChatColor.RED + error);
            }
            return true;
        }

        if (sub.equals("leave")) {
            if (!sender.hasPermission("minigames.box.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /box leave <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "No player named '" + args[1] + "' is online.");
                return true;
            }
            String error = manager.leaveQueue(target);
            if (error != null) {
                sender.sendMessage(ChatColor.RED + error);
            } else {
                sender.sendMessage(ChatColor.GREEN + target.getName() + " removed from the boxing queue.");
            }
            return true;
        }

        if (sub.equals("setboxingadmin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can run this command.");
                return true;
            }
            if (!player.hasPermission("minigames.box.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            manager.setBoxingAdmin(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Boxing admin spot set to your current location - only you can teleport there with /box boxingadmin.");
            return true;
        }

        if (sub.equals("boxingadmin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can run this command.");
                return true;
            }
            if (!player.hasPermission("minigames.box.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            Location adminSpot = manager.getBoxingAdmin();
            if (adminSpot == null) {
                player.sendMessage(ChatColor.RED + "No boxing admin spot set yet - run /box setboxingadmin while standing where you want it.");
                return true;
            }
            player.teleport(adminSpot);
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("minigames.box.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            manager.reloadArenaConfig();
            sender.sendMessage(ChatColor.GREEN + "Boxing arena config reloaded.");
            return true;
        }

        if (isSetterCommand(sub)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can run this command.");
                return true;
            }
            if (!player.hasPermission("minigames.box.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            runSetter(player, sub);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private boolean isSetterCommand(String sub) {
        return sub.equals("setring1") || sub.equals("setring2")
                || sub.equals("setspawn1") || sub.equals("setspawn2")
                || sub.equals("setexit");
    }

    private void runSetter(Player player, String sub) {
        String label;
        switch (sub) {
            case "setring1":
                manager.setRingCorner1(player.getLocation());
                label = "ring corner 1";
                break;
            case "setring2":
                manager.setRingCorner2(player.getLocation());
                label = "ring corner 2";
                break;
            case "setspawn1":
                manager.setSpawn1(player.getLocation());
                label = "fighter 1 spawn point";
                break;
            case "setspawn2":
                manager.setSpawn2(player.getLocation());
                label = "fighter 2 spawn point";
                break;
            case "setexit":
                manager.setExit(player.getLocation());
                label = "exit point";
                break;
            default:
                label = sub;
                break;
        }
        player.sendMessage(ChatColor.GREEN + "Boxing " + label + " set to your current location.");
    }

    private void sendHelp(CommandSender sender) {
        if (!sender.hasPermission("minigames.box.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "Boxing matches are started by staff - ask an op to add you.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "--- Boxing ---");
        sender.sendMessage(ChatColor.YELLOW + "/box join <player> " + ChatColor.GRAY + "- add a player to the boxing queue");
        sender.sendMessage(ChatColor.YELLOW + "/box leave <player> " + ChatColor.GRAY + "- remove a player from the boxing queue");
        sender.sendMessage(ChatColor.YELLOW + "/box setring1 " + ChatColor.GRAY
                + "- set ring corner 1 (no PvP allowed outside these two corners)");
        sender.sendMessage(ChatColor.YELLOW + "/box setring2 " + ChatColor.GRAY + "- set ring corner 2");
        sender.sendMessage(ChatColor.YELLOW + "/box setspawn1 " + ChatColor.GRAY + "- set fighter 1's starting spot");
        sender.sendMessage(ChatColor.YELLOW + "/box setspawn2 " + ChatColor.GRAY + "- set fighter 2's starting spot");
        sender.sendMessage(ChatColor.YELLOW + "/box setexit " + ChatColor.GRAY + "- set where fighters go after the match");
        sender.sendMessage(ChatColor.YELLOW + "/box setboxingadmin " + ChatColor.GRAY + "- save your current spot as your personal boxing admin spot");
        sender.sendMessage(ChatColor.YELLOW + "/box boxingadmin " + ChatColor.GRAY + "- teleport only yourself to your boxing admin spot");
        sender.sendMessage(ChatColor.YELLOW + "/box reload " + ChatColor.GRAY + "- reload the arena config from disk");
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
        if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave"))) {
            List<String> matches = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    matches.add(online.getName());
                }
            }
            return matches;
        }
        return new ArrayList<>();
    }
}
