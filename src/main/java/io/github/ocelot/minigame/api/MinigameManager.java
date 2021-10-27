package io.github.ocelot.minigame.api;

import io.github.ocelot.minigame.MinigameFramework;
import org.bukkit.*;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <p>Manages all minigames currently running.</p>
 *
 * @author Ocelot
 */
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
            {
                this.usedIds.add(id);
                return id;
            }
            id++;
        }
    }

    private CompletableFuture<?> loadWorld(String name, String output)
    {
        return CompletableFuture.runAsync(() ->
        {
            try
            {
                Path dst = Paths.get(output);
                if (Files.exists(dst))
                    deleteRecursive(dst);
                Files.createDirectories(dst);

                Path src = MinigameFramework.getInstance().getDataFolder().toPath().resolve("minigames/" + name);
                if (Files.isDirectory(src))
                {
                    Files.walkFileTree(src, new SimpleFileVisitor<>()
                    {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                        {
                            Files.createDirectories(dst.resolve(src.relativize(dir)));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                        {
                            Files.copy(file, dst.resolve(src.relativize(file)));
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    return;
                }

                try (ZipInputStream zip = new ZipInputStream(new FileInputStream(MinigameFramework.getInstance().getDataFolder().toPath().resolve("minigames/" + name + ".zip").toFile())))
                {
                    ZipEntry entry = zip.getNextEntry();
                    while (entry != null)
                    {
                        Path filePath = dst.resolve(entry.getName());
                        if (entry.isDirectory())
                        {
                            Files.createDirectories(filePath);
                        }
                        else
                        {
                            Files.createFile(filePath);
                            try (FileOutputStream os = new FileOutputStream(filePath.toFile()))
                            {
                                byte[] buffer = new byte[4096];
                                int read;
                                while ((read = zip.read(buffer)) != -1)
                                {
                                    os.write(buffer, 0, read);
                                }
                            }
                        }

                        zip.closeEntry();
                        entry = zip.getNextEntry();
                    }
                }
            }
            catch (IOException e)
            {
                throw new CompletionException("Failed to load world ZIP: " + name + ".zip", e);
            }
        }, MinigameFramework.getInstance().getBackgroundExecutor());
    }

    /**
     * Starts a new minigame world.
     *
     * @param name         The minigame world key
     * @param minigameName The name of the minigame to host
     * @return A future for when the game starts running
     */
    public synchronized CompletableFuture<RunningGame> start(String name, NamespacedKey minigameName)
    {
        if (this.runningGames.containsKey(name))
            throw new CommandException("Minigame server already exists");
        this.runningGames.put(name, CREATING);

        Minigame minigame = MinigameRegistry.create(minigameName);
        Executor executor = MinigameFramework.getInstance().getMainExecutor();
        int id = this.nextId();
        return this.loadWorld(minigame.getWorldName(), "mini" + id).thenApplyAsync(__ ->
        {
            World world = Bukkit.createWorld(new WorldCreator("mini" + id, new NamespacedKey(MinigameFramework.getInstance(), "mini" + id)));
            if (world == null)
                throw new CommandException("Failed to create minigame server");
            RunningGame game = new RunningGame(minigame, world, id);
            this.runningGames.put(name, game);
            return game;
        }, executor).exceptionallyAsync(e ->
        {
            this.runningGames.remove(name);
            this.usedIds.remove(id);
            throw new CompletionException(e);
        }, executor);
    }

    /**
     * Stops the minigame with the specified id.
     *
     * @param name The id of the minigame to stop
     */
    public synchronized void stop(String name)
    {
        RunningGame game = this.runningGames.get(name);
        if (game == null || game == CREATING)
            throw new CommandException("Unknown server: " + name);
        game.close();
        this.runningGames.remove(name, game);
        this.usedIds.remove(game.id);
    }

    /**
     * Stops all minigames and clears all worlds.
     */
    public synchronized void close()
    {
        this.runningGames.values().forEach(RunningGame::close);
        this.runningGames.clear();
        this.usedIds.clear();
    }

    /**
     * Retrieves a running game by the server name.
     *
     * @param name The name of the server running
     * @return The game with that name
     */
    public synchronized Optional<RunningGame> getRunningGame(String name)
    {
        return Optional.ofNullable(this.runningGames.get(name));
    }

    /**
     * Retrieves a running game by the world id.
     *
     * @param worldId The id of the world on the running server
     * @return The game with that world
     */
    public synchronized Optional<RunningGame> getRunningGame(UUID worldId)
    {
        return this.runningGames.values().stream().filter(game -> game.world != null && game.world.getUID().equals(worldId)).findFirst();
    }

    /**
     * @return A set of all running games
     */
    public synchronized Set<String> getRunningGames()
    {
        return this.runningGames.keySet();
    }

    /**
     * <p>A minigame that is currently running.</p>
     *
     * @author Ocelot
     */
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
            this.game.setWorld(world);
            this.game.init();
        }

        protected void close()
        {
            MinigameFramework framework = MinigameFramework.getInstance();
            Location spawn = framework.getOverworld().getSpawnLocation();

            this.game.close();
            CompletableFuture.allOf(this.world.getPlayers().stream().map(player -> player.teleportAsync(spawn)).toArray(CompletableFuture[]::new)).thenRunAsync(() ->
            {
                Path folder = this.world.getWorldFolder().toPath();
                Bukkit.unloadWorld(this.world, false);
                framework.getBackgroundExecutor().execute(() -> deleteRecursive(folder));
            }, framework.getMainExecutor());
        }

        /**
         * Adds the specifiied player to the minigame world.
         *
         * @param player The player to add
         * @return Whether that player was able to be added
         */
        public boolean addPlayer(Player player)
        {
            if (!this.game.canJoin(player))
                return false;

            Location location = this.game.positionJoiningPlayer(player);
            location.add(0.5, 0, 0.5);
            location.setWorld(this.world);
            player.teleportAsync(location, PlayerTeleportEvent.TeleportCause.COMMAND).thenRunAsync(() -> this.game.addPlayer(player), MinigameFramework.getInstance().getMainExecutor());
            return true;
        }

        /**
         * Removes the specified player from this game.
         *
         * @param player The player to remove
         */
        public void removePlayer(Player player)
        {
            if (!this.world.getPlayers().contains(player))
                return;
            this.game.removePlayer(player);
            player.teleportAsync(MinigameFramework.getInstance().getOverworld().getSpawnLocation());
        }
    }

    private static void deleteRecursive(Path folder)
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
    }
}
