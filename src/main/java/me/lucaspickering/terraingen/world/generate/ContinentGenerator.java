package me.lucaspickering.terraingen.world.generate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import me.lucaspickering.terraingen.util.Direction;
import me.lucaspickering.terraingen.util.Funcs;
import me.lucaspickering.terraingen.util.IntRange;
import me.lucaspickering.terraingen.world.Biome;
import me.lucaspickering.terraingen.world.World;
import me.lucaspickering.terraingen.world.tile.Tile;
import me.lucaspickering.terraingen.world.util.Cluster;
import me.lucaspickering.terraingen.world.util.TileMap;
import me.lucaspickering.terraingen.world.util.TilePoint;
import me.lucaspickering.terraingen.world.util.TileSet;

public class ContinentGenerator implements Generator {

    // Range of number of continents to generate
    private static final IntRange CONTINENT_COUNT_RANGE = new IntRange(10, 20);

    // the range that a continent's target size can be in. Note that continents may end up being
    // smaller than the minimum of this range, if there aren't enough tiles to make them bigger.
    private static final IntRange CONTINENT_SIZE_RANGE = new IntRange(100, 1000);

    // Average size of each biome
    private static final int AVERAGE_BIOME_SIZE = 10;

    // The biomes that we can paint in this routine, and the relative chance that each one will
    // be selected
    public static Map<Biome, Integer> BIOME_WEIGHTS;

