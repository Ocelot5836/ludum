package io.github.ocelot.ludum.api;

import io.github.ocelot.ludum.Ludum;
import io.github.ocelot.ludum.core.DefaultMinigameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * <p>A basic minigame in its own world.</p>
 *
 * @author Ocelot
 */
public abstract class Minigame
{
    private final String worldName;
    private World world;
    private MinigameState state;

    public Minigame(String world)
    {
        this.worldName = world;
        this.state = new DefaultMinigameState(this);
    }

    /**
     * Called when the minigame is first created in a new world.
     */
    public void init()
    {
        this.state.init();
    }

    /**
     * Called when the server ticks.
     */
    public void tick()
    {
        this.state.tick();
    }

    /**
     * Called just before all players are removed from the minigame when closing.
     */
    public void close()
    {
        this.state.close();
    }

    /**
     * Stops executing this minigame and moves all players back to the main world.
     */
    public void stop()
    {
        if (this.world != null)
        {
            MinigameManager manager = Ludum.getInstance().getMinigameManager();
            manager.getRunningGame(this.world.getUID()).ifPresent(game -> manager.stop(game.getName()));
        }
    }

    /**
     * Called when the specified player is added to the world.
     *
     * @param player The player being added
     */
    public void addPlayer(Player player)
    {
    }

    /**
     * Called when the specified player is removed from the world.
     *
     * @param player The player being removed
     */
    public void removePlayer(Player player)
    {
    }

    /**
     * Checks to see if the specified player can join this world.
     *
     * @param player The player to check
     * @return Whether that player can join
     */
    public boolean canJoin(Player player)
    {
        return true;
    }

    /**
     * Retrieves the position a player should be in when joining the world.
     *
     * @param player The player joining
     * @return The location to place that player
     */
    public Location positionJoiningPlayer(Player player)
    {
        return player.getLocation();
    }

    /**
     * @return The name of the world file to load
     */
    public String getWorldName()
    {
        return worldName;
    }

    /**
     * @return The world this minigame takes place in
     */
    public World getWorld()
    {
        return world;
    }

    /**
     * @return The current minigame state instance
     */
    public MinigameState getState()
    {
        return state;
    }

    /**
     * Changes the current minigame state and sets it up.
     *
     * @param state The new state or <code>null</code> to use a default state
     */
    public void setState(@Nullable MinigameState state)
    {
        this.state.close();
        this.state = state != null ? state : new DefaultMinigameState(this);
        this.state.init();
    }

    /**
     * Sets the world for this minigame.
     *
     * @param world The new world instance
     */
    public final void setWorld(World world)
    {
        this.world = world;
    }
}
