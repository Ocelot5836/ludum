package io.github.ocelot.minigame.minigame;

import org.bukkit.World;

public interface Minigame
{
    void init(World world);

    void close(World world);
}
