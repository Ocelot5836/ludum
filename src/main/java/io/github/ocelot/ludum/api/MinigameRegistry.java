package io.github.ocelot.ludum.api;

import io.github.ocelot.ludum.Ludum;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * <p>Manages all minigames registered.</p>
 *
 * @author Ocelot
 */
public final class MinigameRegistry
{
    private static final Map<NamespacedKey, Supplier<Minigame>> MINIGAMES = new HashMap<>();

    private MinigameRegistry()
    {
    }

    @ApiStatus.Internal
    public static void flush()
    {
        MINIGAMES.clear();
    }

    /**
     * Registers a new minigame under the specified name.
     *
     * @param name     The name of the minigame to register
     * @param minigame The minigame to register
     */
    public static synchronized void register(NamespacedKey name, Supplier<Minigame> minigame)
    {
        if (MINIGAMES.put(name, minigame) != null)
            Ludum.getInstance().getLog4JLogger().warn("Duplicate minigame: " + name);
    }

    /**
     * Creates a new minigame with the specified id.
     *
     * @param name The name of the minigame to create
     * @return A new minigame
     * @throws IllegalStateException If there is no minigame with that id
     */
    public static Minigame create(NamespacedKey name)
    {
        if (!MINIGAMES.containsKey(name))
            throw new IllegalStateException("Unknown minigame: " + name);
        return MINIGAMES.get(name).get();
    }

    /**
     * Checks to see if the specified minigame is registered.
     *
     * @param name The name of the minigame to check
     * @return Whether that minigame is registered and {@link #create(NamespacedKey)} will throw an {@link IllegalStateException}
     */
    public static boolean isRegistered(NamespacedKey name)
    {
        return MINIGAMES.containsKey(name);
    }

    /**
     * @return A set of all minigame keys
     */
    public static Set<NamespacedKey> getKeys()
    {
        return MINIGAMES.keySet();
    }
}
