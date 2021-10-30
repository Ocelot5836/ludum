package io.github.ocelot.minigame.core;

import io.github.ocelot.minigame.api.Minigame;
import io.github.ocelot.minigame.api.MinigameState;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class DefaultMinigameState extends MinigameState
{
    public DefaultMinigameState(Minigame minigame)
    {
        super(minigame);
    }

    @Override
    public void init()
    {
    }

    @Override
    public void tick()
    {
    }

    @Override
    public void close()
    {
    }
}
