package com.lucasmuhr.minigames;

import com.lucasmuhr.minigames.boxing.BoxingCommand;
import com.lucasmuhr.minigames.boxing.BoxingManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MinigamesPlugin extends JavaPlugin {

    private BoxingManager boxingManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        boxingManager = new BoxingManager(this);
        getServer().getPluginManager().registerEvents(boxingManager, this);

        BoxingCommand boxingCommand = new BoxingCommand(boxingManager);
        PluginCommand boxCommand = getCommand("box");
        if (boxCommand != null) {
            boxCommand.setExecutor(boxingCommand);
            boxCommand.setTabCompleter(boxingCommand);
        } else {
            getLogger().warning("Could not register /box - check that plugin.yml was packaged correctly.");
        }

        getLogger().info("SMP Minigames enabled - boxing is ready.");
    }

    @Override
    public void onDisable() {
        if (boxingManager != null) {
            boxingManager.shutdown();
        }
    }
}
