package io.github.ocelot.ludum;

import co.aikar.commands.PaperCommandManager;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.ocelot.ludum.api.MinigameManager;
import io.github.ocelot.ludum.api.MinigameRegistry;
import io.github.ocelot.ludum.core.LudumEvents;
import io.github.ocelot.ludum.core.command.MinigameCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>A system designed for creating new worlds with individual minigames running in them.</p>
 * <p>To create a new minigame, use {@link MinigameRegistry#register(NamespacedKey, Supplier)} in {@link Plugin#onEnable()}.</p>
 *
 * @author Ocelot
 */
public class Ludum extends JavaPlugin
{
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static Ludum instance;

    private World overworld;
    private Executor mainExecutor;
    private ExecutorService backgroundExecutor;
    private MinigameManager minigameManager;

    public Ludum()
    {
    }

    @Override
    public void onEnable()
    {
        Ludum.instance = this;

        MinigameRegistry.flush();

        this.overworld = Bukkit.getWorlds().get(0);

        this.mainExecutor = Bukkit.getScheduler().getMainThreadExecutor(this);
        int i = Math.min(Runtime.getRuntime().availableProcessors() - 1, 7);
        if (i <= 0)
        {
            this.backgroundExecutor = MoreExecutors.newDirectExecutorService();
        }
        else
        {
            this.backgroundExecutor = new ForkJoinPool(i, task ->
            {
                ForkJoinWorkerThread forkjoinworkerthread = new ForkJoinWorkerThread(task)
                {
                    @Override
                    protected void onTermination(Throwable e)
                    {
                        if (e != null)
                        {
                            Bukkit.getLogger().warning(this.getName() + " died");
                            e.printStackTrace();
                        }

                        super.onTermination(e);
                    }
                };
                forkjoinworkerthread.setName("Worker-Minigame-" + WORKER_COUNT.getAndIncrement());
                return forkjoinworkerthread;
            }, (t, e) ->
            {
                if (e instanceof CompletionException)
                    e = e.getCause();
                e.printStackTrace();
            }, true);
        }

        this.minigameManager = new MinigameManager();

        Path minigamesFolder = Ludum.getInstance().getDataFolder().toPath().resolve("minigames");
        if (!Files.exists(minigamesFolder))
        {
            this.backgroundExecutor.execute(() ->
            {
                try
                {
                    Files.createDirectories(minigamesFolder);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
        }

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new MinigameCommand());
        commandManager.getCommandCompletions().registerCompletion("games", context -> MinigameRegistry.getKeys().stream().map(NamespacedKey::asString).collect(Collectors.toUnmodifiableSet()));
        commandManager.getCommandCompletions().registerCompletion("names", context -> this.minigameManager.getRunningGames());

        this.getServer().getPluginManager().registerEvents(new LudumEvents(), this);

        Bukkit.getLogger().info(ChatColor.GREEN + "Enabled Starter!");
    }

    @Override
    public void onDisable()
    {
        this.minigameManager.close();
        this.minigameManager = null;
        this.mainExecutor = null;
        this.backgroundExecutor.shutdown();
        try
        {
            if (!this.backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS))
                Bukkit.getLogger().warning("Failed to shut down background executor after 10 seconds");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        this.backgroundExecutor = null;
        WORKER_COUNT.set(1);
    }

    /**
     * @return The manager for all minigames
     */
    public MinigameManager getMinigameManager()
    {
        return minigameManager;
    }

    @ApiStatus.Internal
    public Executor getMainExecutor()
    {
        return mainExecutor;
    }

    @ApiStatus.Internal
    public Executor getBackgroundExecutor()
    {
        return backgroundExecutor;
    }

    @ApiStatus.Internal
    public World getOverworld()
    {
        return overworld;
    }

    /**
     * @return The static instance of the plugin
     */
    public static Ludum getInstance()
    {
        return instance;
    }
}
