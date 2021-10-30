package io.github.ocelot.ludum.core;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import io.github.ocelot.ludum.Ludum;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class LudumEvents implements Listener
{
    @EventHandler
    public void onEvent(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        if (Ludum.getInstance().getMinigameManager().getRunningGame(player.getWorld().getUID()).isPresent())
        {
            player.teleport(Ludum.getInstance().getOverworld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onEvent(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        Ludum.getInstance().getMinigameManager().getRunningGame(player.getWorld().getUID()).ifPresent(game -> game.removePlayer(player));
    }

    @EventHandler
    public void onEvent(ServerTickStartEvent event)
    {
        Ludum.getInstance().getMinigameManager().tick();
    }
}
