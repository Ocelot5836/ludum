package io.github.ocelot.minigame.api;

import io.github.ocelot.minigame.MinigameFramework;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * <p>A basic minigame in its own world.</p>
 *
 * @author Ocelot
 */
public abstract class Minigame
{
    protected final Logger logger;
    private final String worldName;
    private World world;

    public Minigame(String world)
    {
        this.worldName = world;
        this.logger = MinigameFramework.getInstance().getLog4JLogger();
    }

    /**
     * Called when the minigame is first created in a new world.
     */
    public void init()
    {
    }

    /**
     * Called just before all players are removed from the minigame when closing.
     */
    public void close()
    {
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
     * Sets the world for this minigame.
     *
     * @param world The new world instance
     */
    public final void setWorld(World world)
    {
        this.world = world;
    }
}
