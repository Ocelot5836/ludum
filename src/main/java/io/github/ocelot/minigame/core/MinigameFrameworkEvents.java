package io.github.ocelot.minigame.core;

import io.github.ocelot.minigame.MinigameFramework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MinigameFrameworkEvents implements Listener
{
    @EventHandler
    public void onEvent(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        if (MinigameFramework.getInstance().getMinigameManager().getRunningGame(player.getWorld().getUID()).isPresent())
        {
            player.teleport(MinigameFramework.getInstance().getOverworld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onEvent(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        MinigameFramework.getInstance().getMinigameManager().getRunningGame(player.getWorld().getUID()).ifPresent(game -> game.removePlayer(player));
    }
}
