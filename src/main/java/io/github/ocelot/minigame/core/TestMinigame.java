package io.github.ocelot.minigame.core;

import io.github.ocelot.minigame.api.Minigame;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TestMinigame extends Minigame
{
    public TestMinigame()
    {
        super("test");
    }

    @Override
    public void init()
    {
        this.logger.info("Game start");
    }

    @Override
    public void close()
    {
        this.logger.info("Game stop");
    }

    @Override
    public Location positionJoiningPlayer(Player player)
    {
        return new Location(null, 250, 60, 250);
    }
}
