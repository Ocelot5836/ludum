package io.github.ocelot.minigame.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.ocelot.minigame.MinigameFramework;
import io.github.ocelot.minigame.minigame.Minigame;
import io.github.ocelot.minigame.minigame.MinigameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

@CommandAlias("minigame")
public class MinigameCommand extends BaseCommand
{
    @Subcommand("start")
    @Syntax("<game> <name>")
    @CommandCompletion("@games")
    @Description("Starts a new minigame in a server")
    public static void onStart(Player player, @Values("@games") String game, @Single String name)
    {
        try
        {
            MinigameFramework.getInstance().getMinigameManager().start(name, new Minigame()
            {
                @Override
                public void init(World world)
                {
                    MinigameFramework.getInstance().getLog4JLogger().info("Game start");
                }

                @Override
                public void close(World world)
                {
                    MinigameFramework.getInstance().getLog4JLogger().info("Game stop");
                }
            });
            player.sendMessage(Component.text("Created " + name + " running minigame: " + game));
        }
        catch (Exception e)
        {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Subcommand("stop")
    @Syntax("<name>")
    @CommandCompletion("@names")
    @Description("Stops a running minigame server")
    public static void onStop(Player player, @Single @Values("@names") String name)
    {
        try
        {
            MinigameFramework.getInstance().getMinigameManager().stop(name);
            player.sendMessage(Component.text("Stopped " + name));
        }
        catch (Exception e)
        {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Subcommand("join")
    @Syntax("<name> [target]")
    @CommandCompletion("@names")
    @Description("Joins a running minigame server")
    public static void onJoin(Player player, @Single @Values("@names") String name, @Optional Player target)
    {
        try
        {
            MinigameManager.RunningGame game = MinigameFramework.getInstance().getMinigameManager().getRunningGame(name);
            if (game == null)
                throw new CommandException("Unknown game server: " + name);

            if (target != null)
            {
                target.teleportAsync(new Location(game.getWorld(), 0, 256, 0), PlayerTeleportEvent.TeleportCause.COMMAND).thenRun(() -> player.sendMessage(Component.text("Sent " + target.getName() + " to " + name)));
            }
            else
            {
                player.teleportAsync(new Location(game.getWorld(), 0, 256, 0), PlayerTeleportEvent.TeleportCause.COMMAND).thenRun(() -> player.sendMessage(Component.text("Teleported to " + name)));
            }
        }
        catch (Exception e)
        {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Subcommand("list")
    @Description("Lists all running minigame servers")
    public static void onList(@Single Player player)
    {
        player.sendMessage(Component.text("Current Servers: " + String.join(", ", MinigameFramework.getInstance().getMinigameManager().getRunningGames())));
    }
}
