package io.github.ocelot.minigame;

import co.aikar.commands.PaperCommandManager;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.ocelot.minigame.api.MinigameManager;
import io.github.ocelot.minigame.api.MinigameRegistry;
import io.github.ocelot.minigame.core.MinigameFrameworkEvents;
import io.github.ocelot.minigame.core.TestMinigame;
import io.github.ocelot.minigame.core.command.MinigameCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class MinigameFramework extends JavaPlugin
{
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static MinigameFramework instance;

    private World overworld;
    private Executor mainExecutor;
    private ExecutorService backgroundExecutor;
    private MinigameManager minigameManager;

    public MinigameFramework()
    {
    }

    @Override
    public void onEnable()
    {
        MinigameFramework.instance = this;

        MinigameRegistry.flush();
        MinigameRegistry.register(NamespacedKey.fromString("test", this), TestMinigame::new);

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
                            getLog4JLogger().warn("{} died", this.getName(), e);
                        }
                        else
                        {
                            getLog4JLogger().debug("{} shutdown", this.getName());
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
                getLog4JLogger().error(String.format("Caught exception in thread %s", t), e);
            }, true);
        }

        this.minigameManager = new MinigameManager();

        Path minigamesFolder = MinigameFramework.getInstance().getDataFolder().toPath().resolve("minigames");
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
                    this.getLog4JLogger().error("Failed to create minigames folder stub", e);
                }
            });
        }

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new MinigameCommand());
        commandManager.getCommandCompletions().registerCompletion("games", context -> MinigameRegistry.getKeys().stream().map(NamespacedKey::asString).collect(Collectors.toUnmodifiableSet()));
        commandManager.getCommandCompletions().registerCompletion("names", context -> this.minigameManager.getRunningGames());

        this.getServer().getPluginManager().registerEvents(new MinigameFrameworkEvents(), this);

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
                this.getLog4JLogger().error("Failed to shut down background executor after 10 seconds");
        }
        catch (InterruptedException e)
        {
            this.getLog4JLogger().error("Failed to shut down background executor", e);
        }
        this.backgroundExecutor = null;
        WORKER_COUNT.set(1);

        Bukkit.getLogger().info(ChatColor.RED + "Disabled Starter!");
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
    public static MinigameFramework getInstance()
    {
        return instance;
    }
}
