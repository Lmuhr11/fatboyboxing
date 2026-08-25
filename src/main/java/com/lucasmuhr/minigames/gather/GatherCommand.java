package com.lucasmuhr.minigames.gather;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class GatherCommand implements CommandExecutor {

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

        sender.sendMessage(ChatColor.GOLD + "--- Gather ---");
        sender.sendMessage(ChatColor.YELLOW + "/mg sendall <world> " + ChatColor.GRAY
                + "- teleport every online player to <world>, remembering where they were");
        sender.sendMessage(ChatColor.YELLOW + "/mg returnall " + ChatColor.GRAY
                + "- teleport everyone sent away back to their remembered spot");
        return true;
    }
}
