package io.github.ocelot.minigame;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import io.github.ocelot.minigame.command.MinigameCommand;
import io.github.ocelot.minigame.minigame.MinigameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class MinigameFramework extends JavaPlugin
{
    private static MinigameFramework instance;

    private MinigameManager minigameManager;

    public MinigameFramework()
    {
    }

    @Override
    public void onEnable()
    {
        MinigameFramework.instance = this;

        this.minigameManager = new MinigameManager();

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new MinigameCommand());
        commandManager.getCommandCompletions().registerCompletion("games", context -> ImmutableList.of("test"));
        commandManager.getCommandCompletions().registerCompletion("names", context -> this.minigameManager.getRunningGames());

        Bukkit.getLogger().info(ChatColor.GREEN + "Enabled Starter!");
    }

    @Override
    public void onDisable()
    {
        this.minigameManager.close();
        this.minigameManager = null;

        Bukkit.getLogger().info(ChatColor.RED + "Disabled Starter!");
    }

    public MinigameManager getMinigameManager()
    {
        return minigameManager;
    }

    public static MinigameFramework getInstance()
    {
        return instance;
    }
}
