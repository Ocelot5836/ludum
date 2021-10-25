package io.github.ocelot.minigame.minigame;

import io.github.ocelot.minigame.MinigameFramework;
import org.bukkit.*;
import org.bukkit.command.CommandException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MinigameManager
{
    private static final RunningGame CREATING = new RunningGame()
    {
        @Override
        protected void close()
        {
        }
    };

    private final Map<String, RunningGame> runningGames;
    private final Set<Integer> usedIds;

    public MinigameManager()
    {
        this.runningGames = new HashMap<>();
        this.usedIds = new HashSet<>();
    }

    private int nextId()
    {
        int id = 0;
        while (true)
        {
            if (!this.usedIds.contains(id))
                return id;
            id++;
        }
    }

    public synchronized RunningGame start(String name, Minigame minigame)
    {
        if (this.runningGames.containsKey(name))
            throw new CommandException("Minigame server already exists");
        this.runningGames.put(name, CREATING);

        try
        {
            int id = this.nextId();
            World world = Bukkit.createWorld(new WorldCreator("mini" + id, new NamespacedKey(MinigameFramework.getInstance(), "mini" + id)));
            if (world == null)
                throw new CommandException("Failed to create minigame server");
            this.usedIds.add(id);
            RunningGame game = new RunningGame(minigame, world, id);
            this.runningGames.put(name, game);
            return game;
        }
        catch (Throwable t)
        {
            this.runningGames.remove(name);
            throw t;
        }
    }

    public synchronized void stop(String name)
    {
        RunningGame game = this.runningGames.get(name);
        if (game == null || game == CREATING)
            throw new CommandException("Unknown server: " + name);
        game.close();
        this.runningGames.remove(name, game);
        this.usedIds.remove(game.id);
    }

    public synchronized void close()
    {
        this.runningGames.values().forEach(RunningGame::close);
        this.runningGames.clear();
        this.usedIds.clear();
    }

    public synchronized RunningGame getRunningGame(String name)
    {
        return this.runningGames.get(name);
    }

    public synchronized Set<String> getRunningGames()
    {
        return this.runningGames.keySet();
    }

    public static class RunningGame
    {
        private final Minigame game;
        private final World world;
        private final int id;

        private RunningGame()
        {
            this.game = null;
            this.world = null;
            this.id = -1;
        }

        private RunningGame(Minigame game, World world, int id)
        {
            this.game = game;
            this.world = world;
            this.id = id;
            this.game.init(world);
        }

        protected void close()
        {
            this.game.close(this.world);
            this.world.getPlayers().forEach(player -> player.teleport(new Location(Bukkit.getWorld(NamespacedKey.minecraft("overworld")), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ())));
            Path folder = this.world.getWorldFolder().toPath();
            Bukkit.unloadWorld(this.world, false);
            Bukkit.getScheduler().runTaskAsynchronously(MinigameFramework.getInstance(), () ->
            {
                if (!Files.exists(folder))
                    return;
                try
                {
                    Path[] paths = Files.walk(folder).sorted(Comparator.reverseOrder()).toArray(Path[]::new);
                    for (Path path : paths)
                        Files.delete(path);
                }
                catch (Exception e)
                {
                    MinigameFramework.getInstance().getLog4JLogger().error("Failed to delete " + folder, e);
                }
                MinigameFramework.getInstance().getLog4JLogger().debug("Deleted " + folder);
            });
        }

        public Minigame getGame()
        {
            return game;
        }

        public World getWorld()
        {
            return world;
        }
    }
}