    // Initialize all the weights
    static {
        try {
            BIOME_WEIGHTS = new EnumMap<>(Biome.class);
            BIOME_WEIGHTS.put(Biome.PLAINS, 10);
            BIOME_WEIGHTS.put(Biome.FOREST, 10);
            BIOME_WEIGHTS.put(Biome.DESERT, 2);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private final TileMap<Cluster> tileToContinentMap = new TileMap<>();

    @Override
    public void generate(World world, Random random) {
        final TileSet worldTiles = world.getTiles();
        final TileSet
            availableTiles =
            new TileSet(worldTiles); // Copy this because we'll be modifying it
        // Cluster tiles to make the continents
        final List<Cluster> continents = generateContinents(worldTiles, availableTiles, random);

        // Adjust elevation to create oceans/coasts
        generateOceanFloor(availableTiles, continents, random);

        // Paint biomes onto each continent
        continents.forEach(c -> paintContinent(c, random));
    }

    /**
     * Clusters together tiles to create a random number of continents. The given {@link TileSet}
     * instance will be modified so that all tiles that are put into a continent or adjacent to
     * a continent are removed. In other words, everything left in the {@link TileSet} object after
     * this function is called will be exactly the tiles that are not part of or adjacent to a
     * continent.
     *
     * @param world          the tiles that make up the world (will NOT be modified)
     * @param availableTiles the tiles that make up the world (to be modified)
     * @param random         the {@link Random} instance to use
     * @return the continents created
     */
    private List<Cluster> generateContinents(TileSet world, TileSet availableTiles, Random random) {
        final int numToGenerate = CONTINENT_COUNT_RANGE.randomIn(random);
        List<Cluster> continents = new ArrayList<>(numToGenerate);

        // While we haven't hit our target number and there are enough tiles left,
        // generate a new continent
        while (continents.size() < numToGenerate
               && availableTiles.size() >= CONTINENT_SIZE_RANGE.min()) {
            final Cluster continent = generateContinent(world, availableTiles, random);
            // If the continent is null, that means that it was generated, but merged into
            // another continent that is already in the list.
            if (continent != null) {
                continents.add(continent);
            }
        }

        // Re-cluster the continents to join any continents that connected to each other
        continents = reclusterContinents(continents);

        cleanupContinents(world, availableTiles, continents);

        return continents;
    }

    /**
     * Generates a single continent from the given collection of available tiles.
     *
     * @param world          the tiles that make up the world (will NOT be modified)
     * @param availableTiles the tiles that aren't not yet in a continent (to be modified)
     * @param random         the {@link Random} instance to use
     * @return the generated continent
     */
    private Cluster generateContinent(TileSet world, TileSet availableTiles, Random random) {
        final Cluster continent = Cluster.fromWorld(world); // The continent
        final int targetSize = CONTINENT_SIZE_RANGE.randomIn(random); // Pick a target size

        // Add the seed to the continent, and remove it from the pool of available tiles
        final Tile seed = Funcs.randomFromCollection(random, availableTiles); // The first tile
        addToContinent(seed, availableTiles, continent);

        // Keep adding until we hit our target size
        while (continent.size() < targetSize) {
            // If a tile is adjacent to any tile in the continent, it becomes a candidate
            final TileSet candidates = continent.allAdjacents();
            candidates.retainAll(availableTiles); // Filter out tiles that aren't available

            // No candidates, done with this continent
            if (candidates.isEmpty()) {
                break;
            }

            // Pick a random tile adjacent to the continent and add it in
            final Tile nextTile = Funcs.randomFromCollection(random, candidates);
            addToContinent(nextTile, availableTiles, continent);
        }

        assert !continent.isEmpty(); // At least one tile should have been added

        return continent;
    }

    private List<Cluster> reclusterContinents(List<Cluster> continents) {
        final TileSet allTiles = new TileSet();
        for (Cluster continent : continents) {
            allTiles.addAll(continent);
        }

        final List<Cluster> newContinents = allTiles.cluster();
        for (Cluster continent : newContinents) {
            for (Tile tile : continent) {
                tileToContinentMap.put(tile, continent);
            }
        }

        return newContinents;
    }

    /**
     * "Cleans up" all the given continents. This fixes errors/imperfections  such as unassigned
     * tiles inside of continents and long strings of land that look strange.
     *
     * @param world          the world (will NOT be modified)
     * @param availableTiles tiles that are not yet in a continent (will most likely be modified)
     * @param continents     all the continents to clean up
     */
    private void cleanupContinents(TileSet world, TileSet availableTiles, List<Cluster> continents) {
        // Cluster the negative tilesK
        final List<Cluster> nonContinentClusters = availableTiles.cluster();

        // Fill in the "holes" in each continent, i.e. find all clusters that are entirely inside
        // one continent, and add them into that continent.
        for (Cluster nonContinentCluster : nonContinentClusters) {
            // If the cluster is small enough that it won't become an ocean, check if its  inside
            // one continent. This is just an optimization, as extremely large clusters are
            // all but guaranteed to not be entirely inside one continent. Skipping them saves time.
            if (nonContinentCluster.size() < WaterPainter.MIN_OCEAN_SIZE) {
                // Copy the cluster so that it exists in the context of the entire world, then
                // check if it is entirely within one continent.
                final Cluster copiedCluster = Cluster.copyToWorld(world, nonContinentCluster);
                final Cluster surroundingContinent = getSurroundingContinent(copiedCluster);
                if (surroundingContinent != null) {
                    // Add the cluster to the continent that completely surrounds it
                    for (Tile tile : nonContinentCluster) {
                        addToContinent(tile, availableTiles, surroundingContinent);
                    }
                }
            }
        }

        // Smooth each continent
        for (Cluster continent : continents) {
            smoothCoast(availableTiles, continent);
        }
    }

    /**
     * Gets the continent that completely surrounds the given cluster. Returns {@code null} if
     * the given cluster borders more than one continent.
     *
     * @param cluster the cluster to check the surroundings of
     * @return the continent that completely surrounds the given cluster, or {@code null} if it
     * borders more than one continent
     * @throws IllegalStateException if the given cluster borders a tile that is not part of a
     *                               continent
     */
    private Cluster getSurroundingContinent(Cluster cluster) {
        // Find out if all tiles adjacent to this cluster are in the same continent
        Cluster prevAdjContinent = null;
        for (Tile adjTile : cluster.allAdjacents()) {
            final Cluster adjContinent = tileToContinentMap.get(adjTile);

            // This shouldn't happen, because if this cluster is adjacent to a non-continent tile,
            // then that tile should be in this cluster instead.
            if (adjContinent == null) {
                throw new IllegalStateException("Continent tile is missing from the map");
            }

            // If the previous continent hasn't been set yet, do that now. Otherwise, check if
            // this tile has the same continent as the previous (== is correct because they should
            // have the exact same Cluster object).
            if (prevAdjContinent == null) {
                prevAdjContinent = adjContinent;
            } else if (prevAdjContinent != adjContinent) {
                // Continent mismatch, this cluster borders multiple continents, return null
                return null;
            }
        }

        // Every tile in this cluster is adjacent to prevAdjContinent, so return that as our result
        return prevAdjContinent;
    }

    /**
     * Smooth the coast of continents by removing thin bits of land that stick out.
     *
     * @param availableTiles the tiles that aren't in any continent (some tiles will probably be
     *                       added back to this)
     * @param continent      the continent to be smoothed
     */
    private void smoothCoast(TileSet availableTiles, Cluster continent) {
        for (Tile tile : continent) {
            final Map<Direction, Tile> adjTiles = continent.getAdjacentTiles(tile);

            // If the tile borders only 1 other tile in the continent (or none), mark it for
            // removal
            boolean remove = adjTiles.size() <= 1;

            // If it isn't already marked for removal, check if it borders only two tiles
            // that aren't adjacent to each other (i.e. check if this tile is a "bridge")
            if (!remove && adjTiles.size() == 2) {
                final List<Direction> dir = new ArrayList<>(adjTiles.keySet());
                // Check that the two directions aren't adjacent to each otherK
                if (!dir.get(0).isAdjacentTo(dir.get(1))) {
                    remove = true;
                }
            }

            if (remove) {
                // Remove the tile from the continent
                availableTiles.add(tile);
                continent.remove(tile);
                tileToContinentMap.remove(tile);

                // Let a recursive call handle the rest (we can't modify the continent then
                // continue to iterate on it)
                smoothCoast(availableTiles, continent);
                return;
            }
        }
    }

    /**
     * Adds the given tile to the given continent and removes the tile from the collection of
     * available tiles. Available tiles are ones that are not yet in a continent. If the tile is
     * added to the continent, then {@link #tileToContinentMap} will be updated.
     *
     * @param tile           the tile to be added to the continent
     * @param availableTiles the collection of tiles that are available to be added, i.e. the tiles
     *                       that are not yet in any continent
     * @param continent      the continent receiving the tile
     */
    private void addToContinent(Tile tile, TileSet availableTiles, Cluster continent) {
        final boolean added = continent.add(tile);
        if (added) {
            tileToContinentMap.put(tile, continent);
            if (!availableTiles.remove(tile)) {
                throw new IllegalStateException("Tile is not available to be added");
            }
        }
    }

    private void generateOceanFloor(TileSet nonContinentTiles, List<Cluster> continents,
                                    Random random) {
        nonContinentTiles.forEach(tile -> tile.setElevation(-20));

        // Make all tiles adjacent to each continent shallow, so they become coast
        for (Cluster continent : continents) {
            for (Tile tile : continent.allAdjacents()) {
                tile.setElevation(-6);
            }
        }
    }

    /**
     * "Paints" biomes onto the given continent.
     *
     * @param continent the continent to be painted
     * @param random    the {@link Random} instance to use
     */
    private void paintContinent(Cluster continent, Random random) {
        // Step 1 - calculate n
        // Figure out how many biome biomes we want
        // n = number of tiles / average size of blotch
        // Step 2 - select seeds
        // Pick n tiles to be "seed tiles", i.e. the first tiles of their respective biomes.
        // The seeds have a minimum spacing from each other, which is enforced now.
        // Step 3 - grow seeds
        // Each blotch will be grown from its seed to be about average size.
        // By the end of this step, every tile will have been assigned.
        // Iterate over each blotch, and at each iteration, add one tile to that blotch that is
        // adjacent to it. Then, move onto the next blotch. Rinse and repeat until there are no
        // more tiles to assign.
        // Step 4 - assign the biomes
        // Iterate over each blotch and select the biome for that blotch, then assign the biome for
        // each tile in that blotch.

        // Step 1
        final int numSeeds = continent.size() / AVERAGE_BIOME_SIZE;

        // Step 2
        final TileSet seeds = continent.selectTiles(random, numSeeds, 0);
        final TileSet unselectedTiles = new TileSet(continent); // We need a copy so we can modify it
        unselectedTiles.removeAll(seeds); // We've already selected the seeds, so remove them

        // Each biome, keyed by its seed
        final TileMap<Cluster> biomes = new TileMap<>();
        final Set<TilePoint> incompleteBiomes = new HashSet<>(); // Biomes with room to grow
        for (Tile seed : seeds) {
            // Pick a biome for this seed, then add it to the map
            final Cluster blotch = Cluster.fromWorld(continent);
            blotch.add(seed);
            biomes.put(seed, blotch);
            incompleteBiomes.add(seed.pos());
        }

        // Step 3 (the hard part)
        // While there are tiles left to assign...
        while (!unselectedTiles.isEmpty() && !incompleteBiomes.isEmpty()) {
            // Pick a seed that still has openings to work from
            final TilePoint seed = Funcs.randomFromCollection(random, incompleteBiomes);
            final Cluster biome = biomes.get(seed); // The biome grown from that seed

            final TileSet adjTiles = biome.allAdjacents(); // All tiles adjacent to this biome
            adjTiles.retainAll(unselectedTiles); // Remove tiles that are already in a biome

            if (adjTiles.isEmpty()) {
                // We've run out of ways to expand this biome, so consider it complete
                incompleteBiomes.remove(seed);
                continue;
            }

            // Pick one of those unassigned adjacent tiles, and add it to this biome
            final Tile tile = Funcs.randomFromCollection(random, adjTiles);
            biome.add(tile);
            unselectedTiles.remove(tile);
        }

        // Step 4
        // Pick a biome for each cluster, using weighted chance as defined in BIOME_WEIGHTS
        for (Cluster blotch : biomes.values()) {
            final Biome biome = Funcs.randomFromCollectionWeighted(random,
                                                                   BIOME_WEIGHTS.keySet(),
                                                                   BIOME_WEIGHTS::get);
            blotch.forEach(tile -> tile.setBiome(biome)); // Set the biome for each tile
        }
    }
}
