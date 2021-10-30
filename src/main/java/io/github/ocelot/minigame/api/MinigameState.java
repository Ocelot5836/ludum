package io.github.ocelot.minigame.api;

import io.github.ocelot.minigame.MinigameFramework;
import org.apache.logging.log4j.Logger;

/**
 * <p>A single state for a minigame currently running.</p>
 *
 * @author Ocelot
 */
public abstract class MinigameState
{
    protected final Logger logger;
    protected final Minigame minigame;

    public MinigameState(Minigame minigame)
    {
        this.logger = MinigameFramework.getInstance().getLog4JLogger();
        this.minigame = minigame;
    }

    /**
     * Called when {@link Minigame#init()} is called, or when this state is newly set.
     */
    public abstract void init();

    /**
     * Called at the stat of each tick to update this state.
     */
    public abstract void tick();

    /**
     * Cleans up any resources used by this state.
     */
    public abstract void close();
}
