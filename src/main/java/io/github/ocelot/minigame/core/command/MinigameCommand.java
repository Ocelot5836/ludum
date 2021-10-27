package io.github.ocelot.minigame.core.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.ocelot.minigame.MinigameFramework;
import io.github.ocelot.minigame.api.MinigameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@CommandPermission("minigame")
@CommandAlias("minigame")
public class MinigameCommand extends BaseCommand
{
    @Subcommand("start")
    @Syntax("<game> <name>")
    @CommandCompletion("@games")
    @Description("Starts a new minigame in a server")
    public static void onStart(Player player, @Values("@games") NamespacedKey game, @Single String name)
    {
        MinigameFramework.getInstance().getMinigameManager().start(name, game).handleAsync((value, e) ->
        {
            if (e != null)
            {
                e.printStackTrace();
                player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            }
            else
            {
                player.sendMessage(Component.text("Created " + name + " running minigame: " + game));
            }
            return value;
        }, MinigameFramework.getInstance().getMainExecutor());
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
            MinigameManager.RunningGame game = MinigameFramework.getInstance().getMinigameManager().getRunningGame(name).orElseThrow(() -> new CommandException("Unknown game server: " + name));
            if (!game.addPlayer(target != null ? target : player))
                throw new CommandException("Minigame does not accept players: " + name);
        }
        catch (Exception e)
        {
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Subcommand("leave")
    @Syntax("[target]")
    @Description("Leaves a minigame if in one")
    public static void onLeave(Player player, @Optional Player target)
    {
        try
        {
            Player p = target != null ? target : player;
            MinigameManager.RunningGame game = MinigameFramework.getInstance().getMinigameManager().getRunningGame(p.getWorld().getUID()).orElseThrow(() -> new CommandException("Player is not in minigame"));
            game.removePlayer(target != null ? target : player);
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
        if (MinigameFramework.getInstance().getMinigameManager().getRunningGames().isEmpty())
        {
            player.sendMessage(Component.text("No Servers Running"));
        }
        else
        {
            player.sendMessage(Component.text("Current Servers: " + String.join(", ", MinigameFramework.getInstance().getMinigameManager().getRunningGames())));
        }
    }
}
