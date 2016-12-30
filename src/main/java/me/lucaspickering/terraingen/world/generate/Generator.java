package me.lucaspickering.terraingen.world.generate;

import java.util.Map;
import java.util.Random;

import me.lucaspickering.terraingen.util.TilePoint;
import me.lucaspickering.terraingen.world.tile.Tile;

/**
 * Represents one stage in the world generation process. A {@link Generator} is given a
 * {@link Map} of {@link TilePoint}s to {@link Tile}s, representing a partially-generated
 * world. It modifies the tiles in that map, and returns the same map with all the same objects.
 */
@FunctionalInterface
public interface Generator {

    /**
     * Modifies the given world, generating new terrain features.
     *
     * @param tiles  the tiles to modify
     * @param random the source of random for generation
     */
    void generate(Map<TilePoint, Tile> tiles, Random random);

}
